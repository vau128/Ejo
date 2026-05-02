import paho.mqtt.client as mqtt
from ultralytics import YOLO
import cv2
import time
import requests

# [설정] AI 모델 및 서버 주소
model = YOLO('yolov8n.pt') 
CAM_URL = "rtsp://abcd1234:00001234@172.20.10.10:554/stream1"
# [협의 필요] 서버 팀원에게 FastAPI 서버의 IP와 포트번호를 받아야 함
FASTAPI_URL = "http://서버IP:8000/seat/status" 

# ---------------------------------------------------------
# [추가] 7개 좌석의 구역(Bounding Box) 좌표 설정
# 카메라 화면을 640x480으로 리사이즈했을 때 기준
# 실제 카페에 카메라를 설치한 후, 앵글에 맞게 이 좌표들을 수정해야 합니다.
# 형식: {좌석번호: [x_min, y_min, x_max, y_max]}
# ---------------------------------------------------------
SEAT_ZONES = {
    1: [10, 10, 150, 150],   # 1번 좌석 좌표 (임시)
    2: [160, 10, 300, 150],  # 2번 좌석 좌표 (임시)
    3: [310, 10, 450, 150],  # 3번 좌석 좌표 (임시)
    4: [460, 10, 600, 150],  # 4번 좌석 좌표 (임시)
    5: [10, 160, 150, 300],  # 5번 좌석 좌표 (임시)
    6: [160, 160, 300, 300], # 6번 좌석 좌표 (임시)
}

# ---------------------------------------------------------
# [상태 관리] 7개 좌석의 독립적인 상태를 저장하기 위한 딕셔너리
# ---------------------------------------------------------
# seat_data[번호] 형식으로 각 좌석의 압력, AI 판정, 시간 상태를 개별 관리합니다.
seat_data = {i: {
    "current_state": "빈좌석",
    "is_checked_out": False,
    "lost_item_start_time": None,
    "suspicious_start_time": None,
    "last_ai_time": 0,
    "is_item_present": False
} for i in range(1, 8)} # 1번부터 7번 좌석까지 초기화

# ---------------------------------------------------------
# [F-05] S3 업로드 함수 (계정 정보 확인 후 주석 해제하여 사용)
# ---------------------------------------------------------
def upload_to_s3(file_path, file_name):
    """[F-05] S3 업로드 함수 (계정 정보 확인 후 사용)"""
    # [협의 필요] 팀원이 버킷 이름을 알려주면 여기에 넣어야 함
    BUCKET_NAME = "your-seat-project-bucket" 
    
    # s3 = boto3.client('s3') # 계정 정보 세팅 후 주석 해제
    try:
        # s3.upload_file(file_path, BUCKET_NAME, file_name)
        url = f"https://{BUCKET_NAME}.s3.amazonaws.com/{file_name}"
        print(f"S3 업로드 가상 성공 (URL: {url})")
        return url
    except Exception as e:
        print(f"S3 업로드 에러: {e}")
        return None

# [수정] 어느 좌석을 판별할지 알기 위해 seat_id 파라미터 추가
def run_ai_analysis(seat_id, save_path=None):
    """카메라 캡처 및 AI 분석 (좌표 기반 구역 판별 추가)"""
    try:
        cap = cv2.VideoCapture(CAM_URL)
        if not cap.isOpened(): return False
        ret, frame = cap.read()
        cap.release()
        
        if ret:
            frame = cv2.resize(frame, (640, 480))
            if save_path:
                cv2.imwrite(save_path, frame) # 사진 저장 로직
            results = model.predict(frame, conf=0.25, verbose=False)
            
            # 좌표 기반 판별 로직
            zone = SEAT_ZONES.get(seat_id)
            if not zone: return False # 등록되지 않은 좌석이면 무시
            
            # YOLO가 찾은 모든 객체를 순회하며 해당 좌석 구역에 있는지 검사
            for box in results[0].boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                cx, cy = (x1 + x2) / 2, (y1 + y2) / 2 # 객체의 중심점 계산
                
                # 객체의 중심점이 설정한 좌석 구역(zone) 내에 포함되는지 확인
                if zone[0] <= cx <= zone[2] and zone[1] <= cy <= zone[3]:
                    return True # 해당 좌석 구역에 짐이 있음!
                    
            return False # 구역 내에 짐이 없으면 False
    except Exception as e:
        print(f"AI 에러: {e}")
        return False

def send_to_server(seat_id, status, image_url=None):
    """[F-05] FastAPI 서버로 최종 판정 결과를 전송 (이미지 URL 포함 가능)"""
    try:
        data = {
            "seat_id": f"SEAT_{seat_id:02d}", # 좌석 번호를 SEAT_01 형식으로 동적 생성
            "status": status,
            "image_url": image_url, # 사진 주소
            "update_time": time.strftime('%Y-%m-%d %H:%M:%S')
        }
        # FastAPI 서버로 JSON 데이터 전송
        response = requests.post(FASTAPI_URL, json=data, timeout=5)
        if response.status_code == 200:
            print(f"서버 전송 성공 ({data['seat_id']}): {status}")
        else:
            print(f"서버 응답 에러: {response.status_code}")
    except Exception as e:
        print(f"서버 연결 실패: {e}")

def on_message(client, userdata, msg):
    global seat_data
    
    try:
        topic = msg.topic
        payload = msg.payload.decode().strip()
        
        # 1. 토픽 분석을 통한 좌석 번호 추출 (ex: seat/status/1 -> 1)
        # ---------------------------------------------------------
        topic_parts = topic.split('/')
        if len(topic_parts) < 3: return # 잘못된 토픽 형식 무시
        
        seat_num = int(topic_parts[-1]) # 마지막 숫자를 좌석 번호로 사용
        if seat_num not in seat_data: return # 정의되지 않은 좌석 번호 무시
        
        s = seat_data[seat_num] # 해당 좌석의 데이터 참조

        # [이벤트] 앱/서버팀에서 특정 좌석의 '퇴실' 클릭 시 신호 수신
        if "checkout" in topic:
            s["is_checked_out"] = True
            print(f"이벤트: {seat_num}번 좌석 퇴실 신호 수신")
            return

        # 2. 압력 데이터 수신 및 AI 분석 최적화
        # ---------------------------------------------------------
        try:
            pressure_value = int(payload)
        except ValueError: return

        is_person_present = pressure_value > 1500 
        
        # 전체 시스템 부하를 줄이기 위해 각 좌석별로 10초 간격 AI 분석 실행
        current_time = time.time()
        if current_time - s["last_ai_time"] >= 10:
            # [수정] seat_num을 함께 넘겨주어 해당 좌석 좌표만 검사하도록 변경
            s["is_item_present"] = run_ai_analysis(seat_id=seat_num)
            s["last_ai_time"] = current_time
            print(f"{seat_num}번 좌석 AI 분석 수행 완료")
        
        # 3. 좌석 상태 판별 로직 (개별 좌석 상태 업데이트)
        # ---------------------------------------------------------
        if is_person_present:
            s["lost_item_start_time"] = None
            s["suspicious_start_time"] = None
            if s["is_checked_out"]:
                s["current_state"] = "사석화 (퇴실후 미퇴거)"
            else:
                s["current_state"] = "정상 사용중"
                s["is_checked_out"] = False 
        else:
            if s["is_item_present"]:
                if s["is_checked_out"]:
                    s["current_state"] = "분실물 확인 중"
                    if s["lost_item_start_time"] is None: s["lost_item_start_time"] = time.time()
                else:
                    s["current_state"] = "사석화 의심 (자리비움)"
                    if s["suspicious_start_time"] is None: s["suspicious_start_time"] = time.time()
            else:
                s["current_state"] = "정상 빈좌석"
                s["is_checked_out"] = False
                s["lost_item_start_time"] = None
                s["suspicious_start_time"] = None

        # 4. [F-04/F-05] 시간 기반 최종 확정 및 서버 전송
        # ---------------------------------------------------------
        # [분실물 확정] 퇴실 후 짐이 20분간 방치된 경우
        if s["lost_item_start_time"]:
            if (time.time() - s["lost_item_start_time"]) / 60 >= 20:
                if s["current_state"] != "분실물 확정":
                    s["current_state"] = "분실물 확정"
                    img_name = f"lost_seat{seat_num}_{int(time.time())}.jpg"
                    # [수정] 사진 저장할 때도 seat_num 넘겨주기
                    run_ai_analysis(seat_id=seat_num, save_path=img_name) 
                    send_to_server(seat_num, s["current_state"], image_url="pending_s3_url")

        # [사석화 확정] 점유 중 자리비움이 15분간 지속된 경우
        if s["suspicious_start_time"]:
            if (time.time() - s["suspicious_start_time"]) / 60 >= 15:
                if s["current_state"] != "사석화 확정":
                    s["current_state"] = "사석화 확정"
                    send_to_server(seat_num, s["current_state"])

        print(f"[{time.strftime('%H:%M:%S')}] Seat {seat_num} | 상태: {s['current_state']} | 압력: {pressure_value} | 짐: {s['is_item_present']}")
    
    except Exception as e:
        print(f"로직 에러 (Seat {seat_num if 'seat_num' in locals() else 'Unknown'}): {e}")

def on_disconnect(client, userdata, rc):
    if rc != 0: print("MQTT 연결 유실, 재접속 대기 중...")

client = mqtt.Client()
client.on_message = on_message
client.on_disconnect = on_disconnect

try:
    client.connect("localhost", 1883, 60)
    # seat/status/1, seat/status/2 등 모든 하위 토픽 수신을 위해 와일드카드(#) 사용
    client.subscribe([("seat/status/#", 0), ("seat/checkout/#", 0)])
    print("시스템 가동 중... (7개 좌석 통합 관리 모드)")
    client.loop_forever()
except Exception as e:
    print(f"가동 실패: {e}")