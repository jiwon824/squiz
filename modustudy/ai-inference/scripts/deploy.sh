#!/bin/bash
# AI 추론 서버 배포 스크립트
# 사용법: ./deploy.sh [cpu|gpu]

set -e

MODE=${1:-cpu}
SCRIPT_DIR=$(dirname "$0")
PROJECT_DIR=$(dirname "$SCRIPT_DIR")

cd "$PROJECT_DIR"

echo "=========================================="
echo "ModuStudy AI 추론 서버 배포"
echo "모드: $MODE"
echo "=========================================="

# 환경 파일 확인
if [ ! -f .env ]; then
    echo "[WARNING] .env 파일이 없습니다. .env.example을 복사합니다."
    cp .env.example .env
fi

# 모델 파일 확인
if [ ! -f models/qwen3-8b-summarizer-q4km.gguf ]; then
    echo "[ERROR] 모델 파일이 없습니다: models/qwen3-8b-summarizer-q4km.gguf"
    echo "SSAFY 서버에서 모델을 복사해주세요:"
    echo "  scp j-i14d106@ssafy-gpu:~/models/qwen3-8b-summarizer-q4km.gguf ./models/"
    exit 1
fi

# 기존 컨테이너 정리
echo "[1/3] 기존 컨테이너 정리..."
if [ "$MODE" = "gpu" ]; then
    docker-compose -f docker-compose.gpu.yml down 2>/dev/null || true
else
    docker-compose down 2>/dev/null || true
fi

# 이미지 빌드
echo "[2/3] 이미지 빌드..."
if [ "$MODE" = "gpu" ]; then
    docker-compose -f docker-compose.gpu.yml build
else
    docker-compose build
fi

# 서비스 시작
echo "[3/3] 서비스 시작..."
if [ "$MODE" = "gpu" ]; then
    docker-compose -f docker-compose.gpu.yml up -d
else
    docker-compose up -d
fi

# 헬스체크 대기
echo ""
echo "서버 시작 대기 중..."
sleep 10

for i in {1..12}; do
    if curl -s http://localhost:8000/health | grep -q '"status":"ok"'; then
        echo ""
        echo "=========================================="
        echo "배포 완료!"
        echo "=========================================="
        echo ""
        curl -s http://localhost:8000/health | python3 -m json.tool
        echo ""
        echo "API 문서: http://localhost:8000/docs"
        echo "=========================================="
        exit 0
    fi
    echo "대기 중... ($i/12)"
    sleep 5
done

echo ""
echo "[ERROR] 서버 시작 실패"
echo "로그 확인: docker-compose logs -f"
exit 1
