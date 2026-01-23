# CS Quiz AI Service Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 시스템 의존성 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 의존성 먼저 복사 (캐싱 활용)
COPY requirements.txt .

# PyTorch + torchvision CPU 버전 설치 (호환성 보장)
RUN pip install --no-cache-dir torch==2.2.0 torchvision==0.17.0 --index-url https://download.pytorch.org/whl/cpu

# 나머지 의존성 설치
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 복사
COPY . .

# 데이터 디렉토리 생성
RUN mkdir -p /app/data

EXPOSE 5000

# 환경변수 기본값
ENV HOST=0.0.0.0
ENV PORT=5000
ENV DEBUG=False

# gunicorn으로 프로덕션 실행 (requirements.txt에 포함됨)
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "--timeout", "120", "app:create_app()"]
