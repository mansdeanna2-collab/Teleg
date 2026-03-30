FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

RUN mkdir -p /app/data

COPY build/libs/admin-server.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=production

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
