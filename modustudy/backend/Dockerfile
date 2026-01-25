# ===========================================
# Build Stage
# ===========================================
FROM gradle:8.5-jdk21 AS build

WORKDIR /app

# Gradle 캐시 활용을 위해 의존성 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시)
RUN gradle dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src ./src
RUN gradle build -x test --no-daemon

# ===========================================
# Runtime Stage
# ===========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 타임존 설정 (KST) + 한글 폰트 설치 (PDF 생성용)
RUN apk add --no-cache tzdata font-noto-cjk \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

# 업로드 디렉토리 생성
RUN mkdir -p /app/uploads

# 빌드된 JAR 복사 (plain.jar 제외)
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행 (타임존 JVM 옵션 추가)
ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Seoul", "-Dspring.profiles.active=prod", "app.jar"]
