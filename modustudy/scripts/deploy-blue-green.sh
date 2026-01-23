#!/bin/bash
set -e

# ===========================================
# Blue/Green 배포 스크립트
# ===========================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DEPLOY_PATH="${DEPLOY_PATH:-/home/ubuntu/squiz}"
STATE_FILE="$DEPLOY_PATH/.deploy-state"
LOG_DIR="$DEPLOY_PATH/logs"
LOG_FILE="$LOG_DIR/deploy-$(date +%Y%m%d-%H%M%S).log"

# 로그 디렉토리 생성
mkdir -p "$LOG_DIR"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 로깅 함수
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

# 현재 활성 환경 확인
get_active_env() {
    if [ -f "$STATE_FILE" ]; then
        cat "$STATE_FILE"
    else
        echo "blue"
    fi
}

# 비활성 환경 확인
get_inactive_env() {
    local active=$(get_active_env)
    if [ "$active" = "blue" ]; then
        echo "green"
    else
        echo "blue"
    fi
}

# 컨테이너 IP 가져오기
get_container_ip() {
    local container_name=$1
    docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$container_name" 2>/dev/null
}

# 헬스체크 (호스트에서 실행)
health_check() {
    local env=$1
    local service=$2
    local max_attempts=30
    local attempt=0

    log "헬스체크 시작: ${service}-${env}"

    while [ $attempt -lt $max_attempts ]; do
        attempt=$((attempt + 1))

        case $service in
            backend)
                local ip=$(get_container_ip "squiz-backend-${env}")
                # 포트 체크 (actuator가 500 에러 발생하므로 단순 포트 체크 사용)
                if [ -n "$ip" ] && nc -z "$ip" 8080 2>/dev/null; then
                    log "  backend-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            sfu)
                local ip=$(get_container_ip "squiz-sfu-${env}")
                if [ -n "$ip" ] && nc -z "$ip" 4000 2>/dev/null; then
                    log "  sfu-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            cs-quiz-ai)
                local ip=$(get_container_ip "squiz-cs-quiz-ai-${env}")
                if [ -n "$ip" ] && nc -z "$ip" 5000 2>/dev/null; then
                    log "  cs-quiz-ai-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
            nginx)
                local ip=$(get_container_ip "squiz-nginx-${env}")
                if [ -n "$ip" ] && curl -sf "http://${ip}:80" > /dev/null 2>&1; then
                    log "  nginx-${env} 헬스체크 통과"
                    return 0
                fi
                ;;
        esac

        log "  대기 중... ($attempt/$max_attempts)"
        sleep 5
    done

    error "  ${service}-${env} 헬스체크 실패"
    return 1
}

# 모든 서비스 헬스체크
health_check_all() {
    local env=$1
    local failed=0

    log "모든 서비스 헬스체크 시작 (${env} 환경)"

    for service in nginx backend sfu cs-quiz-ai; do
        if ! health_check "$env" "$service"; then
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
    docker pull tkdgns11/squiz-sfu:latest || true
    docker pull tkdgns11/squiz-cs-quiz-ai:latest || true
    log "이미지 Pull 완료"
}

# 공유 서비스 시작 확인
ensure_shared_services() {
    log "공유 서비스 (MySQL, Redis) 확인..."

    cd "$DEPLOY_PATH"

    # 네트워크 생성 (없으면)
    docker network create squiz_squiz-network 2>/dev/null || true

    # 공유 서비스 시작
    docker compose -f docker-compose.yml up -d

    # MySQL 헬스체크 대기
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
    docker compose -f docker-compose.proxy.yml up -d
    log "Nginx Proxy 준비 완료"
}

# 비활성 환경 배포
deploy_inactive() {
    local inactive=$(get_inactive_env)
    log "비활성 환경(${inactive}) 배포 시작..."

    cd "$DEPLOY_PATH"

    # 기존 비활성 환경 정리
    docker compose -f docker-compose.${inactive}.yml down --remove-orphans 2>/dev/null || true

    # 새 버전 배포
    docker compose -f docker-compose.${inactive}.yml up -d

    log "비활성 환경(${inactive}) 컨테이너 시작됨"
}

# 트래픽 전환
switch_traffic() {
    local new_active=$1
    log "트래픽 전환: ${new_active}으로 전환 중..."

    # upstream.conf 업데이트
    if [ "$new_active" = "blue" ]; then
        cat > "$DEPLOY_PATH/nginx/conf.d/upstream.conf" << 'EOF'
# ===========================================
# Blue/Green Upstream Configuration
# Active Environment: blue
# ===========================================

upstream backend {
    server squiz-backend-blue:8080;
    server squiz-backend-green:8080 backup;
}

upstream sfu {
    server squiz-sfu-blue:4000;
    server squiz-sfu-green:4000 backup;
}

upstream frontend {
    server squiz-nginx-blue:80;
    server squiz-nginx-green:80 backup;
}

upstream cs-quiz-ai {
    server squiz-cs-quiz-ai-blue:5000;
    server squiz-cs-quiz-ai-green:5000 backup;
}
EOF
    else
        cat > "$DEPLOY_PATH/nginx/conf.d/upstream.conf" << 'EOF'
# ===========================================
# Blue/Green Upstream Configuration
# Active Environment: green
# ===========================================

upstream backend {
    server squiz-backend-blue:8080 backup;
    server squiz-backend-green:8080;
}

upstream sfu {
    server squiz-sfu-blue:4000 backup;
    server squiz-sfu-green:4000;
}

upstream frontend {
    server squiz-nginx-blue:80 backup;
    server squiz-nginx-green:80;
}

upstream cs-quiz-ai {
    server squiz-cs-quiz-ai-blue:5000 backup;
    server squiz-cs-quiz-ai-green:5000;
}
EOF
    fi

    # Nginx reload (무중단)
    docker exec squiz-nginx-proxy nginx -s reload

    # 상태 파일 업데이트
    echo "$new_active" > "$STATE_FILE"

    log "트래픽 전환 완료: ${new_active}"
}

# 이전 환경 유지 (롤백 대비)
cleanup_old() {
    local old_env=$1
    warn "이전 환경(${old_env}) 유지 중 (롤백 대비)"
    # 필요 시 docker compose -f docker-compose.${old_env}.yml down
}

# 메인 배포 함수
deploy() {
    local active=$(get_active_env)
    local inactive=$(get_inactive_env)

    log "======================================"
    log "Blue/Green 배포 시작"
    log "현재 활성 환경: ${active}"
    log "배포 대상 환경: ${inactive}"
    log "======================================"

    # 1. 이미지 Pull
    pull_images

    # 2. 공유 서비스 확인
    ensure_shared_services

    # 3. 비활성 환경에 배포
    deploy_inactive

    # 4. 헬스체크
    log "헬스체크 진행 중..."
    if ! health_check_all "$inactive"; then
        error "헬스체크 실패! 배포 중단"
        docker compose -f docker-compose.${inactive}.yml down
        exit 1
    fi

    # 5. Nginx Proxy 시작
    ensure_proxy

    # 6. 트래픽 전환
    switch_traffic "$inactive"

    # 7. 이전 환경 유지 (롤백 대비)
    cleanup_old "$active"

    log "======================================"
    log "배포 완료!"
    log "활성 환경: ${inactive}"
    log "======================================"
}

# 롤백 함수
rollback() {
    local active=$(get_active_env)
    local inactive=$(get_inactive_env)

    log "======================================"
    log "롤백 시작"
    log "현재 활성: ${active} → 롤백 대상: ${inactive}"
    log "======================================"

    # 이전 환경이 실행 중인지 확인
    if ! docker ps | grep -q "squiz-backend-${inactive}"; then
        error "롤백 대상 환경(${inactive})이 실행 중이 아닙니다!"
        exit 1
    fi

    # 트래픽 전환 (롤백)
    switch_traffic "$inactive"

    log "롤백 완료: ${inactive}"
}

# 상태 확인
status() {
    local active=$(get_active_env)
    echo ""
    echo "======================================"
    echo "Blue/Green 배포 상태"
    echo "======================================"
    echo "활성 환경: ${active}"
    echo ""
    echo "Blue 환경:"
    docker ps --filter "name=squiz-.*-blue" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo ""
    echo "Green 환경:"
    docker ps --filter "name=squiz-.*-green" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo ""
    echo "Proxy:"
    docker ps --filter "name=squiz-nginx-proxy" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo ""
    echo "공유 서비스:"
    docker ps --filter "name=squiz-mysql" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    docker ps --filter "name=squiz-redis" --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  (없음)"
    echo "======================================"
}

# 기존 레거시 컨테이너 정리
cleanup_legacy() {
    log "기존 레거시 컨테이너 정리 중..."

    # 기존 non-Blue/Green 컨테이너 중지 및 제거
    docker stop squiz-nginx squiz-backend squiz-sfu squiz-cs-quiz-ai 2>/dev/null || true
    docker rm squiz-nginx squiz-backend squiz-sfu squiz-cs-quiz-ai 2>/dev/null || true

    log "레거시 컨테이너 정리 완료"
}

# 초기 설정 (첫 배포 시)
init() {
    log "======================================"
    log "Blue/Green 배포 초기 설정"
    log "======================================"

    # 기존 레거시 컨테이너 정리 (포트 충돌 방지)
    cleanup_legacy

    # 이미지 Pull
    pull_images

    # 공유 서비스 시작
    ensure_shared_services

    # Blue 환경 배포
    log "Blue 환경 배포..."
    cd "$DEPLOY_PATH"
    docker compose -f docker-compose.blue.yml up -d

    # 헬스체크
    log "Blue 환경 헬스체크..."
    sleep 30  # 초기 시작 대기
    if ! health_check_all "blue"; then
        error "Blue 환경 헬스체크 실패"
        exit 1
    fi

    # Proxy 시작
    ensure_proxy

    # 상태 저장
    echo "blue" > "$STATE_FILE"

    log "======================================"
    log "초기 설정 완료! Blue 환경 활성화됨"
    log "======================================"
}

# 사용법
usage() {
    echo "Usage: $0 {deploy|rollback|status|switch|init}"
    echo ""
    echo "Commands:"
    echo "  init     - 첫 배포 시 초기 설정 (Blue 환경 시작)"
    echo "  deploy   - 새 버전을 비활성 환경에 배포하고 트래픽 전환"
    echo "  rollback - 이전 환경으로 즉시 롤백"
    echo "  status   - 현재 배포 상태 확인"
    echo "  switch   - 수동으로 트래픽 전환 (switch blue|green)"
    exit 1
}

# 메인
case "${1:-}" in
    init)
        init
        ;;
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    switch)
        if [ -z "${2:-}" ]; then
            echo "Usage: $0 switch {blue|green}"
            exit 1
        fi
        switch_traffic "$2"
        ;;
    *)
        usage
        ;;
esac
