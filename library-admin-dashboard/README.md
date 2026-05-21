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
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_API_BASE_URL`에는 백엔드의 origin만 넣으면 됩니다. 프론트 내부에서 `/api`를 자동으로 붙이므로 `http://localhost:8080/api`처럼 넣어도 동작은 하지만, 배포 시에는 `http://EC2_PUBLIC_IP:8080` 형태로 통일하는 편이 안전합니다.

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

## AWS Amplify 배포

모노레포 루트인 `Ejo`에 [amplify.yml](/Users/jiyun/development/Ejo/amplify.yml)를 두고, Amplify에서 이 저장소를 연결한 뒤 `library-admin-dashboard` 앱만 빌드하도록 설정합니다.

필수 환경 변수:

```bash
VITE_API_BASE_URL=http://EC2_PUBLIC_IP:8080
```

배포 후 확인 항목:

- 우하단에 `Backend Connected` 표시가 나오는지 확인
- 브라우저 Console에 `{ message: "server ok" }` 응답이 출력되는지 확인
- 로그인, 대시보드 조회 등 실제 API 호출이 정상 동작하는지 확인
- Amplify 기본 도메인은 HTTPS인데 백엔드가 HTTP면 브라우저가 Mixed Content로 차단할 수 있음

Mixed Content가 발생하면 프론트와 백엔드 중 하나를 바꾸는 것이 아니라, 백엔드도 HTTPS로 노출해야 합니다. 일반적으로는 ALB 또는 Nginx 리버스 프록시 앞단에 ACM 인증서를 붙여 `https://...` 로 API를 제공하는 방식이 필요합니다.
