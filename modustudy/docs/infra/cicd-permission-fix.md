# CI/CD 배포 문제 해결 기록

## 개요

GitLab CI/CD 파이프라인에서 발생한 여러 배포 문제와 해결 방법을 정리합니다.

---

## 문제 1: rsync 권한 에러

### 증상

```
rsync: [Receiver] ERROR: cannot stat destination "/home/ubuntu/squiz/": Permission denied (13)
```

### 원인

`/home/ubuntu/` 디렉토리 권한이 `750`(drwxr-x---)으로 설정되어 있어서 `gitlab-runner` 유저가 하위 디렉토리에 접근 불가.

```
/home/
├── ubuntu/          (drwxr-x--- : 750) ← others에 x 권한 없음
│   └── squiz/       (drwxrwxr-x : 775, owner: gitlab-runner)
```

### 해결

```bash
sudo chmod o+x /home/ubuntu
```

변경 후: `drwxr-x--x` (751)

---

## 문제 2: .env 파일 삭제됨

### 증상

```
The DB_USERNAME variable is not set. Defaulting to a blank string.
The DB_PASSWORD variable is not set. Defaulting to a blank string.
```

docker-compose 실행 시 DB 환경변수를 찾지 못함.

### 원인

`.gitlab-ci.yml`의 rsync 명령어에 `--delete` 옵션이 있어서 git에 없는 `.env` 파일이 삭제됨.

```yaml
# 기존 (문제)
- rsync -av --delete --exclude '.git' ... modustudy/ $DEPLOY_PATH/
```

### 해결

rsync에서 `.env`, `ssl`, `init.sql` 디렉토리를 exclude 추가:

```yaml
# 수정 후
- rsync -av --delete --exclude '.git' --exclude 'node_modules' --exclude '.gradle' --exclude 'build' --exclude '.env' --exclude 'ssl' --exclude 'init.sql' modustudy/ $DEPLOY_PATH/
```

### EC2 .env 파일 복구 (수동)

```bash
sudo bash -c 'cat > /home/ubuntu/squiz/.env << EOF
DB_ROOT_PASSWORD=<비밀번호>
DB_USERNAME=<유저명>
DB_PASSWORD=<비밀번호>
EOF'
sudo chown gitlab-runner:gitlab-runner /home/ubuntu/squiz/.env
```

---

## 문제 3: sfu-server Dockerfile 없음

### 증상

```
unable to prepare context: unable to evaluate symlinks in Dockerfile path:
lstat /home/ubuntu/squiz/sfu-server/Dockerfile: no such file or directory
Service 'sfu-server' failed to build
```

### 원인

`docker-compose.yml`에서 `./sfu-server/Dockerfile`을 참조하지만, 해당 위치에 Dockerfile이 없음.

- `sfu-server/` - Dockerfile 없음
- `rtc_mockup/sfu-server/` - Dockerfile 있음

### 해결

`modustudy/sfu-server/Dockerfile` 파일 생성:

```dockerfile
FROM node:22-alpine

RUN apk add --no-cache python3 py3-pip make g++ linux-headers

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY src ./src

EXPOSE 3000
EXPOSE 40000-40100/udp

CMD ["node", "src/index.js"]
```

### EC2 임시 복구 (수동)

```bash
sudo cp /home/ubuntu/squiz/rtc_mockup/sfu-server/Dockerfile /home/ubuntu/squiz/sfu-server/
sudo chown gitlab-runner:gitlab-runner /home/ubuntu/squiz/sfu-server/Dockerfile
```

---

## 수정된 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `.gitlab-ci.yml` | rsync exclude에 `.env`, `ssl`, `init.sql` 추가 |
| `modustudy/sfu-server/Dockerfile` | 신규 생성 |

---

## EC2 환경 정보

### 환경변수 (`/etc/environment`)

```bash
TEST_DB_URL="jdbc:mysql://localhost:3306/squiz_test?..."
TEST_DB_USERNAME="<유저명>"
TEST_DB_PASSWORD="<비밀번호>"
```

### Docker 컨테이너

| 컨테이너 | 이미지 | 포트 |
|----------|--------|------|
| squiz-mysql | mysql:8.0 | 3306 |
| squiz-redis | redis:alpine | 6379 |
| squiz-backend | squiz_backend | 8080 |
| squiz-nginx | squiz_nginx | 80, 443 |
| squiz-sfu | squiz_sfu-server | 3000, 40000-40100/udp |
| squiz-recorder | squiz_recorder | 3001 |

### 데이터베이스

| DB 이름 | 용도 |
|---------|------|
| squiz | 운영 DB |
| squiz_test | 테스트 DB |

---

## 예방 조치

1. **서버 초기 설정 시**
   - `/home/ubuntu`에 `o+x` 권한 부여
   - `.env` 파일 미리 생성

2. **CI/CD 설정 시**
   - rsync `--delete` 사용 시 서버 전용 파일 exclude 필수
   - docker-compose에서 참조하는 모든 파일이 git에 있는지 확인

3. **문서화**
   - EC2 환경변수 목록 문서화
   - 서버 전용 파일 목록 관리
