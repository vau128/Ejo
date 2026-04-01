# Library Admin Backend

AIoT 도서관 관리자 대시보드용 Spring Boot 백엔드 예시입니다.

## 실행

```bash
mvn spring-boot:run
```

## 기본 정보

- Port: `8080`
- Base URL: `http://localhost:8080/api`
- 기본 로그인 계정: `admin / admin123`

## 제공 API

### 인증
- `POST /api/auth/login`

### 관리자 대시보드
- `GET /api/dashboard/overview`
- `GET /api/dashboard/actions`
- `POST /api/dashboard/actions/warning`
- `POST /api/dashboard/actions/release`
- `POST /api/dashboard/actions/resolve`
- `GET /api/dashboard/alerts/history`
- `GET /api/dashboard/alerts/management`
- `PATCH /api/dashboard/alerts/management/{ruleId}`
- `GET /api/dashboard/stats`
- `GET /api/dashboard/seats/zone-3?status=&search=`
- `GET /api/dashboard/seats/zone-3/{seatId}`
- `GET /api/dashboard/seats/abnormal`
- `GET /api/dashboard/lost-items`
- `PATCH /api/dashboard/lost-items/{itemId}`
- `GET /api/dashboard/system-status`
- `GET /api/dashboard/system-status/sensor-logs`

## 구조 설명

- 프론트엔드 로그인 페이지와 연결되는 인증 API
- 3구역 좌석 상태, 비정상 좌석, 알림 이력, 분실물, 시스템 상태를 위한 페이지별 API
- 실제 DB 대신 메모리 기반 더미 데이터를 사용하지만, 추후 JPA/RDS 연동으로 확장하기 쉽게 서비스 계층으로 분리
