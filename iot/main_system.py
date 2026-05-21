import os
import time
import json

import cv2
import paho.mqtt.client as mqtt
import requests
from ultralytics import YOLO

# [설정] AI 모델 및 서버 주소
model = YOLO("yolov8n.pt")
CAM_URL = os.getenv("CAM_URL", "")
BACKEND_BASE_URL = os.getenv("BACKEND_BASE_URL", "http://13.209.33.104/api").rstrip("/")
MQTT_BROKER = os.getenv("MQTT_BROKER", "172.20.10.9")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
LOST_ITEM_SCAN_TOPIC = os.getenv("LOST_ITEM_SCAN_TOPIC", "admin/trigger_lost_item")
SEAT_ID_MAP = {
    1: "seat-1",
    2: "seat-2",
    3: "seat-3",
    4: "seat-4",
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
}

# ---------------------------------------------------------
# [상태 관리] 4개 좌석의 독립적인 상태를 저장하기 위한 딕셔너리
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
    for i in range(1, 5)
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


def post_json(path, payload):
    """Spring Boot 서버 최종 API 호출"""
    url = f"{BACKEND_BASE_URL}{path}"
    response = requests.post(url, json=payload, timeout=5)
    response.raise_for_status()
    return response


def send_to_server(seat_id, status, image_url=None):
    """현재 백엔드 API 규격에 맞춰 상태를 전송"""
    try:
        if "사석화" in status:
            payload = {"seat_num": seat_id, "status": "squatting"}
            response = post_json("/seat/squatting", payload)
            print(f"사석화 전송 성공 ({seat_id}번): {response.text}")
        elif status == "분실물 확정":
            payload = {
                "seat_num": seat_id,
                "image_url": image_url or "https://example.com/lost_item.jpg",
                "category": "unknown",
            }
            response = post_json("/seat/lost-item", payload)
            print(f"분실물 전송 성공 ({seat_id}번): {response.text}")
        else:
            print(f"전송 생략 ({seat_id}번): {status}")
    except Exception as e:
        print(f"서버 연결 실패: {e}")


def trigger_manual_lost_item_scan():
    """관리자 수동 스캔 명령을 처리하고 분실물을 바로 등록한다."""
    print("관리자 수동 분실물 스캔 시작")
    detected_count = 0

    for seat_id in range(1, 5):
        img_name = f"manual_lost_item_seat{seat_id}_{int(time.time())}.jpg"
        has_item = run_ai_analysis(seat_id, save_path=img_name)
        if not has_item:
            continue

        image_url = upload_to_s3(img_name, img_name) or "https://example.com/lost_item.jpg"
        send_to_server(seat_id, "분실물 확정", image_url=image_url)
        print(f"관리자 수동 분실물 등록 완료 ({seat_id}번)")
        detected_count += 1

    if detected_count == 0:
        print("관리자 수동 스캔 결과: 감지된 분실물 없음")
        return False

    print(f"관리자 수동 스캔 결과: {detected_count}건 등록")
    return True


def on_message(client, userdata, msg):
    global seat_data

    try:
        topic = msg.topic
        payload = msg.payload.decode().strip()

        if topic == LOST_ITEM_SCAN_TOPIC:
            command = None
            try:
                decoded = json.loads(payload) if payload else {}
                command = decoded.get("command")
            except json.JSONDecodeError:
                print(f"수동 스캔 명령 파싱 실패: {payload}")
                return

            if command == "detect":
                trigger_manual_lost_item_scan()
            else:
                print(f"지원하지 않는 수동 스캔 명령: {command}")
            return

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
    print(f"MQTT broker: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"Backend API: {BACKEND_BASE_URL}")
    client.connect(MQTT_BROKER, MQTT_PORT, 60)
    client.subscribe([("seat/status/#", 0), ("seat/checkout/#", 0), (LOST_ITEM_SCAN_TOPIC, 0)])
    print("시스템 가동 중... (4개 좌석 통합 관리 모드)")
    client.loop_forever()
except Exception as e:
    print(f"가동 실패: {e}")
