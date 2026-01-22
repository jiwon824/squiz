#!/bin/bash
#
# Squiz MySQL 데이터베이스 복원 스크립트
# 사용법: ./restore-db.sh <백업파일.sql.gz>
#
# Flyway 버전 호환성 체크 기능 포함
#

set -e

# ===========================================
# 설정
# ===========================================
CONTAINER_NAME="squiz-mysql"
DATABASE_NAME="squiz"
BACKUP_FILE="$1"

# 환경변수 로드
if [ -f /home/ubuntu/squiz/.env ]; then
    source /home/ubuntu/squiz/.env
fi

# ===========================================
# 함수 정의
# ===========================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# Flyway 현재 버전 조회
get_current_flyway_version() {
    local version=$(docker exec "$CONTAINER_NAME" mysql \
        -u"${DB_USERNAME}" \
        -p"${DB_PASSWORD}" \
        -N -s \
        -e "SELECT COALESCE(MAX(version), '0') FROM ${DATABASE_NAME}.flyway_schema_history WHERE success = 1;" \
        2>/dev/null || echo "0")

    if [ -z "$version" ] || [ "$version" = "NULL" ]; then
        echo "0"
    else
        echo "$version"
    fi
}

# 백업 파일에서 Flyway 버전 추출
get_backup_flyway_version() {
    local backup_file="$1"
    local meta_file="${backup_file%.sql.gz}.meta"

    # 메타 파일에서 버전 조회
    if [ -f "$meta_file" ]; then
        grep "FLYWAY_VERSION" "$meta_file" | cut -d'=' -f2
        return
    fi

    # 파일명에서 버전 추출 (squiz_daily_V3_20250120_030000.sql.gz)
    local version=$(echo "$backup_file" | grep -oP '_V\K[0-9]+' || echo "unknown")
    echo "$version"
}

# 버전 호환성 체크
check_version_compatibility() {
    local backup_version="$1"
    local current_version="$2"

    if [ "$backup_version" = "unknown" ]; then
        log "⚠️  경고: 백업 파일의 Flyway 버전을 확인할 수 없습니다"
        log "         (구버전 백업 파일일 수 있음)"
        return 1
    fi

    if [ "$backup_version" != "$current_version" ]; then
        log "⚠️  버전 불일치 감지!"
        log "    백업 버전: V${backup_version}"
        log "    현재 DB 버전: V${current_version}"

        if [ "$backup_version" -lt "$current_version" ]; then
            log ""
            log "⚠️  백업이 현재 DB보다 구버전입니다!"
            log "    복원 후 현재 앱 코드와 호환되지 않을 수 있습니다."
            log ""
            log "권장 조치:"
            log "  1) 앱도 해당 버전으로 롤백"
            log "  2) 또는 복원 후 마이그레이션 재실행"
        else
            log ""
            log "⚠️  백업이 현재 DB보다 신버전입니다!"
            log "    앱 버전도 함께 업그레이드가 필요합니다."
        fi

        return 1
    fi

    log "✅ 버전 일치: V${current_version}"
    return 0
}

# ===========================================
# 검증
# ===========================================

if [ -z "$BACKUP_FILE" ]; then
    echo "사용법: $0 <백업파일.sql.gz>"
    echo ""
    echo "사용 가능한 백업 파일 (버전 포함):"
    echo "=========================================="

    for type in daily weekly manual; do
        if [ -d "/home/ubuntu/squiz/backups/$type" ]; then
            echo "[$type]"
            find "/home/ubuntu/squiz/backups/$type" -name "*.sql.gz" -printf "  %f (%s bytes, %Tc)\n" 2>/dev/null | head -10
        fi
    done

    echo "=========================================="
    echo ""
    echo "현재 DB Flyway 버전: V$(get_current_flyway_version)"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    error_exit "백업 파일을 찾을 수 없습니다: $BACKUP_FILE"
fi

# Docker 컨테이너 실행 확인
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    error_exit "MySQL 컨테이너가 실행 중이 아닙니다: $CONTAINER_NAME"
fi

# ===========================================
# 버전 체크
# ===========================================

log "====== Squiz DB 복원 시작 ======"
log "백업 파일: $BACKUP_FILE"

BACKUP_VERSION=$(get_backup_flyway_version "$BACKUP_FILE")
CURRENT_VERSION=$(get_current_flyway_version)

log "백업 Flyway 버전: V${BACKUP_VERSION}"
log "현재 DB Flyway 버전: V${CURRENT_VERSION}"

# 버전 호환성 체크
if ! check_version_compatibility "$BACKUP_VERSION" "$CURRENT_VERSION"; then
    echo ""
    echo "⚠️  경고: 버전이 일치하지 않습니다!"
    echo "복원을 진행하면 데이터 손실이나 앱 오류가 발생할 수 있습니다."
    echo ""
    read -p "그래도 복원을 진행하시겠습니까? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        log "복원 취소됨"
        exit 0
    fi

    log "사용자가 버전 불일치에도 복원 진행을 선택함"
fi

# ===========================================
# 최종 확인
# ===========================================

echo ""
echo "⚠️  경고: 현재 데이터베이스가 덮어쓰기됩니다!"
echo "복원할 파일: $BACKUP_FILE"
echo "복원할 버전: V${BACKUP_VERSION}"
echo ""
read -p "복원을 진행하시겠습니까? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    log "복원 취소됨"
    exit 0
fi

# ===========================================
# 복원 실행
# ===========================================

log "복원 중..."

# 압축 해제 후 MySQL로 복원
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" mysql \
    -u"${DB_USERNAME}" \
    -p"${DB_PASSWORD}" \
    2>/dev/null

if [ $? -eq 0 ]; then
    # 복원 후 버전 재확인
    NEW_VERSION=$(get_current_flyway_version)
    log "====== Squiz DB 복원 완료 ======"
    log "복원된 DB Flyway 버전: V${NEW_VERSION}"

    if [ "$NEW_VERSION" != "$BACKUP_VERSION" ]; then
        log "⚠️  주의: 복원된 버전(V${NEW_VERSION})이 예상 버전(V${BACKUP_VERSION})과 다릅니다."
    fi
else
    error_exit "복원 실패"
fi
