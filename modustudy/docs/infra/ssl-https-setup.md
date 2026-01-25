# SSL/HTTPS 설정 가이드

## 개요

Squiz 프로젝트의 HTTPS 적용 과정을 설명합니다. Let's Encrypt 무료 인증서를 사용합니다.

---

## 아키텍처

```
                        Internet
                            │
                            ▼
                    ┌───────────────┐
                    │   :443 HTTPS  │
                    │    (Nginx)    │
                    └───────┬───────┘
                            │ SSL 종료
                            ▼
              ┌─────────────────────────────┐
              │      내부 네트워크 (HTTP)    │
              │                             │
              │  backend:8080 (HTTP)        │
              │  sfu-server:4000 (HTTP)     │
              │  recorder:3001 (HTTP)       │
              └─────────────────────────────┘

※ SFU Server는 WebRTC 미디어용으로 별도 SSL 인증서 사용
```

---

## 1. Let's Encrypt 인증서 발급

### 1.1 Certbot 설치

```bash
sudo apt update
sudo apt install certbot -y
```

### 1.2 인증서 발급

**중요**: 인증서 발급 전 80번 포트를 사용하는 서비스(nginx) 중지 필요

```bash
# nginx 컨테이너 중지
cd /home/ubuntu/squiz
docker-compose stop nginx

# 인증서 발급 (standalone 모드)
sudo certbot certonly --standalone -d i14d106.p.ssafy.io
```

### 1.3 발급된 인증서 경로

```
/etc/letsencrypt/live/i14d106.p.ssafy.io/
├── fullchain.pem    # 인증서 체인 (서버 인증서 + 중간 인증서)
├── privkey.pem      # 개인 키
├── cert.pem         # 서버 인증서
└── chain.pem        # 중간 인증서
```

### 1.4 인증서 확인

```bash
sudo certbot certificates
```

출력 예시:
```
Certificate Name: i14d106.p.ssafy.io
    Domains: i14d106.p.ssafy.io
    Expiry Date: 2025-04-XX (VALID: 89 days)
    Certificate Path: /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem
    Private Key Path: /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem
```

---

## 2. Nginx HTTPS 설정

### 2.1 nginx 설정 파일 (`nginx/conf.d/default.conf`)

```nginx
# ===========================================
# HTTP Server (80 → HTTPS 리다이렉트)
# ===========================================
server {
    listen 80;
    server_name i14d106.p.ssafy.io;

    # Let's Encrypt 인증서 갱신용
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # 나머지는 HTTPS로 리다이렉트
    location / {
        return 301 https://$host$request_uri;
    }
}

# ===========================================
# HTTPS Server (443)
# ===========================================
server {
    listen 443 ssl;
    http2 on;
    server_name i14d106.p.ssafy.io;

    # SSL 인증서 (./ssl/ 디렉토리에서 복사된 파일)
    ssl_certificate /ssl/fullchain.pem;
    ssl_certificate_key /ssl/privkey.pem;

    # SSL 보안 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    # Frontend (React)
    location / {
        root   /usr/share/nginx/html;
        index  index.html;
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Backend WebSocket
    location /ws/ {
        proxy_pass http://backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # SFU Server
    location /sfu/ {
        proxy_pass http://sfu-server:4000/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 86400;
    }

    # Recorder
    location /recorder/ {
        proxy_pass http://recorder:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 2.2 docker-compose.yml 볼륨 마운트

**중요**: Let's Encrypt 인증서는 심볼릭 링크로 되어 있어 Docker 컨테이너에서 직접 마운트 시 인식하지 못할 수 있습니다. 실제 파일을 `./ssl/` 디렉토리에 복사하여 사용합니다.

```yaml
nginx:
  build:
    context: ./frontend
    dockerfile: Dockerfile
  container_name: squiz-nginx
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./nginx/conf.d:/etc/nginx/conf.d:ro
    - ./ssl:/ssl:ro                                 # SSL 인증서 (복사본)
```

### 2.3 SSL 인증서 복사 (심볼릭 링크 해결)

Let's Encrypt 인증서를 실제 파일로 복사합니다:

```bash
# SSL 디렉토리 생성
sudo mkdir -p /home/ubuntu/squiz/ssl

# 인증서 복사 (-L: 심볼릭 링크를 실제 파일로)
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem /home/ubuntu/squiz/ssl/
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem /home/ubuntu/squiz/ssl/

# 권한 설정
sudo chmod 644 /home/ubuntu/squiz/ssl/fullchain.pem
sudo chmod 644 /home/ubuntu/squiz/ssl/privkey.pem
```

### 2.4 nginx 설정에서 인증서 경로

복사된 인증서를 사용하도록 설정:

```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name i14d106.p.ssafy.io;

    ssl_certificate /ssl/fullchain.pem;        # 컨테이너 내부 경로
    ssl_certificate_key /ssl/privkey.pem;
    # ...
}
```

---

## 3. SFU Server SSL 설정

SFU 서버는 WebRTC 미디어 전송을 위해 별도의 SSL 인증서가 필요합니다.

### 3.1 인증서 복사

Let's Encrypt 인증서를 SFU 서버용으로 복사:

```bash
# ssl 디렉토리 생성
mkdir -p /home/ubuntu/squiz/ssl

# 인증서 복사 (-L 옵션: 심볼릭 링크 따라가기)
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem /home/ubuntu/squiz/ssl/sfu-key.pem
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem /home/ubuntu/squiz/ssl/sfu-cert.pem

# 권한 설정
sudo chown ubuntu:ubuntu /home/ubuntu/squiz/ssl/*
chmod 644 /home/ubuntu/squiz/ssl/*
```

### 3.2 docker-compose.yml SFU 설정

```yaml
sfu-server:
  build:
    context: ./sfu-server
    dockerfile: Dockerfile
  container_name: squiz-sfu
  ports:
    - "4000:4000"
    - "20000-22000:20000-22000/udp"
  environment:
    - NODE_ENV=production
    - SFU_SSL_KEY_PATH=/ssl/sfu-key.pem
    - SFU_SSL_CERT_PATH=/ssl/sfu-cert.pem
    - SFU_ANNOUNCED_IP=i14d106.p.ssafy.io
  volumes:
    - ./ssl:/ssl:ro
```

---

## 4. 인증서 갱신

Let's Encrypt 인증서는 **90일** 유효합니다. 자동 갱신 설정이 필요합니다.

### 4.1 수동 갱신 테스트

```bash
sudo certbot renew --dry-run
```

### 4.2 자동 갱신 크론잡 설정

```bash
sudo crontab -e
```

다음 내용 추가:
```cron
# 매월 1일, 15일 새벽 3시에 인증서 갱신 시도
0 3 1,15 * * certbot renew --quiet --post-hook "cd /home/ubuntu/squiz && docker-compose restart nginx"
```

### 4.3 SFU 인증서 갱신 스크립트

`/home/ubuntu/squiz/renew-ssl.sh`:
```bash
#!/bin/bash

# Let's Encrypt 인증서 갱신
certbot renew --quiet

# SFU 서버용 인증서 복사
cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem /home/ubuntu/squiz/ssl/sfu-key.pem
cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem /home/ubuntu/squiz/ssl/sfu-cert.pem

# 컨테이너 재시작
cd /home/ubuntu/squiz
docker-compose restart nginx sfu-server
```

```bash
chmod +x /home/ubuntu/squiz/renew-ssl.sh
```

크론잡 수정:
```cron
0 3 1,15 * * /home/ubuntu/squiz/renew-ssl.sh
```

---

## 5. 트러블슈팅

### 5.1 인증서 발급 실패

**증상**: `certbot certonly` 실패

**원인**: 80번 포트가 이미 사용 중

**해결**:
```bash
docker-compose stop nginx
sudo certbot certonly --standalone -d i14d106.p.ssafy.io
docker-compose up -d
```

### 5.2 Nginx SSL 로드 실패 (심볼릭 링크 문제)

**증상**: `cannot load certificate "/etc/letsencrypt/live/...": BIO_new_file() failed`

**원인**: Let's Encrypt 인증서가 심볼릭 링크로 되어 있어 Docker 컨테이너에서 인식 불가

**해결**: 인증서를 실제 파일로 복사
```bash
# 인증서 복사 (-L: 심볼릭 링크 해제)
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/fullchain.pem /home/ubuntu/squiz/ssl/
sudo cp -L /etc/letsencrypt/live/i14d106.p.ssafy.io/privkey.pem /home/ubuntu/squiz/ssl/

# nginx 설정에서 경로 변경
ssl_certificate /ssl/fullchain.pem;
ssl_certificate_key /ssl/privkey.pem;

# docker-compose.yml에 볼륨 추가
volumes:
  - ./ssl:/ssl:ro
```

### 5.3 Nginx SSL 볼륨 마운트 누락

**증상**: `ls: /ssl/: No such file or directory` (컨테이너 내부)

**원인**: docker-compose.yml에 SSL 볼륨 마운트 누락

**해결**: nginx 서비스에 볼륨 추가
```yaml
nginx:
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ./nginx/conf.d:/etc/nginx/conf.d:ro
    - ./ssl:/ssl:ro  # 이 줄 추가
```

### 5.4 Docker Compose 타임아웃

**증상**: `UnixHTTPConnectionPool... Read timed out`

**해결**:
```bash
export COMPOSE_HTTP_TIMEOUT=300
docker-compose down --remove-orphans
docker-compose up -d --build
```

### 5.5 ContainerConfig 에러

**증상**: `KeyError: 'ContainerConfig'`

**원인**: docker-compose 버전과 Docker 버전 호환성 문제

**해결**:
```bash
docker-compose down --remove-orphans
docker-compose up -d --build
```

---

## 6. 확인 방법

### 6.1 HTTPS 접속 테스트

```bash
curl -I https://i14d106.p.ssafy.io/
```

정상 응답:
```
HTTP/2 200
server: nginx
content-type: text/html
...
```

### 6.2 SSL 인증서 정보 확인

```bash
echo | openssl s_client -connect i14d106.p.ssafy.io:443 2>/dev/null | openssl x509 -noout -dates
```

출력 예시:
```
notBefore=Jan 20 00:00:00 2025 GMT
notAfter=Apr 20 23:59:59 2025 GMT
```

### 6.3 컨테이너 상태 확인

```bash
docker-compose ps
```

모든 컨테이너가 `Up` 상태인지 확인

---

## 파일 구조

```
/home/ubuntu/squiz/
├── docker-compose.yml
├── .env                          # Git 미포함
├── ssl/                          # Git 미포함
│   ├── sfu-key.pem
│   └── sfu-cert.pem
└── nginx/
    ├── nginx.conf
    └── conf.d/
        └── default.conf

/etc/letsencrypt/live/i14d106.p.ssafy.io/
├── fullchain.pem
├── privkey.pem
├── cert.pem
└── chain.pem
```

---

## 참고

- [Let's Encrypt 공식 문서](https://letsencrypt.org/docs/)
- [Certbot 사용 가이드](https://certbot.eff.org/)
- [Nginx SSL 설정 가이드](https://nginx.org/en/docs/http/configuring_https_servers.html)
