#!/bin/bash
# AI 추론 서버 배포 스크립트
# 사용법: ./deploy.sh [cpu|gpu]

set -e

MODE=${1:-gpu}
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

# 모델 파일 확인 (GPU 모드는 3개 모델 필요)
if [ "$MODE" = "gpu" ]; then
    echo ""
    echo "[CHECK] 모델 파일 확인..."
    
    # 8B 모델
    if [ ! -f models/qwen3-8b-summarizer-q4km.gguf ]; then
        echo "[ERROR] 8B 모델 파일이 없습니다: models/qwen3-8b-summarizer-q4km.gguf"
        exit 1
    fi
    echo "  ✅ 8B 모델: $(ls -lh models/qwen3-8b-summarizer-q4km.gguf | awk '{print $5}')"
    
    # 14B 모델
    if [ ! -f models/qwen3-14b.Q4_K_M.gguf ]; then
        echo "[ERROR] 14B 모델 파일이 없습니다: models/qwen3-14b.Q4_K_M.gguf"
        exit 1
    fi
    echo "  ✅ 14B 모델: $(ls -lh models/qwen3-14b.Q4_K_M.gguf | awk '{print $5}')"
    
    echo "  ℹ️  Whisper large-v3: 첫 실행 시 자동 다운로드됨 (~3GB)"
    echo ""
else
    # CPU 모드는 8B만
    if [ ! -f models/qwen3-8b-summarizer-q4km.gguf ]; then
        echo "[ERROR] 모델 파일이 없습니다: models/qwen3-8b-summarizer-q4km.gguf"
        exit 1
    fi
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

# 헬스체크 대기 (모델 로드로 인해 시간 더 필요)
echo ""
echo "서버 시작 대기 중... (모델 3개 로드, 최대 3분)"
sleep 30

for i in {1..30}; do
    HEALTH=$(curl -s http://localhost:8000/health 2>/dev/null || echo "")
    if echo "$HEALTH" | grep -q '"status":"ok"'; then
        echo ""
        echo "=========================================="
        echo "배포 완료!"
        echo "=========================================="
        echo ""
        echo "$HEALTH" | python3 -m json.tool 2>/dev/null || echo "$HEALTH"
        echo ""
        echo "API 문서: http://localhost:8000/docs"
        echo "=========================================="
        exit 0
    fi
    echo "대기 중... ($i/30)"
    sleep 5
done

echo ""
echo "[ERROR] 서버 시작 실패"
echo "로그 확인: docker-compose -f docker-compose.gpu.yml logs -f"
exit 1
