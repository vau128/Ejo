# Security Setup

이 문서는 운영 배포 전에 반드시 넣어야 하는 관리자 인증 환경 변수와 HTTPS 적용 절차를 정리한다.

## 1. 관리자 시크릿 생성

아래 스크립트는 다음 두 값을 한 번에 만든다.

- `ADMIN_PASSWORD_HASH`
- `ADMIN_JWT_SECRET`

실행:

```bash
cd /Users/jiyun/development/Ejo
chmod +x library-admin-backend/scripts/generate-admin-security.sh
library-admin-backend/scripts/generate-admin-security.sh '원하는관리자비밀번호'
```

출력 예시:

```env
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH=$2a$10$...
ADMIN_JWT_SECRET=...
```

주의:

- `ADMIN_PASSWORD_HASH`만 운영에 넣고, `ADMIN_PASSWORD`는 비워두는 편이 안전하다.
- `ADMIN_JWT_SECRET`은 길고 랜덤한 값이어야 하며 운영마다 다르게 둔다.
- 출력 결과를 git에 커밋하지 않는다.

## 2. 백엔드 실행 전 환경 변수 주입

### 일회성 실행

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD_HASH='생성된해시값'
export ADMIN_JWT_SECRET='생성된랜덤시크릿'
export SPRING_PROFILES_ACTIVE=prod

mvn -f library-admin-backend/pom.xml spring-boot:run
```

### EC2 systemd 서비스 예시

서비스가 systemd로 돌고 있다면 아래처럼 서비스 override에 넣는 편이 안전하다.

```bash
sudo systemctl edit ejo-backend
```

추가 내용:

```ini
[Service]
Environment="ADMIN_USERNAME=admin"
Environment="ADMIN_PASSWORD_HASH=생성된해시값"
Environment="ADMIN_JWT_SECRET=생성된랜덤시크릿"
Environment="SPRING_PROFILES_ACTIVE=prod"
```

적용:

```bash
sudo systemctl daemon-reload
sudo systemctl restart ejo-backend
sudo systemctl status ejo-backend
```

서비스 이름이 `ejo-backend`가 아니라면 실제 이름으로 바꾼다.

## 3. HTTPS 적용

현재 프론트는 Amplify이므로 HTTPS가 기본이다. 백엔드도 반드시 HTTPS로 노출해야 Mixed Content가 나지 않는다.

### 추천 1: EC2 단일 서버라면 Nginx + Certbot

1. 백엔드는 `127.0.0.1:8080` 또는 `localhost:8080`에서 실행
2. Nginx가 `443`을 받고 Spring Boot `8080`으로 프록시
3. Certbot으로 인증서 발급
4. 프론트의 `VITE_API_BASE_URL`을 `https://백엔드도메인`으로 변경

Nginx 예시:

```nginx
server {
    listen 80;
    server_name api.ssejo.site;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.ssejo.site;

    ssl_certificate /etc/letsencrypt/live/api.ssejo.site/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.ssejo.site/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

인증서 발급:

```bash
sudo certbot --nginx -d api.ssejo.site
```

### 추천 2: 이미 ALB가 있다면 ACM 사용

1. ACM에서 `api.ssejo.site` 인증서 발급
2. ALB `443` 리스너에 인증서 연결
3. Target Group을 EC2 `8080`으로 연결
4. DNS를 ALB로 연결
5. 프론트 API 주소를 `https://api.ssejo.site`로 변경

## 4. 프론트 설정

Amplify 또는 프론트 `.env`의 API 주소는 최종적으로 HTTPS 주소여야 한다.

```env
VITE_API_BASE_URL=https://api.ssejo.site
```

## 5. 배포 후 확인

```bash
curl -i https://api.ssejo.site/api/test
curl -i -X POST https://api.ssejo.site/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"관리자비밀번호"}'
```

로그인 응답의 `token`을 복사한 뒤 관리자 API 확인:

```bash
curl -i https://api.ssejo.site/api/dashboard/overview \
  -H 'Authorization: Bearer 발급된토큰'
```

정상이면:

- `/api/test`는 200
- `/api/auth/login`은 200
- 토큰 없이 `/api/dashboard/overview`는 401
- 토큰과 함께 `/api/dashboard/overview`는 200
