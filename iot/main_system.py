import os
import time

import cv2
import paho.mqtt.client as mqtt
import requests
from ultralytics import YOLO

# [설정] AI 모델 및 서버 주소
model = YOLO("yolov8n.pt")
CAM_URL = os.getenv("CAM_URL", "")
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api/iot/seat-status")
SEAT_ID_MAP = {
    1: "seat-1",
    2: "seat-2",
    3: "seat-3",
    4: "seat-4",
    5: "seat-5",
    6: "seat-6",
}

# ---------------------------------------------------------
# [추가] 6개 좌석의 구역(Bounding Box) 좌표 설정
# 카메라 화면을 640x480으로 리사이즈했을 때 기준
# 실제 카메라 앵글에 맞게 수정해야 합니다.
# 형식: {좌석번호: [x_min, y_min, x_max, y_max]}
# ---------------------------------------------------------
SEAT_ZONES = {
    1: [10, 10, 150, 150],
    2: [160, 10, 300, 150],
    3: [310, 10, 450, 150],
    4: [460, 10, 600, 150],
    5: [10, 160, 150, 300],
    6: [160, 160, 300, 300],
}

# ---------------------------------------------------------
# [상태 관리] 6개 좌석의 독립적인 상태를 저장하기 위한 딕셔너리
# ---------------------------------------------------------
seat_data = {
    i: {
        "current_state": "빈좌석",
        "last_sent_state": None,
        "is_checked_out": False,
        "lost_item_start_time": None,
        "suspicious_start_time": None,
        "last_ai_time": 0,
        "is_item_present": False,
    }
    for i in range(1, 7)
}


def upload_to_s3(file_path, file_name):
    """[F-05] S3 업로드 함수 (계정 정보 확인 후 사용)"""
    bucket_name = "your-seat-project-bucket"

    try:
        url = f"https://{bucket_name}.s3.amazonaws.com/{file_name}"
        print(f"S3 업로드 가상 성공 (URL: {url})")
        return url
    except Exception as e:
        print(f"S3 업로드 에러: {e}")
        return None


def run_ai_analysis(seat_id, save_path=None):
    """카메라 캡처 및 AI 분석"""
    try:
        if not CAM_URL:
            return False

        cap = cv2.VideoCapture(CAM_URL)
        if not cap.isOpened():
            return False

        ret, frame = cap.read()
        cap.release()

        if not ret:
            return False

        frame = cv2.resize(frame, (640, 480))
        if save_path:
            cv2.imwrite(save_path, frame)

        results = model.predict(frame, conf=0.25, verbose=False)
        zone = SEAT_ZONES.get(seat_id)
        if not zone:
            return False

        for box in results[0].boxes:
            x1, y1, x2, y2 = box.xyxy[0].tolist()
            cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
            if zone[0] <= cx <= zone[2] and zone[1] <= cy <= zone[3]:
                return True

        return False
    except Exception as e:
        print(f"AI 에러: {e}")
        return False


def send_to_server(seat_id, status, image_url=None):
    """Spring Boot 서버로 최종 판정 결과를 전송"""
    try:
        data = {
            "seat_id": SEAT_ID_MAP.get(seat_id, f"seat-{seat_id}"),
            "status": status,
            "image_url": image_url,
            "update_time": time.strftime("%Y-%m-%d %H:%M:%S"),
        }
        response = requests.post(BACKEND_URL, json=data, timeout=5)
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

        topic_parts = topic.split("/")
        if len(topic_parts) < 3:
            return

        seat_num = int(topic_parts[-1])
        if seat_num not in seat_data:
            return

        s = seat_data[seat_num]

        if "checkout" in topic:
            s["is_checked_out"] = True
            print(f"이벤트: {seat_num}번 좌석 퇴실 신호 수신")
            return

        try:
            pressure_value = int(payload)
        except ValueError:
            return

        is_person_present = pressure_value > 1500

        current_time = time.time()
        if current_time - s["last_ai_time"] >= 10:
            s["is_item_present"] = run_ai_analysis(seat_num)
            s["last_ai_time"] = current_time
            print(f"{seat_num}번 좌석 AI 분석 수행 완료")

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
                    if s["lost_item_start_time"] is None:
                        s["lost_item_start_time"] = time.time()
                else:
                    s["current_state"] = "사석화 의심 (자리비움)"
                    if s["suspicious_start_time"] is None:
                        s["suspicious_start_time"] = time.time()
            else:
                s["current_state"] = "정상 빈좌석"
                s["is_checked_out"] = False
                s["lost_item_start_time"] = None
                s["suspicious_start_time"] = None

        if s["current_state"] != s["last_sent_state"]:
            send_to_server(seat_num, s["current_state"])
            s["last_sent_state"] = s["current_state"]

        if s["lost_item_start_time"]:
            if (time.time() - s["lost_item_start_time"]) / 60 >= 20:
                if s["current_state"] != "분실물 확정":
                    s["current_state"] = "분실물 확정"
                    img_name = f"lost_seat{seat_num}_{int(time.time())}.jpg"
                    run_ai_analysis(seat_num, save_path=img_name)
                    send_to_server(seat_num, s["current_state"], image_url="pending_s3_url")
                    s["last_sent_state"] = s["current_state"]

        if s["suspicious_start_time"]:
            if (time.time() - s["suspicious_start_time"]) / 60 >= 15:
                if s["current_state"] != "사석화 확정":
                    s["current_state"] = "사석화 확정"
                    send_to_server(seat_num, s["current_state"])
                    s["last_sent_state"] = s["current_state"]

        print(
            f"[{time.strftime('%H:%M:%S')}] Seat {seat_num} | 상태: {s['current_state']} | "
            f"압력: {pressure_value} | 짐: {s['is_item_present']}"
        )
    except Exception as e:
        print(f"로직 에러 (Seat {seat_num if 'seat_num' in locals() else 'Unknown'}): {e}")


def on_disconnect(client, userdata, rc):
    if rc != 0:
        print("MQTT 연결 유실, 재접속 대기 중...")


client = mqtt.Client()
client.on_message = on_message
client.on_disconnect = on_disconnect

try:
    client.connect("localhost", 1883, 60)
    client.subscribe([("seat/status/#", 0), ("seat/checkout/#", 0)])
    print("시스템 가동 중... (6개 좌석 통합 관리 모드)")
    client.loop_forever()
except Exception as e:
    print(f"가동 실패: {e}")
