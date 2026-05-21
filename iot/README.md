# 📡 IoT & Edge Computing 파트 (사석화 방지 및 헬스케어 스마트 좌석 시스템)

이 디렉토리(`iot/`)는 도서관/스터디카페 좌석 사석화 방지 시스템 중 **엣지 서버(Raspberry Pi)의 지능형 상태 관리 로직**과 **센서 제어(ESP32) 펌웨어** 코드를 포함하고 있습니다. 

단순 착석 유무를 넘어, **다중 압력 센서를 활용한 사용자 헬스케어(자세 판별) 기능**과 **비전 AI(YOLO) 기반의 분실물 탐지 기능**이 통합된 고도화 버전입니다.

<br>

## ✨ 핵심 기술 및 로직 (Key Features)
- **🧘‍♂️ 다중 센서 퓨전 기반 헬스케어**: 좌석당 3개의 FSR 압력 센서(좌/우/등받이)를 배치하여 '정상', '다리 꼬기', '거북목' 등 실시간 자세를 판별하고 JSON 형태로 전송합니다.
- **🔋 하드웨어 전력 최적화 (Adaptive Deep Sleep)**: 비교기(DM2794)의 하드웨어 인터럽트를 활용해, 물리적인 착석 압력이 감지되었을 때만 ESP32를 깨워 배터리 효율을 극대화했습니다.
- **👁️ 1 Camera, 4 Seats (ROI Mapping)**: 단일 IP 카메라 영상의 가로 픽셀을 4등분(ROI)하여 독립적인 좌석 구역을 수학적으로 분리하고, 객체 탐지 시 분실물과 좌석 번호를 스스로 매핑합니다.
- **⏱️ 딕셔너리 기반 멀티태스킹**: 4개 좌석의 실시간 상태와 개별 사석화 타이머를 딕셔너리 구조로 병렬 처리하여 메인 서버의 부하를 엣지 단에서 분산시킵니다.

<br>

## 📁 파일 구성
- `main_system.py`: 여러 개의 좌석 통합 관리, AI 비전 분석(분실물 탐지), S3 이미지 업로드, MQTT(JSON) 수신, 백엔드(REST API) 전송을 수행하는 라즈베리 파이 메인 실행 파일입니다.
- `add_healthcare.ino`: 3개의 압력 센서 데이터를 읽고 헬스케어 자세를 연산한 뒤, 라즈베리 파이(MQTT 브로커)로 Publish 하는 ESP32 펌웨어 소스 코드입니다.
- `requirements.txt`: 엣지 서버 환경 구동에 필요한 Python 의존성 패키지 목록입니다. (Ultralytics, OpenCV, Paho-MQTT, Boto3 등)

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
실행 전 `edge_server.py` 내부의 `CAM_URL`(카메라 주소), API 엔드포인트 주소, `AWS_ACCESS_KEY` 등을 실제 테스트 환경에 맞게 수정해야 합니다.
```bash
python3 edge_server.py
```