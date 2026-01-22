#!/bin/bash
#
# Squiz MySQL 데이터베이스 백업 스크립트
# 사용법: ./backup-db.sh [daily|weekly|manual]
#
# Flyway 버전을 파일명에 포함하여 복원 시 호환성 확인 가능
#

set -e

# ===========================================
# 설정
# ===========================================
BACKUP_DIR="/home/ubuntu/squiz/backups"
CONTAINER_NAME="squiz-mysql"
DATABASE_NAME="squiz"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_TYPE="${1:-manual}"

# 보관 기간 설정
DAILY_KEEP_DAYS=7      # 일간 백업 7일 보관
WEEKLY_KEEP_DAYS=30    # 주간 백업 30일 보관
MANUAL_KEEP_DAYS=90    # 수동 백업 90일 보관

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
get_flyway_version() {
    local version=$(docker exec "$CONTAINER_NAME" mysql \
        -u"${DB_USERNAME}" \
        -p"${DB_PASSWORD}" \
        -N -s \
        -e "SELECT COALESCE(MAX(version), '0') FROM ${DATABASE_NAME}.flyway_schema_history WHERE success = 1;" \
        2>/dev/null || echo "0")

    # 버전이 비어있으면 0 반환
    if [ -z "$version" ] || [ "$version" = "NULL" ]; then
        echo "0"
    else
        echo "$version"
    fi
}

# 백업 디렉토리 생성
create_backup_dir() {
    mkdir -p "$BACKUP_DIR/$BACKUP_TYPE"
    log "백업 디렉토리 확인: $BACKUP_DIR/$BACKUP_TYPE"
}

# MySQL 백업 실행
backup_mysql() {
    # Flyway 버전 조회
    local flyway_version=$(get_flyway_version)
    log "현재 Flyway 버전: V${flyway_version}"

    # 파일명에 버전 포함: squiz_daily_V3_20250120_030000.sql.gz
    local backup_file="$BACKUP_DIR/$BACKUP_TYPE/${DATABASE_NAME}_${BACKUP_TYPE}_V${flyway_version}_${DATE}.sql.gz"

    log "MySQL 백업 시작: $DATABASE_NAME"

    # Docker 컨테이너 실행 확인
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        error_exit "MySQL 컨테이너가 실행 중이 아닙니다: $CONTAINER_NAME"
    fi

    # mysqldump 실행 및 압축
    docker exec "$CONTAINER_NAME" mysqldump \
        -u"${DB_USERNAME}" \
        -p"${DB_PASSWORD}" \
        --single-transaction \
        --routines \
        --triggers \
        --databases "$DATABASE_NAME" \
        2>/dev/null | gzip > "$backup_file"

    if [ $? -eq 0 ] && [ -s "$backup_file" ]; then
        local size=$(du -h "$backup_file" | cut -f1)
        log "백업 완료: $backup_file ($size)"

        # 버전 메타데이터 파일 생성
        echo "FLYWAY_VERSION=${flyway_version}" > "${backup_file%.sql.gz}.meta"
        echo "BACKUP_DATE=${DATE}" >> "${backup_file%.sql.gz}.meta"
        echo "DATABASE=${DATABASE_NAME}" >> "${backup_file%.sql.gz}.meta"
    else
        rm -f "$backup_file"
        error_exit "백업 실패"
    fi
}

# 오래된 백업 삭제
cleanup_old_backups() {
    local keep_days

    case "$BACKUP_TYPE" in
        daily)  keep_days=$DAILY_KEEP_DAYS ;;
        weekly) keep_days=$WEEKLY_KEEP_DAYS ;;
        manual) keep_days=$MANUAL_KEEP_DAYS ;;
        *)      keep_days=$MANUAL_KEEP_DAYS ;;
    esac

    log "오래된 백업 정리 (${keep_days}일 이상)"

    local deleted_count=$(find "$BACKUP_DIR/$BACKUP_TYPE" -name "*.sql.gz" -mtime +$keep_days -delete -print | wc -l)
    # 메타 파일도 함께 삭제
    find "$BACKUP_DIR/$BACKUP_TYPE" -name "*.meta" -mtime +$keep_days -delete 2>/dev/null || true

    if [ "$deleted_count" -gt 0 ]; then
        log "삭제된 백업 파일: ${deleted_count}개"
    fi
}

# 백업 목록 출력
list_backups() {
    log "현재 백업 목록:"
    echo "=========================================="

    for type in daily weekly manual; do
        if [ -d "$BACKUP_DIR/$type" ]; then
            local count=$(find "$BACKUP_DIR/$type" -name "*.sql.gz" 2>/dev/null | wc -l)
            local size=$(du -sh "$BACKUP_DIR/$type" 2>/dev/null | cut -f1)

            # 최신 버전 표시
            local latest=$(ls -t "$BACKUP_DIR/$type"/*.sql.gz 2>/dev/null | head -1)
            local latest_version=""
            if [ -n "$latest" ]; then
                latest_version=$(echo "$latest" | grep -oP '_V\K[0-9]+' || echo "?")
            fi

            echo "[$type] ${count}개 파일, 총 ${size:-0}, 최신 버전: V${latest_version:-?}"
        fi
    done

    echo "=========================================="

    # 현재 DB의 Flyway 버전도 표시
    local current_version=$(get_flyway_version)
    echo "현재 DB Flyway 버전: V${current_version}"
}

# ===========================================
# 메인 실행
# ===========================================

log "====== Squiz DB 백업 시작 ======"
log "백업 타입: $BACKUP_TYPE"

create_backup_dir
backup_mysql
cleanup_old_backups
list_backups

log "====== Squiz DB 백업 완료 ======"
