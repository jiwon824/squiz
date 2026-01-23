# ===========================================
# Stage 1: Build
# ===========================================
FROM node:20-alpine AS builder

# UTF-8 인코딩 설정
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

# 의존성 파일 복사
COPY package*.json ./

# 의존성 설치
RUN npm ci

# 소스 코드 복사
COPY . .

# node_modules 실행 권한 부여 후 빌드 (타입 체크 스킵)
RUN chmod -R +x node_modules/.bin && ./node_modules/.bin/vite build

# ===========================================
# Stage 2: Production (Nginx)
# ===========================================
FROM nginx:alpine

# 빌드된 파일 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx 설정 (SPA 라우팅)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
