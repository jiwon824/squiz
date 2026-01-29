#!/bin/bash
# SSAFY 서버에서 GGUF 모델 다운로드
# 사용법: ./download-model.sh [ssafy-host]

set -e

SSAFY_HOST=${1:-"j-i14d106@i14d106.p.ssafy.io"}
MODEL_NAME="qwen3-8b-summarizer-q4km.gguf"
REMOTE_PATH="~/models/$MODEL_NAME"
LOCAL_PATH="./models/$MODEL_NAME"

SCRIPT_DIR=$(dirname "$0")
PROJECT_DIR=$(dirname "$SCRIPT_DIR")

cd "$PROJECT_DIR"

echo "=========================================="
echo "GGUF 모델 다운로드"
echo "=========================================="
echo "소스: $SSAFY_HOST:$REMOTE_PATH"
echo "대상: $LOCAL_PATH"
echo ""

# 모델 디렉토리 생성
mkdir -p models

# 이미 존재하는지 확인
if [ -f "$LOCAL_PATH" ]; then
    echo "[INFO] 모델 파일이 이미 존재합니다."
    ls -lh "$LOCAL_PATH"

    read -p "덮어쓰시겠습니까? (y/N) " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        echo "취소됨"
        exit 0
    fi
fi

# SCP로 다운로드
echo "다운로드 중... (약 4.7GB)"
scp "$SSAFY_HOST:$REMOTE_PATH" "$LOCAL_PATH"

echo ""
echo "=========================================="
echo "다운로드 완료!"
echo "=========================================="
ls -lh "$LOCAL_PATH"
