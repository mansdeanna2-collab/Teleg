FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/admin-server.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "app.jar"]
