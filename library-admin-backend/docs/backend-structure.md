# Backend Structure Proposal

이 문서는 현재 mock 기반 백엔드를 실제 DB, IoT, AWS 구현으로 교체하기 쉽게 만들기 위한 구조 제안이다.

## Recommended Package Layout

```text
com.example.librarydashboard
├── config
├── controller
├── dto
├── service
├── domain
│   ├── model
│   └── enum
├── port
│   ├── in
│   └── out
├── adapter
│   ├── in
│   │   └── web
│   └── out
│       ├── memory
│       ├── mysql
│       ├── iot
│       └── aws
└── support
    ├── exception
    └── response
```

## How To Split Responsibilities

- `controller`: HTTP 요청/응답만 담당
- `service`: 유스케이스 흐름 담당
- `port.out`: DB, IoT, AWS 연동 인터페이스
- `adapter.out.memory`: 현재 mock 구현 유지
- `adapter.out.mysql`: MySQL 담당자 구현 영역
- `adapter.out.iot`: 라즈베리파이 또는 IoT API 담당자 구현 영역
- `adapter.out.aws`: S3, Secrets Manager, SNS 등 AWS 담당자 구현 영역

## Immediate Refactor Target

- `AppService`: 인증, 학생정보, 좌석, 분실물, 설정 관련 유스케이스 분리
- `DashboardService`: 관리자 액션, 알림 이력, 기기 상태, 센서 로그 유스케이스 분리
- 현재 `Map<String, Object>` 중심 구조는 유지 가능하지만, 외부 연동 직전에는 DTO 또는 domain model로 단계적 전환 권장

## Suggested First Extraction

1. `AppService`에서 저장소 접근 로직을 `StudentAccountStore`, `SeatStore`, `LostItemStore`, `UserSettingsStore`로 이동
2. 알림 발송과 기기 이벤트 전달은 `NotificationGateway`, `DeviceEventGateway`로 분리
3. 파일 저장이 필요해질 경우 `ObjectStorageGateway`를 통해 S3 연결
