#!/bin/bash
set -e

# ===========================================
# 단일 환경 배포 스크립트
# ===========================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_PATH="${DEPLOY_PATH:-/home/ubuntu/squiz}"
LOG_DIR="$DEPLOY_PATH/logs"
LOG_FILE="$LOG_DIR/deploy-$(date +%Y%m%d-%H%M%S).log"

mkdir -p "$LOG_DIR"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${GREEN}[INFO]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

warn() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${YELLOW}[WARN]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

error() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${RED}[ERROR]${NC} $timestamp - $1" | tee -a "$LOG_FILE"
}

# 컨테이너 IP 가져오기
get_container_ip() {
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$1" 2>/dev/null
}

# 헬스체크
health_check() {
    local service=$1
    local max_attempts=30
    local attempt=0

    log "헬스체크 시작: ${service}"

    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))

        case $service in
            backend)
                local ip=$(get_container_ip "squiz-backend")
                if [ -n "$ip" ] && nc -z "$ip" 8080 2>/dev/null; then
                    log "  backend 헬스체크 통과"
                    return 0
                fi
                ;;
            sfu)
                local ip=$(get_container_ip "squiz-sfu")
                if [ -n "$ip" ] && nc -z "$ip" 4000 2>/dev/null; then
                    log "  sfu 헬스체크 통과"
                    return 0
                fi
                ;;
            cs-quiz-ai)
                local ip=$(get_container_ip "squiz-cs-quiz-ai")
                if [ -n "$ip" ] && nc -z "$ip" 5000 2>/dev/null; then
                    log "  cs-quiz-ai 헬스체크 통과"
                    return 0
                fi
                ;;
            frontend)
                local ip=$(get_container_ip "squiz-frontend")
                if [ -n "$ip" ] && curl -sf "http://${ip}:80" > /dev/null 2>&1; then
                    log "  frontend 헬스체크 통과"
                    return 0
                fi
                ;;
        esac

        log "  대기 중... ($attempt/$max_attempts)"
        sleep 5
    done

    error "  ${service} 헬스체크 실패"
    return 1
}

# 모든 서비스 헬스체크
health_check_all() {
    local failed=0
    log "모든 서비스 헬스체크 시작"

    # SFU는 AI 추론 서버(3.38.92.48)에서 실행되므로 제외
    for service in frontend backend cs-quiz-ai; do
        if ! health_check "$service"; then
            failed=1
        fi
    done

    return $failed
}

# 이미지 Pull
pull_images() {
    log "Docker Hub에서 최신 이미지 Pull..."
    docker pull tkdgns11/squiz-backend:latest || true
    docker pull tkdgns11/squiz-frontend:latest || true
    # SFU는 AI 추론 서버에서 실행되므로 제외
    docker pull tkdgns11/squiz-cs-quiz-ai:latest || true
    log "이미지 Pull 완료"
}

# 공유 서비스 시작 확인
ensure_shared_services() {
    log "공유 서비스 (MySQL, Redis) 확인..."
    cd "$DEPLOY_PATH"

    docker network create squiz-network 2>/dev/null || true
    docker volume create squiz-uploads 2>/dev/null || true

    docker compose -p squiz-shared -f docker-compose.yml up -d

    log "MySQL 헬스체크 대기..."
    local attempt=0
    while [ $attempt -lt 30 ]; do
        attempt=$((attempt + 1))
        if docker exec squiz-mysql mysqladmin ping -h localhost > /dev/null 2>&1; then
            log "MySQL 준비 완료"
            break
        fi
        sleep 2
    done

    log "공유 서비스 준비 완료"
}

# Nginx Proxy 시작
ensure_proxy() {
    log "Nginx Proxy 시작..."
    cd "$DEPLOY_PATH"
    docker compose -p squiz-proxy -f docker-compose.proxy.yml up -d
    docker exec squiz-nginx-proxy nginx -s reload 2>/dev/null || true
    log "Nginx Proxy 준비 완료"
}

# 배포
deploy() {
    log "======================================"
    log "배포 시작"
    log "======================================"

    # 1. 이미지 Pull
    pull_images

    # 2. 공유 서비스 확인
    ensure_shared_services

    # 3. 기존 컨테이너 정리 및 새 버전 배포
    cd "$DEPLOY_PATH"
    log "기존 앱 컨테이너 정리..."
    docker compose -p squiz-app -f docker-compose.app.yml down --remove-orphans 2>/dev/null || true
    docker rm -f squiz-backend squiz-frontend squiz-cs-quiz-ai 2>/dev/null || true

    # Blue/Green 잔여 컨테이너도 정리 (SFU는 AI 서버에서 실행)
    docker rm -f squiz-backend-blue squiz-backend-green squiz-nginx-blue squiz-nginx-green squiz-cs-quiz-ai-blue squiz-cs-quiz-ai-green 2>/dev/null || true

    log "새 버전 배포..."
    docker compose -p squiz-app -f docker-compose.app.yml up -d

    # 4. 헬스체크
    log "헬스체크 진행 중..."
    if ! health_check_all; then
        error "헬스체크 실패! 배포 중단"
        docker compose -p squiz-app -f docker-compose.app.yml down
        # 실패 후에도 Nginx 리로드하여 DNS 캐시 정리 (502 방지)
        warn "Nginx 리로드 (DNS 캐시 정리)..."
        docker exec squiz-nginx-proxy nginx -s reload 2>/dev/null || true
        exit 1
    fi

    # 5. Nginx Proxy 시작/리로드
    ensure_proxy

    log "======================================"
    log "배포 완료!"
    log "======================================"
}

# 상태 확인
status() {
    echo ""
    echo "======================================"
    echo "배포 상태"
    echo "======================================"
    echo ""
    echo "앱 서비스:"
    docker ps --filter "name=squiz-frontend" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    docker ps --filter "name=squiz-backend" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    docker ps --filter "name=squiz-cs-quiz-ai" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo "  (SFU: AI 서버 3.38.92.48에서 실행)"
    echo ""
    echo "Proxy:"
    docker ps --filter "name=squiz-nginx-proxy" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo ""
    echo "공유 서비스:"
    docker ps --filter "name=squiz-mysql" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    docker ps --filter "name=squiz-redis" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo "======================================"
}

# 사용법
usage() {
    echo "Usage: $0 {deploy|status}"
    echo ""
    echo "Commands:"
    echo "  deploy  - 최신 이미지로 배포"
    echo "  status  - 현재 배포 상태 확인"
    exit 1
}

# 메인
case "${1:-}" in
    deploy)
        deploy
        ;;
    status)
        status
        ;;
    *)
        usage
        ;;
esac
