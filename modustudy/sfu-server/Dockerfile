# ===========================================
# SFU Server (Mediasoup)
# ===========================================
FROM node:22-alpine

# Mediasoup 빌드에 필요한 패키지
RUN apk add --no-cache python3 py3-pip make g++ linux-headers

WORKDIR /app

# 의존성 설치 (캐시)
COPY package*.json ./
RUN npm ci --omit=dev

# 소스 복사
COPY src ./src

# 포트 노출 (4000: socket.io, 20000-22000: WebRTC UDP)
EXPOSE 4000
EXPOSE 20000-22000/udp

# 실행
CMD ["node", "src/index.js"]
