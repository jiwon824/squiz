# MySQL 데이터베이스 백업/복원 가이드

## 개요

Squiz 프로젝트의 MySQL 데이터베이스 백업 및 복원 방법을 설명합니다.

**Flyway 버전 관리**: 백업 파일에 Flyway 스키마 버전이 포함되어 복원 시 호환성을 확인할 수 있습니다.

---

## 백업 스크립트

### 위치

```
/home/ubuntu/squiz/scripts/
├── backup-db.sh    # 백업 스크립트
└── restore-db.sh   # 복원 스크립트
```

### 백업 디렉토리 구조

```
/home/ubuntu/squiz/backups/
├── daily/          # 일간 백업 (7일 보관)
├── weekly/         # 주간 백업 (30일 보관)
└── manual/         # 수동 백업 (90일 보관)
```

---

## 사용법

### 1. 스크립트 배포 (최초 1회)

EC2 서버에서:

```bash
cd /home/ubuntu/squiz

# scripts 디렉토리 생성
mkdir -p scripts backups

# 스크립트 복사 (Git에서 pull 후)
cp modustudy/scripts/backup-db.sh scripts/
cp modustudy/scripts/restore-db.sh scripts/

# 실행 권한 부여
chmod +x scripts/*.sh
```

### 2. 수동 백업

```bash
cd /home/ubuntu/squiz

# 기본 (manual 타입)
./scripts/backup-db.sh

# 타입 지정
./scripts/backup-db.sh daily
./scripts/backup-db.sh weekly
./scripts/backup-db.sh manual
```

### 3. 복원

```bash
cd /home/ubuntu/squiz

# 백업 파일 목록 확인 (버전 포함)
./scripts/restore-db.sh

# 복원 실행
./scripts/restore-db.sh /home/ubuntu/squiz/backups/daily/squiz_daily_V1_20250120_030000.sql.gz
```

**버전 호환성 체크**: 복원 시 백업 버전과 현재 DB 버전을 비교하여 경고를 표시합니다.

```
백업 Flyway 버전: V1
현재 DB Flyway 버전: V3

⚠️  버전 불일치 감지!
    백업이 현재 DB보다 구버전입니다!
    복원 후 현재 앱 코드와 호환되지 않을 수 있습니다.
```

---

## 자동 백업 설정 (Cron)

### 크론잡 등록

```bash
sudo crontab -e
```

다음 내용 추가:

```cron
# 매일 새벽 3시 - 일간 백업
0 3 * * * /home/ubuntu/squiz/scripts/backup-db.sh daily >> /var/log/squiz-backup.log 2>&1

# 매주 일요일 새벽 4시 - 주간 백업
0 4 * * 0 /home/ubuntu/squiz/scripts/backup-db.sh weekly >> /var/log/squiz-backup.log 2>&1
```

### 로그 확인

```bash
tail -f /var/log/squiz-backup.log
```

---

## 백업 보관 정책

| 백업 타입 | 실행 주기 | 보관 기간 |
|-----------|----------|-----------|
| daily | 매일 새벽 3시 | 7일 |
| weekly | 매주 일요일 새벽 4시 | 30일 |
| manual | 수동 실행 | 90일 |

---

## 백업 파일 형식

```
squiz_<타입>_V<버전>_<날짜>_<시간>.sql.gz

예시:
squiz_daily_V1_20250120_030000.sql.gz      # Flyway V1 스키마
squiz_weekly_V3_20250119_040000.sql.gz     # Flyway V3 스키마
squiz_manual_V5_20250120_143052.sql.gz     # Flyway V5 스키마
```

- 형식: mysqldump SQL
- 압축: gzip
- 포함: 테이블 구조, 데이터, 트리거, 스토어드 프로시저
- **버전**: Flyway 마이그레이션 버전 (V1, V2, ...)

### 메타데이터 파일

각 백업 파일과 함께 `.meta` 파일이 생성됩니다:

```
squiz_daily_V1_20250120_030000.meta

내용:
FLYWAY_VERSION=1
BACKUP_DATE=20250120_030000
DATABASE=squiz
```

---

## 백업 확인

### 현재 백업 목록

```bash
./scripts/backup-db.sh

# 출력 예시:
# [daily] 7개 파일, 총 42M
# [weekly] 4개 파일, 총 24M
# [manual] 2개 파일, 총 12M
```

### 상세 목록

```bash
ls -lh /home/ubuntu/squiz/backups/daily/
```

### 백업 파일 내용 확인 (압축 해제 없이)

```bash
zcat /home/ubuntu/squiz/backups/daily/squiz_daily_20250120_030000.sql.gz | head -50
```

---

## 외부 백업 (권장)

### S3로 백업 전송

AWS CLI 설치 후:

```bash
# 일간 백업을 S3로 동기화
aws s3 sync /home/ubuntu/squiz/backups/ s3://your-bucket/squiz-backups/
```

크론잡 추가:

```cron
# 매일 새벽 3시 30분 - S3 동기화
30 3 * * * aws s3 sync /home/ubuntu/squiz/backups/ s3://your-bucket/squiz-backups/
```

### 다른 서버로 백업 전송

```bash
# rsync로 다른 서버에 복사
rsync -avz /home/ubuntu/squiz/backups/ backup-server:/backups/squiz/
```

---

## 복원 시나리오

### 시나리오 1: 최신 백업으로 복원

```bash
# 최신 일간 백업 찾기
LATEST=$(ls -t /home/ubuntu/squiz/backups/daily/*.sql.gz | head -1)

# 복원
./scripts/restore-db.sh "$LATEST"
```

### 시나리오 2: 특정 날짜 백업으로 복원

```bash
# 2025-01-15 백업 찾기
ls /home/ubuntu/squiz/backups/*/squiz_*_20250115_*.sql.gz

# 복원
./scripts/restore-db.sh /home/ubuntu/squiz/backups/daily/squiz_daily_20250115_030000.sql.gz
```

### 시나리오 3: 새 서버에 복원

```bash
# 1. 백업 파일 전송
scp backup.sql.gz ubuntu@new-server:/home/ubuntu/squiz/

# 2. 새 서버에서 복원
./scripts/restore-db.sh /home/ubuntu/squiz/backup.sql.gz
```

---

## 트러블슈팅

### 백업 실패: 컨테이너 없음

**증상**: `MySQL 컨테이너가 실행 중이 아닙니다`

**해결**:
```bash
docker-compose up -d mysql
./scripts/backup-db.sh
```

### 백업 실패: 권한 오류

**증상**: `Permission denied`

**해결**:
```bash
chmod +x /home/ubuntu/squiz/scripts/backup-db.sh
```

### 복원 실패: 용량 부족

**증상**: `No space left on device`

**해결**:
```bash
# 오래된 백업 정리
find /home/ubuntu/squiz/backups -name "*.sql.gz" -mtime +30 -delete

# Docker 정리
docker system prune -f
```

---

## 참고

- 백업 전 서비스 중단 불필요 (`--single-transaction` 옵션 사용)
- 대용량 DB의 경우 백업/복원에 시간 소요
- 중요 작업 전 수동 백업 권장
