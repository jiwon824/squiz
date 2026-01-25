# Docker Compose v2 가이드

## 개요

Docker Compose v2는 기존 v1을 Go 언어로 재작성한 버전입니다. Docker CLI 플러그인으로 통합되어 더 빠르고 안정적입니다.

---

## v1 vs v2 비교

| 항목 | v1 (레거시) | v2 (신규) |
|------|-----------|----------|
| **언어** | Python | Go |
| **명령어** | `docker-compose` | `docker compose` |
| **설치** | 별도 pip 패키지 | Docker 플러그인 (내장) |
| **속도** | 느림 | 빠름 (2~3배) |
| **메모리** | 많이 사용 | 적게 사용 |
| **유지보수** | 중단됨 (2023) | 활발히 개발 중 |

### 버전 체계

```
v1 계열: 1.x (예: 1.29.2)
v2 계열: 2.x → 3.x → 4.x → 5.x (계속 업데이트)
```

**"v2"는 아키텍처 버전**이며, 버전 번호는 2.x부터 5.x까지 계속 증가합니다.

---

## 명령어 차이

```bash
# v1 (레거시)
docker-compose up -d
docker-compose down
docker-compose ps

# v2 (신규) - 하이픈 대신 공백
docker compose up -d
docker compose down
docker compose ps
```

---

## v2에서 개선된 점

### 1. 성능 향상
- Go 언어로 재작성되어 2~3배 빠름
- 메모리 사용량 감소
- 병렬 처리 개선

### 2. 새로운 기능

```bash
# 헬스체크 완료까지 대기
docker compose up -d --wait

# 실행 없이 미리보기
docker compose up --dry-run

# 특정 서비스만 빌드
docker compose build --no-cache nginx
```

### 3. 버그 수정
- `ContainerConfig` KeyError 해결
- 볼륨 마운트 안정성 개선
- 네트워크 처리 개선

### 4. BuildKit 기본 지원
- 더 빠른 이미지 빌드
- 캐시 효율성 향상

---

## 설치 방법

### Ubuntu/Debian

```bash
# Docker 플러그인 디렉토리 생성
sudo mkdir -p /usr/local/lib/docker/cli-plugins

# Docker Compose v2 다운로드
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 버전 확인
docker compose version
```

### 확인

```bash
# v2가 설치되었는지 확인
docker compose version
# 출력: Docker Compose version v5.0.1
```

---

## 마이그레이션

### 기존 스크립트 수정

```bash
# Before (v1)
docker-compose up -d --build
docker-compose down
docker-compose logs -f

# After (v2)
docker compose up -d --build
docker compose down
docker compose logs -f
```

### CI/CD 파이프라인 수정

```yaml
# .gitlab-ci.yml
deploy:
  script:
    # Before
    # - docker-compose up -d --build

    # After
    - docker compose up -d --build --force-recreate --remove-orphans
```

---

## 무중단 배포

v2에서는 `--force-recreate` 옵션이 안정적으로 동작합니다:

```bash
# 기존 컨테이너를 중지하지 않고 새 컨테이너로 교체
docker compose up -d --build --force-recreate --remove-orphans
```

### 동작 방식

```
1. 새 이미지 빌드
2. 새 컨테이너 생성 및 시작
3. 기존 컨테이너 중지 및 제거
```

기존 컨테이너가 실행 중인 상태에서 새 컨테이너가 준비되므로 다운타임이 최소화됩니다.

---

## v1의 ContainerConfig 버그

### 증상

```
KeyError: 'ContainerConfig'
```

### 원인

docker-compose v1.29.2와 최신 Docker Engine 간의 호환성 문제

### 해결

Docker Compose v2로 업그레이드

---

## 호환성

- `docker-compose.yml` 파일 형식은 동일하게 사용 가능
- 대부분의 명령어 옵션 호환
- 일부 deprecated 옵션은 경고 표시

---

## 참고

- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Docker Compose v2 릴리즈](https://github.com/docker/compose/releases)
- [v1 → v2 마이그레이션 가이드](https://docs.docker.com/compose/migrate/)
