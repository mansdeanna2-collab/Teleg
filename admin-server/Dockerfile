# ---- Stage 1: Build ----
FROM gradle:8.7.0-jdk17 AS builder

WORKDIR /build

# Copy gradle config files first for better layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Copy source code
COPY src ./src

# Build the JAR
RUN ./gradlew bootJar --no-daemon

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

RUN mkdir -p /app/data

COPY --from=builder /build/build/libs/admin-server.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=production

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
