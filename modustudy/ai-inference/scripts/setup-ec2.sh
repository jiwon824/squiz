#!/bin/bash
# EC2 AI 추론 서버 초기 설정 스크립트
# Deep Learning OSS Nvidia Driver AMI GPU PyTorch (Ubuntu 24.04) 기준
# NVIDIA 드라이버, CUDA 사전 설치됨

set -e

echo "=========================================="
echo "ModuStudy AI 추론 서버 설정"
echo "AMI: Deep Learning OSS Nvidia Driver AMI"
echo "=========================================="

# 0. GPU 확인
echo "[0/6] GPU 상태 확인..."
nvidia-smi || { echo "[ERROR] GPU가 감지되지 않습니다. AMI를 확인하세요."; exit 1; }
echo ""

# 1. 시스템 업데이트
echo "[1/6] 시스템 업데이트..."
sudo apt-get update
sudo apt-get upgrade -y

# 2. Docker 설치
echo "[2/6] Docker 설치..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo "Docker 설치 완료"
else
    echo "Docker 이미 설치됨"
fi

# 3. Docker Compose 설치
echo "[3/6] Docker Compose 설치..."
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "Docker Compose 설치 완료"
else
    echo "Docker Compose 이미 설치됨"
fi

# 4. NVIDIA Container Toolkit 설치 (Docker에서 GPU 사용)
echo "[4/6] NVIDIA Container Toolkit 설치..."
if ! dpkg -l | grep -q nvidia-container-toolkit; then
    distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
    curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
    curl -s -L https://nvidia.github.io/libnvidia-container/$distribution/libnvidia-container.list | \
        sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
        sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
    sudo apt-get update
    sudo apt-get install -y nvidia-container-toolkit
    sudo nvidia-ctk runtime configure --runtime=docker
    sudo systemctl restart docker
    echo "NVIDIA Container Toolkit 설치 완료"
else
    echo "NVIDIA Container Toolkit 이미 설치됨"
fi

# 5. 디렉토리 구조 생성
echo "[5/6] 디렉토리 구조 생성..."
mkdir -p ~/ai-inference/models
mkdir -p ~/ai-inference/logs

# 6. 환경 파일 생성 (GPU 모드)
echo "[6/6] 환경 파일 생성..."
if [ ! -f ~/ai-inference/.env ]; then
    cat > ~/ai-inference/.env << 'EOF'
# AI 추론 서버 환경 변수 (GPU 모드)

# LLM 모델
MODEL_PATH=/app/models/qwen3-8b-summarizer-q4km.gguf
RECOMMEND_MODEL_PATH=/app/models/qwen3-14b.Q4_K_M.gguf
N_CTX=4096
N_GPU_LAYERS=-1
N_THREADS=4

# Whisper STT
WHISPER_MODEL=large-v3
WHISPER_DEVICE=cuda

# Server
HOST=0.0.0.0
PORT=8000

# Redis
REDIS_URL=redis://redis:6379

# Claude API (선택)
CLAUDE_API_KEY=
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
echo ""
echo "1. 재로그인 (Docker 그룹 적용)"
echo "   exit"
echo "   ssh -i <key.pem> ubuntu@<서버IP>"
echo ""
echo "2. 모델 파일 업로드 (로컬에서 실행)"
echo "   scp -i <key.pem> qwen3-8b-summarizer-q4km.gguf ubuntu@<서버IP>:~/ai-inference/models/"
echo "   scp -i <key.pem> qwen3-14b.Q4_K_M.gguf ubuntu@<서버IP>:~/ai-inference/models/"
echo ""
echo "3. 배포 파일 복사 및 서버 시작"
echo "   cd ~/ai-inference"
echo "   ./scripts/deploy.sh gpu"
echo ""
echo "4. 상태 확인"
echo "   curl http://localhost:8000/health"
echo "=========================================="
