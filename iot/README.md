# 📡 IoT & Edge Computing 파트 (사석화 방지 시스템)

이 디렉토리(`iot/`)는 도서관 좌석 사석화 방지 시스템 중 **엣지 서버(Raspberry Pi)의 지능형 상태 관리 로직**과 **센서 제어(ESP32) 펌웨어** 코드를 포함하고 있습니다. 

<br>

## ✨ IoT 핵심 로직 (Sensor Fusion)
- **1 Camera, 6 Seats**: 단일 IP 카메라 영상에서 6개의 독립적인 좌석 구역(Bounding Box)을 분리하여 객체를 개별 탐지합니다.
- **이중 교차 검증**: 압력 센서(물리적 무게)를 1차 필터로, YOLO Vision AI(객체 탐지)를 2차 필터로 결합하여 오작동을 방지합니다.
- **딕셔너리 기반 멀티태스킹**: 6개 좌석의 실시간 상태와 개별 타이머를 딕셔너리 구조로 병렬 처리합니다.

<br>

## 📁 파일 구성
- `main_system.py`: 여러 개의 좌석 통합 관리, AI 비전 분석, MQTT 수신, 백엔드(FastAPI) 전송을 수행하는 라즈베리 파이 메인 실행 파일입니다.
- `esp32_sensor.ino`: 압력 센서 데이터를 읽어 라즈베리 파이(MQTT 브로커)로 Publish하는 ESP32 펌웨어 소스 코드입니다.
- `requirements.txt`: 엣지 서버 환경 구동에 필요한 Python 의존성 패키지 목록입니다. (Ultralytics, OpenCV, Paho-MQTT 등)

<br>

## 🛠️ 실행 가이드 (라즈베리 파이 환경)

### 1. 패키지 설치
이 폴더의 가상환경 내에서 필수 라이브러리를 설치합니다.
```bash
pip install -r requirements.txt
```

### 2. MQTT 브로커 상태 확인
라즈베리 파이 내부의 Mosquitto 브로커가 실행 중이어야 하며, 외부(ESP32) 통신을 위해 `mosquitto.conf`에 `listener 1883`, `allow_anonymous true` 설정이 되어 있어야 합니다.
```bash
sudo systemctl status mosquitto
```

### 3. 메인 시스템 가동
실행 전 `main_system.py` 내부의 `CAM_URL`(카메라 주소), `FASTAPI_URL`(서버 주소), `SEAT_ZONES`(좌석별 좌표) 변수를 실제 테스트 환경에 맞게 수정해야 합니다.
```bash
python3 main_system.py
```
