import cv2
import boto3
import os
import requests # API 통신을 위한 라이브러리
import json # JSON 데이터를 파싱하기 위한 라이브러리
from datetime import datetime, timezone
from ultralytics import YOLO
import paho.mqtt.client as mqtt
from threading import Timer
from dotenv import load_dotenv # env 파일을 읽어오기 위한 라이브러리

# ----------------------------------------------------
# [1. 글로벌 변수 및 네트워크 설정]
# ----------------------------------------------------
# .env 파일에서 환경 변수 불러오기
load_dotenv() 

# 보안 처리된 환경 변수에서 값을 가져옵니다.
AWS_ACCESS_KEY = os.getenv("AWS_ACCESS_KEY", "")
AWS_SECRET_KEY = os.getenv("AWS_SECRET_KEY", "")
AWS_REGION = "ap-northeast-2" # 예: 서울 리전
BUCKET_NAME = "new-ejo-bucket"

# Tapo C200 IP 카메라 RTSP 주소
CAM_URL = "rtsp://abcd1234:00001234@172.20.10.10:554/stream1"

# 백엔드 실제 API 주소 반영
BASE_URL = "http://13.209.33.104:8080"
FASTAPI_SQUATTING_URL = f"{BASE_URL}/api/seat/squatting"
FASTAPI_LOST_ITEM_URL = f"{BASE_URL}/api/seat/lost-item"
FASTAPI_POSTURE_URL = f"{BASE_URL}/api/seat/posture"
FASTAPI_CHECKIN_STATUS_URL = f"{BASE_URL}/api/seat/check-in-status"

# S3 클라이언트 초기화
s3_client = boto3.client(
    's3',
    aws_access_key_id=AWS_ACCESS_KEY,
    aws_secret_access_key=AWS_SECRET_KEY,
    region_name=AWS_REGION
)

squatting_timers = {}
# ⚠️ 현재 테스트를 위해 10초로 변경 (관리자가 변경하면 이 값이 업데이트 됨)
SQUATTING_LIMIT = 10  

# ----------------------------------------------------
# [2. 사석화 판정 로직]
# ----------------------------------------------------
def trigger_squatting(seat_num):
    print(f"[WARNING] Seat {seat_num} empty for timer limit! Marked as squatting.")
    try:
        payload = {"seat_num": seat_num, "status": "squatting"}
        response = requests.post(FASTAPI_SQUATTING_URL, json=payload)
        if response.status_code == 200:
            print(f"[SUCCESS] Squatting status for Seat {seat_num} sent!")
        else:
            print(f"[ERROR] Failed to send status. HTTP Code: {response.status_code}")
    except Exception as e:
        print(f"[ERROR] Cannot connect to main server: {e}")

# ----------------------------------------------------
# [3. 분실물 탐지 및 개별 크롭(Crop) 업로드 로직]
# ----------------------------------------------------
def check_lost_items():
    print("[CAMERA] Admin trigger received: Starting CCTV multi-seat scan!")
    
    try:
        # 1. YOLO 모델 로드
        print("[YOLO] Loading model 'best_v4.pt'...")
        model = YOLO('best_v4.pt')
        
        # 2. 웹캠 사진 촬영
        print(f"[CAMERA] Connecting to RTSP stream: {CAM_URL}...")
        cap = cv2.VideoCapture(CAM_URL)
        ret, frame = cap.read()
        
        # 카메라 화면의 가로 너비(Width) 가져오기
        frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        cap.release() 
        
        if not ret:
            print("[ERROR] Failed to capture image from RTSP stream.")
            return

        # 3. 객체 탐지 수행 (원본 frame에서 탐지)
        print("[YOLO] Running object detection on full CCTV view...")
        results = model(frame)
        
        # 3-1. 객체 추출 및 좌표, 종류 임시 저장
        detected_items_with_seats = []
        
        for box in results[0].boxes:
            # 카테고리 이름 추출
            class_id = int(box.cls[0])
            category = results[0].names[class_id]
            
            # 바운딩 박스 좌표 추출 (크롭을 위해 정수형 변환)
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            x_center = (x1 + x2) / 2 # 물체의 중심 x 좌표
            
            # 화면을 4등분하여 좌석 번호 판별 (1, 2, 3, 4번 좌석)
            if x_center < (frame_width / 4):
                detected_seat = 1
            elif x_center < (frame_width * 2 / 4):
                detected_seat = 2
            elif x_center < (frame_width * 3 / 4):
                detected_seat = 3
            else:
                detected_seat = 4
                
            detected_items_with_seats.append({
                "seat_num": detected_seat,
                "category": category,
                "bbox": (x1, y1, x2, y2) # 자르기 위한 좌표 저장
            })
            
        # 중복 제거 (예: 1번 자리에 책이 2권 있어도 1번만 전송)
        unique_items = []
        seen = set()
        for item in detected_items_with_seats:
            identifier = f"{item['seat_num']}_{item['category']}"
            if identifier not in seen:
                seen.add(identifier)
                unique_items.append(item)
                
        print(f"[YOLO] Unique Mapped Items: {[f'Seat {i['seat_num']}: {i['category']}' for i in unique_items]}")

        # 탐지된 물건이 없으면 여기서 바로 종료 (불필요한 동작 방지)
        if not unique_items:
            print("[YOLO] ✨ No items detected. Skipping S3 upload and API call.")
            return 

        # 4. 개별 이미지 크롭, S3 업로드 및 API 전송
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        print(f"[API] Processing and sending lost item data to server one by one...")
        
        for item in unique_items:
            # 4-1. 원본 이미지에서 해당 물건 영역만 크롭 (y좌표 먼저, x좌표 나중)
            x1, y1, x2, y2 = item["bbox"]
            cropped_img = frame[y1:y2, x1:x2]
            
            # 4-2. 개별 파일로 로컬에 저장
            local_filename = f"lost_item_seat{item['seat_num']}_{item['category']}_{timestamp}.jpg"
            cv2.imwrite(local_filename, cropped_img)
            print(f"[LOCAL STORAGE] Cropped image saved: {local_filename}")
            
            # 4-3. S3에 개별 업로드
            s3_key = f"lost_items/{local_filename}"
            s3_client.upload_file(
                local_filename, BUCKET_NAME, s3_key,
                ExtraArgs={'ContentType': 'image/jpeg'}
            )
            s3_url = f"https://{BUCKET_NAME}.s3.{AWS_REGION}.amazonaws.com/{s3_key}"
            
            # 4-4. 백엔드로 개별 S3 URL 전송
            lost_item_payload = {
                "seat_num": item["seat_num"], 
                "image_url": s3_url,         
                "category": item["category"]  
            }
            
            api_response = requests.post(FASTAPI_LOST_ITEM_URL, json=lost_item_payload)
            
            if api_response.status_code == 200:
                 print(f"[SUCCESS] Sent S3 URL for Seat {item['seat_num']} -> {item['category']}")
            else:
                 print(f"[ERROR] Failed to send {item['category']}. HTTP Code: {api_response.status_code}")
        
    except Exception as e:
        print(f"[ERROR] Exception occurred: {e}")
        
    finally:
        # 5. 메모리 강제 해제
        if 'model' in locals():
            del model
            print("[CLEANUP] YOLO model unloaded.")

# ----------------------------------------------------
# [4. MQTT 이벤트 수신 및 라우팅 로직]
# ----------------------------------------------------
def on_message(client, userdata, msg):
    global SQUATTING_LIMIT 
    
    topic = msg.topic
    payload_str = msg.payload.decode("utf-8").strip()
    
    if topic.startswith("seat/status/"):
        seat_num = int(topic.split("/")[-1])
        
        try:
            # ESP32가 보낸 JSON 문자열을 파이썬 딕셔너리로 변환
            data = json.loads(payload_str)
            
            # [상태 B] 사람이 일어난 경우 (status: 0 이 넘어옴)
            if "status" in data and data["status"] == 0:
                print(f"[Seat {seat_num}] Empty seat detected. Checking app status...")
                
                # 백엔드에 현재 발권(체크인) 상태인지 물어보는 검증 로직
                is_checked_in = True # 기본값 (API 통신 실패 시 테스트를 위해 True로 둠)
                try:
                    url = f"{FASTAPI_CHECKIN_STATUS_URL}/{seat_num}"
                    # timeout=2: 서버가 꺼져있을 때 2초만 기다리고 바로 다음 코드로 넘어가게 함
                    response = requests.get(url, timeout=2)
                    if response.status_code == 200:
                        is_checked_in = response.json().get("is_checked_in", True)
                except requests.exceptions.RequestException:
                    print(f"[API] (Mock API) Server unreachable. Assuming Seat {seat_num} is checked in.")
                
                # 체크인 상태일 때만 사석화 타이머 가동
                if is_checked_in:
                    print(f"[Seat {seat_num}] User is still checked in! Starting squatting timer ({SQUATTING_LIMIT}s)...")
                    if seat_num not in squatting_timers:
                        timer = Timer(SQUATTING_LIMIT, trigger_squatting, args=[seat_num])
                        timer.start()
                        squatting_timers[seat_num] = timer
                else:
                    print(f"[INFO] ✨ Seat {seat_num} user checked out properly. No squatting timer started.")
            
            # [상태 A] 사람이 앉아 있는 경우 (posture, left, right, back 데이터가 넘어옴)
            else:
                posture = data.get("posture", "정상")
                left_val = data.get("left", 0)
                right_val = data.get("right", 0)
                back_val = data.get("back", 0)
                
                print(f"[Seat {seat_num}] Occupied. Posture: {posture} (L:{left_val}, R:{right_val}, B:{back_val})")
                
                # 사람이 돌아왔으니 사석화 타이머 취소
                if seat_num in squatting_timers:
                    print(f"[TIMER] Seat {seat_num} user returned. Timer canceled.")
                    squatting_timers[seat_num].cancel()
                    del squatting_timers[seat_num]
                
                # timestamp 생성 (UTC 기준 ISO 8601 포맷)
                current_time_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

                # 라즈베리 파이가 메인 서버(백엔드)로 헬스케어 데이터 전송
                posture_payload = {
                    "seat_num": seat_num,
                    "posture": posture,
                    "left_pressure": data.get("left", 0),
                    "right_pressure": data.get("right", 0),
                    "back_pressure": data.get("back", 0),
                    "timestamp": current_time_iso 
                }
                try:
                    # 응답 결과를 response 변수에 담기
                    response = requests.post(FASTAPI_POSTURE_URL, json=posture_payload)
                    
                    # HTTP 상태 코드가 200번대(성공)일 때만 성공 로그 출력
                    if response.status_code == 200 or response.status_code == 201:
                        print(f"[SUCCESS] Posture data sent! (Seat {seat_num})")
                    else:
                        print(f"[ERROR] Posture API failed. HTTP Code: {response.status_code}, Response: {response.text}")
                        
                except Exception as e:
                    print(f"[ERROR] Failed to connect to server for posture API: {e}")

        except json.JSONDecodeError:
            # JSON 형식이 깨지거나 알 수 없는 데이터가 들어와도 서버가 꺼지지 않도록 방어
            print(f"[ERROR] Invalid JSON data received: {payload_str}")

    # [수동 트리거] 관리자가 분실물 스캔 버튼을 눌렀을 때
    elif topic == "admin/trigger_lost_item":
        # CCTV가 전 좌석을 확인하므로 특정 좌석 번호 없이 전체 스캔 함수 호출
        check_lost_items()

    # [동적 타이머 설정] 관리자가 사석화 기준 시간을 변경했을 때
    elif topic == "admin/config/squatting_time":
        try:
            # 관리자가 앱에서 보낸 JSON (예: {"limit_minutes": 60})
            config_data = json.loads(payload_str)
            new_minutes = config_data.get("limit_minutes", 45) # 기본값은 45분
            
            # 분(Minutes)을 초(Seconds)로 변환하여 글로벌 변수 업데이트
            SQUATTING_LIMIT = new_minutes * 60 
            
            print(f"[CONFIG] 🛠️ Admin updated squatting limit to {new_minutes} minutes ({SQUATTING_LIMIT} seconds)!")
        except Exception as e:
            print(f"[ERROR] Failed to update squatting time: {e}")

# ----------------------------------------------------
# [5. 메인 실행]
# ----------------------------------------------------
if __name__ == "__main__":
    # 버전 명시 추가 (DeprecationWarning 경고 제거)
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1)
    
    client.on_message = on_message
    client.connect("localhost", 1883)
    
    # 구독(Subscribe) 토픽들 등록
    client.subscribe("seat/status/#")
    client.subscribe("admin/trigger_lost_item")
    client.subscribe("admin/config/squatting_time") # 설정 변경 토픽 추가
    
    print("🚀 Edge Server Ready. Listening for events...")
    client.loop_forever()