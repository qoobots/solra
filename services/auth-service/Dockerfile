FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="Solra Auth Service"
LABEL org.opencontainers.image.description="Identity & Access Management"
LABEL org.opencontainers.image.version="0.1.0-SNAPSHOT"

RUN addgroup -S solra && adduser -S solra -G solra

WORKDIR /app

COPY build/libs/auth-service-*.jar app.jar

USER solra

EXPOSE 8081 9081

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
