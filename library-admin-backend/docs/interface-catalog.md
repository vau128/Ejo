# Interface Catalog

외부 연동 담당자가 구현하거나 교체하게 될 백엔드 포트 목록이다.

## Stores

- `StudentAccountStore`
  - 학생 조회
  - 학생 저장
  - 세션 토큰 저장 및 조회

- `SeatStore`
  - 좌석 목록 조회
  - 좌석 단건 조회
  - 좌석 배정/해제

- `LostItemStore`
  - 분실물 목록 조회
  - 분실물 저장

- `UserSettingsStore`
  - 사용자 설정 조회
  - 사용자 설정 저장

## Gateways

- `DeviceEventGateway`
  - 라즈베리파이 heartbeat 수집
  - 좌석 상태 변경 이벤트 발행

- `ObjectStorageGateway`
  - S3 등 외부 스토리지 업로드

- `NotificationGateway`
  - 앱 푸시, 문자, 메일 등 알림 발송

## Ownership Suggestion

- MySQL 담당
  - `StudentAccountStore`
  - `SeatStore`
  - `LostItemStore`
  - `UserSettingsStore`

- IoT 담당
  - `DeviceEventGateway`

- AWS 담당
  - `ObjectStorageGateway`
  - `NotificationGateway`
