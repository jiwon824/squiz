#!/bin/bash
# Spot GPU 인스턴스 초기화 스크립트
# EC2 시작 시 자동 실행됨

set -e

echo "=========================================="
echo "ModuStudy AI GPU 서버 초기화"
echo "=========================================="

# 1. 시스템 업데이트
apt-get update
apt-get upgrade -y

# 2. Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker ubuntu

# 3. NVIDIA Container Toolkit 설치
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | tee /etc/apt/sources.list.d/nvidia-docker.list
apt-get update
apt-get install -y nvidia-container-toolkit
systemctl restart docker

# 4. 프로젝트 디렉토리 생성
mkdir -p /home/ubuntu/ai-inference/models
chown -R ubuntu:ubuntu /home/ubuntu/ai-inference

# 5. Docker Compose 설치
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 6. 환경 변수 파일 생성
cat > /home/ubuntu/ai-inference/.env << 'EOF'
MODEL_PATH=/app/models/qwen3-8b-summarizer-q4km.gguf
WHISPER_MODEL=medium
WHISPER_DEVICE=cuda
N_CTX=4096
N_GPU_LAYERS=-1
N_THREADS=4
HOST=0.0.0.0
PORT=8000
EOF

chown ubuntu:ubuntu /home/ubuntu/ai-inference/.env

echo "=========================================="
echo "초기화 완료!"
echo "모델 파일 업로드 후 docker-compose up -d 실행"
echo "=========================================="
