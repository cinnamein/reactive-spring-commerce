# ── Stage 1: Build ──
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper 및 설정 복사 (캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./

RUN chmod +x gradlew

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스 복사
COPY src src

# jar 빌드
RUN ./gradlew bootJar -x test --no-daemon

# ── Stage 2: Runtime ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]