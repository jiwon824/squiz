#!/bin/bash
# 로컬에서 EC2로 모델 업로드
# 사용법: ./download-model.sh <EC2_HOST> <PEM_KEY>

set -e

EC2_HOST=${1:-"ubuntu@<EC2_IP>"}
PEM_KEY=${2:-""}
MODEL_DIR="./models"

SCRIPT_DIR=$(dirname "$0")
PROJECT_DIR=$(dirname "$SCRIPT_DIR")

cd "$PROJECT_DIR"

echo "=========================================="
echo "AI 모델 업로드 (로컬 → EC2)"
echo "=========================================="
echo "대상 서버: $EC2_HOST"
echo ""

# PEM 키 확인
if [ -z "$PEM_KEY" ]; then
    echo "[ERROR] PEM 키를 지정해주세요."
    echo "사용법: ./download-model.sh ubuntu@<EC2_IP> <path/to/key.pem>"
    exit 1
fi

if [ ! -f "$PEM_KEY" ]; then
    echo "[ERROR] PEM 키 파일이 없습니다: $PEM_KEY"
    exit 1
fi

# 모델 파일 확인
echo "[CHECK] 로컬 모델 파일 확인..."

MODEL_8B="$MODEL_DIR/qwen3-8b-summarizer-q4km.gguf"
MODEL_14B="$MODEL_DIR/qwen3-14b.Q4_K_M.gguf"

if [ ! -f "$MODEL_8B" ]; then
    echo "[ERROR] 8B 모델 없음: $MODEL_8B"
    exit 1
fi
echo "  ✅ 8B 모델: $(ls -lh "$MODEL_8B" | awk '{print $5}')"

if [ ! -f "$MODEL_14B" ]; then
    echo "[ERROR] 14B 모델 없음: $MODEL_14B"
    exit 1
fi
echo "  ✅ 14B 모델: $(ls -lh "$MODEL_14B" | awk '{print $5}')"

echo ""
echo "총 용량: $(du -sh "$MODEL_DIR" | awk '{print $1}')"
echo ""

read -p "업로드를 시작하시겠습니까? (y/N) " confirm
if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "취소됨"
    exit 0
fi

# EC2에 models 디렉토리 생성
echo ""
echo "[1/3] EC2 디렉토리 생성..."
ssh -i "$PEM_KEY" "$EC2_HOST" "mkdir -p ~/ai-inference/models"

# 8B 모델 업로드
echo "[2/3] 8B 모델 업로드 중... (약 4.7GB)"
scp -i "$PEM_KEY" "$MODEL_8B" "$EC2_HOST:~/ai-inference/models/"

# 14B 모델 업로드
echo "[3/3] 14B 모델 업로드 중... (약 8.4GB)"
scp -i "$PEM_KEY" "$MODEL_14B" "$EC2_HOST:~/ai-inference/models/"

echo ""
echo "=========================================="
echo "업로드 완료!"
echo "=========================================="
echo ""
echo "EC2에서 확인:"
echo "  ssh -i $PEM_KEY $EC2_HOST"
echo "  ls -lh ~/ai-inference/models/"
echo ""
echo "Whisper large-v3는 첫 실행 시 자동 다운로드됩니다."
echo "=========================================="
