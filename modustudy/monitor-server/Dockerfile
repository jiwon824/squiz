# ===========================================
# Monitor Server (Lightweight Monitoring)
# ===========================================
FROM node:22-alpine

WORKDIR /app

# 의존성 설치
COPY package*.json ./
RUN npm install --omit=dev

# 소스 복사
COPY src ./src
COPY public ./public

# 포트 노출
EXPOSE 9090

# 실행
CMD ["node", "src/index.js"]
