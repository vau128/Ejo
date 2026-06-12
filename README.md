# AIoT 기반 도서관 사석화 및 분실물 탐지 자동화 시스템

<br/>
<br/>

# 0. Getting Started (시작하기)
```bash
# 1. 백엔드 및 웹 대시보드 가동
$ cd Ejo
$ make check-env
$ make web-dev

$ cd Ejo
$ SPRING_PROFILES_ACTIVE=h2 mvn -f library-admin-backend/pom.xml spring-boot:run

# 2. 학생용 모바일 앱 실행
$ cd Ejo
$ make web-dev
```
*(※ 로컬 H2 인메모리 DB 가동 시: `SPRING_PROFILES_ACTIVE=h2 mvn -f library-admin-backend/pom.xml spring-boot:run`)*

<br/>
<br/>

# 1. Project Overview (프로젝트 개요)
- **프로젝트 이름**: AIoT 기반 도서관 사석화 및 분실물 탐지 자동화 시스템
- **프로젝트 설명**: 도서관의 고질적인 사석화 방지와 분실물 탐지, 사용자의 헬스케어를 위한 지능형 공간 관리 인프라 (ESP32 3채널 압력 센서 기술과 Edge AI, Spring Boot 기반 통합 관리 솔루션)

<br/>
<br/>

# 2. Development Background (개발 배경)
- **현장 문제 및 한계**: 도서관 예약 권한 정보만으로는 물품 방치 상태를 파악하기 어려우며, 순찰 민원 및 분실물 처리 이력이 분산되어 효율적인 관리가 제한됩니다.
- **보완 방식**: 3채널 압력 센서로 실제 착석 유무와 자세를 분류하고, YOLOv8s 모델 기반의 객체 탐지 이력을 기록하여 자동화된 운영 보조 계층을 구축합니다.

<br/>
<br/>

# 3. Development Objectives (개발 목표)
- **통합 연동**: 다양한 상태 이벤트를 일관된 단일 데이터 흐름으로 연결하여 시스템 시작부터 종료까지 끊김 없는 시나리오를 검증합니다.
- **역할별 최적화**: 저전력 센서 데이터 수집(IoT), 십자 사분면 내 핵심 물품 클래스 탐지(AI), 다중 토큰 기반 접근 제어 API 제공(서버)을 명확한 최종 기준으로 설정합니다.

<br/>
<br/>

# 4. Team Members (팀원 및 팀 소개)
| 이지윤 | 김현서 | 정예린 | 한승찬 |
|:------:|:------:|:------:|:------:|
| PL / Web & App | AI Vision | IoT / Embedded | DB / API |
| [GitHub](https://github.com/vau128) | [GitHub](https://github.com/kimhyunseoo) | [GitHub](https://github.com/yerin040821) | [GitHub](https://github.com/halong61) |

<br/>
<br/>

# 5. Key Features & Demo Videos (주요 기능 및 시연 영상)

### 1. 사석화 판별 및 동적 타이머 (AIoT 융합 판정)
- **발권 상태 동적 검증**: 사용자가 부재(`status: 0`)할 경우 백엔드 API를 조회하여 실제 '체크인' 유무를 교차 검증한 후 사석화 예약 타이머를 기동합니다.
- **멀티 가용 타이머 프레임워크**: 라즈베리파이 내부에서 `threading.Timer` 딕셔너리 구조를 구현하여 4개 좌석의 공백 상태를 개별·병렬 관리하며, 관리자 원격 제어 토픽으로 제한 시간을 동적 업데이트합니다.

<div align="center">
  <a href="https://youtu.be/goUO3mhXJfM" target="_blank">
    <img src="https://img.youtube.com/vi/goUO3mhXJfM/maxresdefault.jpg" alt="사석화 판별 시연 영상" width="60%">
  </a>
  <br/>
  🎬 <b><a href="https://youtu.be/goUO3mhXJfM" target="_blank">[시연 영상] AIoT 융합 사석화 판별 및 동적 타이머 가동</a></b>
</div>

<br/>

### 2. 비전 AI 분실물 탐지
- **YOLOv8s 기반 타겟 분실물 탐지**: 학습된 커스텀 모델을 활용하여 10대 핵심 카테고리(배낭, 책, 노트북 등)를 정밀 탐지합니다.
- **개별 객체 크롭(Crop) 및 증거 자동 보존**: 원본 영상 전체가 아닌 탐지된 분실물의 BBox 영역만 개별 크롭하여 AWS S3에 자동 업로드하고 DB에 즉시 동기화합니다.
- **2x2 사분면 수학적 영역 분할**: 단일 IP 카메라 영상의 중심을 기준으로 화면을 십자 형태(2x2)로 분할하여 독립적인 4개의 좌석 구역(ROI)을 매핑합니다.

<div align="center">
  <a href="https://youtu.be/g0q-B1n7t6I" target="_blank">
    <img src="https://img.youtube.com/vi/g0q-B1n7t6I/maxresdefault.jpg" alt="분실물 자동 탐지 시연 영상" width="60%">
  </a>
  <br/>
  🎬 <b><a href="https://youtu.be/g0q-B1n7t6I" target="_blank">[시연 영상] YOLOv8s 기반 분실물 자동 탐지 및 S3 크롭 업로드</a></b>
</div>

<br/>

### 3. 3채널 압력 매핑 기반 헬스케어 (자세 분석)
- **다중 FSR 센서 분석**: 좌석당 3개의 FSR 압력 센서(좌측 엉덩이, 우측 엉덩이, 등받이)를 배치하여 체중 분포를 실시간 분석합니다.
- **실시간 자세 판별 알고리즘**: 양측 압력 값의 편차 및 등받이 접촉 여부를 종합 연산하여 '정상', '왼쪽/오른쪽으로 기울어짐(다리 꼬기)', '거북목/허리 숙임' 등의 자세를 추출하고 축적된 데이터 기반의 주간 자세 통계 화면을 제공합니다.

<div align="center">
  <a href="https://youtu.be/RlNClZ9IW90" target="_blank">
    <img src="https://img.youtube.com/vi/RlNClZ9IW90/maxresdefault.jpg" alt="헬스케어 자세 분석 시연 영상" width="60%">
  </a>
  <br/>
  🎬 <b><a href="https://youtu.be/RlNClZ9IW90" target="_blank">[시연 영상] 3채널 압력 매핑 기반 실시간 자세 분석 및 헬스케어 통계</a></b>
</div>

<br/>
<br/>

# 6. System Architecture (시스템 아키텍처)
<div align="center">
  <img src="assets/이조_시스템아키텍처.png" alt="시스템 아키텍처" width="100%"/>
</div>

- **IoT Devices (ESP32)**: 좌/우/등받이 3채널 압력 센서 데이터를 수집하여 Wi-Fi 기반 MQTT 통신으로 이벤트 발행
- **Edge Gateway (Raspberry Pi 5)**: Mosquitto MQTT 브로커 구동, IP 카메라의 RTSP 스트림 분석, YOLOv8s 기반 분실물 추론, ROI 매핑 및 S3 파일 업로드 처리
- **AWS Cloud (Spring Boot & MySQL)**: 영속성 레이어 관리 및 데이터 정규화, 사용자 역할별 REST API 및 실시간 데이터 제공
- **End Users**: React 대시보드 웹(관리자용) 및 Flutter 모바일 앱(학생용)으로 이원화된 인터페이스 연동

<br/>
<br/>

# 7. Tasks & Responsibilities (작업 및 역할 분담)
| 팀원 | 담당 업무 |
|---|---|
| **이지윤** | <ul><li>요구사항 정의 및 UI/UX 와이어프레임 설계</li><li>React 관리자 웹 대시보드 개발</li><li>Flutter 학생용 모바일 앱 연동</li><li>Spring Boot 백엔드 REST API 구현</li></ul> |
| **김현서** | <ul><li>Roboflow Custom Dataset 구축</li><li>YOLO 계열 모델(v5, v8, v10, v11) 성능 교차 비교 검증</li><li>YOLOv8s 모델 선정 및 10대 핵심 물품 추론 최적화</li></ul> |
| **정예린** | <ul><li>FSR 406 3채널 압력 센서 하드웨어 회로 설계</li><li>ESP32 임베디드 펌웨어 구현 및 알고리즘 구축</li><li>라즈베리파이 로컬 MQTT(Mosquitto) 송수신 로직 구현</li></ul> |
| **한승찬** | <ul><li>데이터베이스 스키마 및 Entity 구조 설계, ERD 모델링</li><li>전체 통합 일정 관리</li><li>시스템 아키텍쳐 및 알고리즘</li></ul> |

<br/>
<br/>

# 8. Technology Stack (기술 스택)

### 8.1 AI & Vision
![YOLOv8](https://img.shields.io/badge/YOLOv8-00FFFF?style=for-the-badge&logo=yolo&logoColor=black)
![OpenCV](https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)

### 8.2 Hardware & Edge Computing
![Raspberry Pi 5](https://img.shields.io/badge/Raspberry_Pi_5-A22846?style=for-the-badge&logo=raspberrypi&logoColor=white)
![ESP32](https://img.shields.io/badge/ESP32-E7352C?style=for-the-badge&logo=espressif&logoColor=white)
![MQTT](https://img.shields.io/badge/MQTT-3C525C?style=for-the-badge&logo=mqtt&logoColor=white)
![C](https://img.shields.io/badge/C-A8B9CC?style=for-the-badge&logo=c&logoColor=white)

### 8.3 Backend & Cloud
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logicColor=white)
![Amazon EC2](https://img.shields.io/badge/Amazon_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

### 8.4 Frontend
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Dart](https://img.shields.io/badge/Dart-0175C2?style=for-the-badge&logo=dart&logoColor=white)

<br/>
<br/>

# 9. Project Structure (프로젝트 구조)
```plaintext
Ejo/
├── library-admin-dashboard/ # React/Vite 기반 관리자 웹 대시보드
│  ├── src/                 # 실시간 좌석 현황, 알림 이력, 통계 분석 컴포넌트
│  └── package.json
├── library-admin-backend/   # Spring Boot 기반 관리자/학생 공용 API 서버
│  ├── src/main/java/       # Controller, Service, Entity, JWT 보안 설정
│  └── pom.xml
├── app/                     # Flutter/Dart 기반 학생용 모바일 앱
│  ├── lib/                 # 실시간 좌석 조회, 나의 자리, 자세 통계 위젯
│  └── pubspec.yaml
└── iot/                     # Raspberry Pi & ESP32 엣지 컴퓨팅 소스코드
    ├── add_healthcare.ino   # FSR 3채널 임베디드 펌웨어
    └── main_system.py       # YOLOv8s 추론, 사석화 타이머 및 S3 자동 업로드 
```
