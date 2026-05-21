# Integration Handoff Draft

이 문서는 DB, IoT, AWS 담당자가 바로 연동 작업을 시작할 수 있게 현재 백엔드 기준의 계약을 정리한 초안이다.

## Current Backend Facts

- Base URL
  - App API: `/api/app`
  - Admin API: `/api/dashboard`
  - Admin Auth API: `/api/auth`
- Student app token header
  - `X-Student-Token`
- Android app default backend URL
  - `http://10.0.2.2:8080/api/app`

## Data Owners

- DB 담당
  - 사용자, 세션, 좌석, 분실물, 설정, 경고, 이벤트 로그 저장
- IoT 담당
  - 라즈베리파이 heartbeat, 센서 이벤트 업로드, 좌석 상태 판정 입력
- AWS 담당
  - 배포 환경 변수, 비밀값, S3 자산 저장, 운영 로그 집계

## Suggested Minimum Tables

- `users`
- `student_sessions`
- `seats`
- `seat_assignments`
- `seat_sensor_events`
- `warnings`
- `lost_item_reports`
- `user_settings`
- `device_heartbeats`

## Required Environment Keys

- Database
  - `DB_HOST`
  - `DB_PORT`
  - `DB_NAME`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `DB_SSL_MODE`
- IoT
  - `IOT_BASE_URL`
  - `IOT_API_KEY`
  - `IOT_TIMEOUT_MS`
  - `IOT_HEARTBEAT_THRESHOLD_SECONDS`
- AWS
  - `AWS_REGION`
  - `AWS_S3_BUCKET`
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`

## App API Contract Summary

### Auth

- `POST /api/app/auth/login`
  - request: `email`, `password`
  - response: `token`, `user`

- `POST /api/app/auth/signup`
  - request: `name`, `studentId`, `email`, `password`, `agreedToPrivacy`
  - response: `token`, `user`

### User

- `GET /api/app/me`
  - header: `X-Student-Token`
  - response: `user`

### Seats

- `GET /api/app/seats`
  - header: `X-Student-Token`
  - response: `summary`, `seats`

- `POST /api/app/seats/{seatId}/selection`
  - header: `X-Student-Token`
  - response: `message`

### Lost Items

- `GET /api/app/lost-items`
  - header: `X-Student-Token`
  - response: `reports`

### Settings

- `GET /api/app/settings`
  - header: `X-Student-Token`
  - response: `settings`

- `PATCH /api/app/settings`
  - header: `X-Student-Token`
  - request: `pushEnabled`, `seatAlertEnabled`, `warningAlertEnabled`
  - response: `message`, `settings`

## Admin API Contract Summary

- `GET /api/dashboard/overview`
- `GET /api/dashboard/actions`
- `POST /api/dashboard/actions/warning`
- `POST /api/dashboard/actions/release`
- `POST /api/dashboard/actions/resolve`
- `GET /api/dashboard/alerts/history`
- `GET /api/dashboard/alerts/management`
- `PATCH /api/dashboard/alerts/management/{ruleId}`
- `GET /api/dashboard/stats`
- `GET /api/dashboard/settings`
- `PATCH /api/dashboard/settings`
- `GET /api/dashboard/seats/zone-3`
- `GET /api/dashboard/seats/zone-3/{seatId}`
- `GET /api/dashboard/seats/abnormal`
- `GET /api/dashboard/lost-items`
- `PATCH /api/dashboard/lost-items/{itemId}`
- `GET /api/dashboard/system-status`
- `GET /api/dashboard/system-status/sensor-logs`

## IoT Event Draft

라즈베리파이 담당자는 아래 두 가지부터 맞추면 된다.

### Heartbeat

```json
{
  "deviceId": "edge-rpi-03",
  "zone": "zone-3",
  "seatId": "seat-12",
  "timestamp": "2026-05-04T12:30:00Z",
  "firmwareVersion": "1.0.0",
  "status": "ONLINE"
}
```

### Sensor Event

```json
{
  "deviceId": "edge-rpi-03",
  "seatId": "seat-12",
  "timestamp": "2026-05-04T12:30:05Z",
  "pressureValue": 842,
  "personDetected": true,
  "objectDetected": false,
  "cameraConfidence": 0.94,
  "derivedStatus": "OCCUPIED"
}
```

## AWS Handoff Notes

- 운영 시크릿은 코드에 두지 않는다.
- S3가 필요하면 키 네이밍 규칙을 먼저 정한다.
  - 예: `lost-items/{yyyy}/{MM}/{uuid}.jpg`
- 배포 환경은 최소 `dev`, `prod` 두 개를 유지한다.
- 로그는 request log, error log, device event log를 분리하는 편이 좋다.
