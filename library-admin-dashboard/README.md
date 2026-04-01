# AIoT Library Admin Dashboard

교내 도서관 좌석 관리용 관리자 웹 대시보드 예시 프로젝트입니다.

## 포함 페이지

- 관리자 로그인
- 관리자 조치 기능
- 알림 전송 이력
- 알림 관리
- 통계
- 3구역 좌석 확인
- 비정상 좌석
- 분실물 관리보드
- 시스템 상태 모니터링

## 실행

```bash
npm install
npm run dev
```

## 환경 변수

`.env` 파일을 만들고 아래 값을 설정하세요.

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

## 백엔드 연결

이 프로젝트는 아래 API를 사용합니다.

- `POST /auth/login`
- `GET /dashboard/overview`
- `GET /dashboard/actions`
- `POST /dashboard/actions/warning`
- `POST /dashboard/actions/release`
- `POST /dashboard/actions/resolve`
- `GET /dashboard/alerts/history`
- `GET /dashboard/alerts/management`
- `PATCH /dashboard/alerts/management/{ruleId}`
- `GET /dashboard/stats`
- `GET /dashboard/seats/zone-3`
- `GET /dashboard/seats/zone-3/{seatId}`
- `GET /dashboard/seats/abnormal`
- `GET /dashboard/lost-items`
- `PATCH /dashboard/lost-items/{itemId}`
- `GET /dashboard/system-status`
- `GET /dashboard/system-status/sensor-logs`

## 기본 로그인 계정

- ID: `admin`
- Password: `admin123`
