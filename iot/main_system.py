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

# [상태 관리] 시스템 플래그
current_state = "빈좌석"
is_checked_out = False        
lost_item_start_time = None   
suspicious_start_time = None  

last_ai_time = 0
is_item_present = False # 마지막 AI 판정 결과 저장용

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

def run_ai_analysis(save_path=None):
    """카메라 캡처 및 AI 분석 (save_path가 있으면 사진 저장)"""
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
            return len(results[0].boxes) > 0 
        return False
    except Exception as e:
        print(f"AI 에러: {e}")
        return False

def send_to_server(status, image_url=None):
    """[F-05] FastAPI 서버로 최종 판정 결과를 전송 (이미지 URL 포함 가능)"""
    try:
        data = {
            "seat_id": "SEAT_01", # 좌석 번호 협의 필요
            "status": status,
            "image_url": image_url, # 사진 주소
            "update_time": time.strftime('%Y-%m-%d %H:%M:%S')
        }
        # FastAPI 서버로 JSON 데이터 전송
        response = requests.post(FASTAPI_URL, json=data, timeout=5)
        if response.status_code == 200:
            print(f"서버 전송 성공: {status}")
        else:
            print(f"서버 응답 에러: {response.status_code}")
    except Exception as e:
        print(f"서버 연결 실패: {e}")

def on_message(client, userdata, msg):
    global current_state, is_checked_out, lost_item_start_time, suspicious_start_time
    global last_ai_time, is_item_present # 최적화 변수
    
    try:
        payload = msg.payload.decode().strip()
        
        # 앱/서버팀에서 '퇴실' 클릭 시 신호 수신
        if msg.topic == "seat/checkout":
            is_checked_out = True
            print("이벤트: 퇴실 신호 수신")
            return

        # 1. 압력 데이터 수신
        try:
            pressure_value = int(payload)
        except ValueError: return

        is_person_present = pressure_value > 1500 
        
        # 5초 간격으로만 AI 분석 실행
        current_time = time.time()
        if current_time - last_ai_time >= 10: # 10초가 지났을 때만 YOLO 가동
            is_item_present = run_ai_analysis()
            last_ai_time = current_time
            print("AI 분석 수행 완료 (10초 주기)")
        
        # 2. 좌석 상태 판별 로직
        if is_person_present:
            lost_item_start_time = None
            suspicious_start_time = None
            if is_checked_out:
                current_state = "사석화 (퇴실후 미퇴거)"
            else:
                current_state = "정상 사용중"
                is_checked_out = False 
        else:
            if is_item_present:
                if is_checked_out:
                    current_state = "분실물 확인 중"
                    if lost_item_start_time is None: lost_item_start_time = time.time()
                else:
                    current_state = "사석화 의심 (자리비움)"
                    if suspicious_start_time is None: suspicious_start_time = time.time()
            else:
                current_state = "정상 빈좌석"
                is_checked_out = False
                lost_item_start_time = None
                suspicious_start_time = None

        # 3. [F-04/F-05] 시간 기반 최종 확정 및 서버 전송
        # 분실물 확정 (20분) 
        if lost_item_start_time:
            if (time.time() - lost_item_start_time) / 60 >= 20:
                if current_state != "분실물 확정":
                    current_state = "분실물 확정"
                    img_name = f"lost_{int(time.time())}.jpg"
                    run_ai_analysis(save_path=img_name) 
                    send_to_server(current_state, image_url="pending_s3_url")

        # 사석화 확정 (15분) 
        if suspicious_start_time:
            if (time.time() - suspicious_start_time) / 60 >= 15:
                if current_state != "사석화 확정":
                    current_state = "사석화 확정"
                    send_to_server(current_state)

        print(f"[{time.strftime('%H:%M:%S')}] 상태: {current_state} | 압력: {pressure_value} | 짐: {is_item_present}")
    
    except Exception as e:
        print(f"로직 에러: {e}")

def on_disconnect(client, userdata, rc):
    if rc != 0: print("MQTT 연결 유실, 재접속 대기 중...")

client = mqtt.Client()
client.on_message = on_message
client.on_disconnect = on_disconnect

try:
    client.connect("localhost", 1883, 60)
    client.subscribe([("seat/status", 0), ("seat/checkout", 0)])
    print("시스템 가동 중... (Ctrl+C로 종료)")
    client.loop_forever()
except Exception as e:
    print(f"가동 실패: {e}")
