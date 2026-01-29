# AI 추론 서버 Dockerfile
# CPU 버전 (GPU 버전은 Dockerfile.gpu 참고)

FROM python:3.11-slim

WORKDIR /app

# 시스템 패키지 (ffmpeg for audio processing)
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Python 패키지
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 앱 복사
COPY inference_server.py .

# 모델 디렉토리 (볼륨 마운트용)
RUN mkdir -p /app/models

# 환경 변수
ENV MODEL_PATH=/app/models/qwen3-8b-summarizer-q4km.gguf
ENV WHISPER_MODEL=medium
ENV WHISPER_DEVICE=cpu
ENV N_CTX=4096
ENV N_GPU_LAYERS=0
ENV N_THREADS=4
ENV HOST=0.0.0.0
ENV PORT=8000

EXPOSE 8000

# 헬스체크
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8000/health').raise_for_status()"

CMD ["python", "inference_server.py"]
