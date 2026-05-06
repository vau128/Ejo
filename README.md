# 🪑 AIoT 기반 도서관 좌석 사석화 방지 시스템 (Edge-Computing)

> **도서관 열람실의 고질적인 사석화 및 분실물 방지를 위한 지능형 모니터링 시스템**
>
> **ESP32 센서 퓨전 기술과 Edge AI, FastAPI 기반 통합 관리 솔루션**

본 프로젝트는 대학 도서관 등 공공 학습 공간에서 빈번하게 발생하는 얌체족(사석화) 문제와 분실물 관리의 어려움을 해결하기 위해 기획되었습니다. **ESP32 기반 압력 센서**와 **Raspberry Pi 엣지 서버의 YOLO 비전 AI**를 결합한 **센서 퓨전(Sensor Fusion)** 전략을 통해 오작동률을 최소화하고, 실시간 좌석 상태를 **관리자 및 사용자 앱/웹**으로 전달하는 지능형 공간 관리 인프라 솔루션입니다.

<br>
<br>

## 📺 Project Preview

| 📱 실시간 사용자/관리자 앱 (App) | ⚙️ Edge AI 객체 탐지 및 서버 연동 |
| :---: | :---: |
| ![App Preview](docs/시연_앱화면_임시.gif) | ![Edge Preview](docs/image_b46b3c.png) |

*(※ 위 이미지 경로(`docs/...`)는 실제 깃허브에 올릴 시연 gif/이미지 이름으로 수정해 주세요.)*

<br>
<br>

## 🚀 주요 기능 (Key Features)

### 1. 센서 퓨전 (Sensor Fusion) 기반 이중 교차 검증
- **1차 필터링 (물리적 무게)**: ESP32와 압력 센서를 통해 사용자의 실제 착석 여부를 실시간으로 감지
- **2차 필터링 (객체 탐지)**: 사람이 자리를 비웠을 때만 카메라와 YOLOv8n 모델이 작동하여 구역 내 소지품 유무를 판별

### 2. 지능형 타이머 및 상태 관리 시스템
- **사석화 자동 감지**: 착석 상태에서 '사람 없음 + 짐 있음' 상태로 전환 시 사석화 의심 타이머 가동 (15분 지속 시 사석화 최종 확정)
- **분실물 보호 로직**: 앱을 통해 사용자가 정상 '퇴실' 처리를 했으나 짐이 감지될 경우, 20분 경과 후 분실물 확정 및 관리자 호출

### 3. Edge Computing 기반 멀티태스킹 최적화
- **1 Camera, 6 Seats**: 단일 IP 카메라 영상에서 6개의 독립적인 좌석 Bounding Box 구역을 설정하여 다중 객체 동시 탐지
- **딕셔너리 메모리 구조**: 6개 좌석의 센서 데이터, AI 판별 결과, 개별 타이머를 병렬로 관리하여 라즈베리 파이 환경의 부하 최소화

### 4. 데이터 전송 및 클라우드 연동
- **S3 현장 증거 보존**: 사석화 및 분실물 확정 시, 해당 시점의 좌석 캡처 이미지를 AWS S3 버킷에 자동 업로드
- **FastAPI 통합**: 최종 상태값 및 이미지 URL을 FastAPI 메인 서버로 즉각 전송하여 데이터베이스 및 모바일 앱 최신화

<br>
<br>

## 🛠 Tech Stack

### AI & Vision
![YOLO](https://img.shields.io/badge/YOLOv8-00FFFF?style=for-the-badge&logo=yolo&logoColor=black)
![OpenCV](https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)

### Hardware & Edge Computing
![Raspberry Pi](https://img.shields.io/badge/Raspberry_Pi_5-A22846?style=for-the-badge&logo=raspberrypi&logoColor=white)
![ESP32](https://img.shields.io/badge/ESP32-E7352C?style=for-the-badge&logo=espressif&logoColor=white)
![C](https://img.shields.io/badge/C-A8B9CC?style=for-the-badge&logo=c&logoColor=white)

### Backend & Cloud
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

### Frontend
![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Flutter](https://img.shields.io/badge/Flutter-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Dart](https://img.shields.io/badge/Dart-0175C2?style=for-the-badge&logo=dart&logoColor=white)

### Communication & Tools
![MQTT](https://img.shields.io/badge/MQTT-660066?style=for-the-badge&logo=mqtt&logoColor=white)
![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)
![Notion](https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white)

<br>
<br>

## 🏗 시스템 아키텍처 (System Architecture)

![시스템 아키텍처](assets/이조_시스템아키텍처.png)

<br>
<br>

## 🔗 관련 문서 (Documents)
- [Team Notion: 프로젝트 관리 및 회의록](여기에 노션 링크)
- [Figma: 앱 와이어프레임 및 디자인](여기에 피그마 링크)

<br>
<br>

## 🛠 협업 규칙 (Collaboration Rules)

### 📌 Git Commit Convention
팀원 간의 원활한 코드 리뷰와 히스토리 파악을 위해 아래의 커밋 메시지 규칙을 준수합니다.
- 메시지 형식: `태그: 작업 내용` (예: `feat: 6개 좌석 통합 관리 딕셔너리 로직 구현`)

| 태그 | 설명 |
| :--- | :--- |
| **feat** | 새로운 기능 추가 |
| **fix** | 버그 수정 |
| **design** | UI 디자인 수정 및 레이아웃 작업 |
| **docs** | 문서 수정 (README, PRD, SRS, PPT 등) |
| **refactor** | 코드 리팩토링 (기능 변경 없는 코드 구조 개선) |
| **chore** | 빌드 업무, 패키지 매니저 설정, .gitignore 수정 등 |
| **test** | 테스트 코드 추가 및 리팩토링 |

### 🌿 Branch Strategy
- **main**: 제품 출고 및 최종 발표용 브랜치 (가장 안정적인 버전)
- **develop**: 다음 출시 버전을 위한 개발 통합 브랜치
- **feature/기능명**: 각 파트별 세부 기능 개발 브랜치 (예: `feature/iot`, `feature/app`)

### 🤝 Code Review & Merge
- 모든 기능 개발은 `feature/` 브랜치에서 진행합니다.
- 개발 완료 후 `develop` 또는 `main` 브랜치로 Pull Request(PR)를 생성합니다.
- 최소 1명 이상의 팀원 승인(Approve)을 득한 후 Merge하는 것을 원칙으로 합니다.
