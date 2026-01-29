#!/bin/bash
# EC2 AI 추론 서버 초기 설정 스크립트
# Ubuntu 22.04 기준

set -e

echo "=========================================="
echo "ModuStudy AI 추론 서버 설정"
echo "=========================================="

# 1. 시스템 업데이트
echo "[1/5] 시스템 업데이트..."
sudo apt-get update
sudo apt-get upgrade -y

# 2. Docker 설치
echo "[2/5] Docker 설치..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo "Docker 설치 완료. 재로그인 필요."
else
    echo "Docker 이미 설치됨"
fi

# 3. Docker Compose 설치
echo "[3/5] Docker Compose 설치..."
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose 설치 완료"
else
    echo "Docker Compose 이미 설치됨"
fi

# 4. 디렉토리 구조 생성
echo "[4/5] 디렉토리 구조 생성..."
mkdir -p ~/ai-inference/models
mkdir -p ~/ai-inference/logs

# 5. 환경 파일 생성
echo "[5/5] 환경 파일 생성..."
if [ ! -f ~/ai-inference/.env ]; then
    cat > ~/ai-inference/.env << 'EOF'
# AI 추론 서버 환경 변수
MODEL_PATH=/app/models/qwen3-8b-summarizer-q4km.gguf
WHISPER_MODEL=medium
WHISPER_DEVICE=cpu
N_CTX=4096
N_GPU_LAYERS=0
N_THREADS=4
HOST=0.0.0.0
PORT=8000
REDIS_URL=redis://redis:6379
EOF
    echo ".env 파일 생성 완료"
else
    echo ".env 파일 이미 존재"
fi

echo ""
echo "=========================================="
echo "설정 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "1. 재로그인 (Docker 그룹 적용)"
echo "   exit && ssh <서버>"
echo ""
echo "2. 모델 파일 복사"
echo "   scp qwen3-8b-summarizer-q4km.gguf <서버>:~/ai-inference/models/"
echo ""
echo "3. 서버 시작"
echo "   cd ~/ai-inference && docker-compose up -d"
echo ""
echo "4. 상태 확인"
echo "   curl http://localhost:8000/health"
echo "=========================================="
