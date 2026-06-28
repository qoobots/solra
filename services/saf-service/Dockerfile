FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.title="Solra SAF Service"
LABEL org.opencontainers.image.description="Content Safety & Moderation"

RUN addgroup -S solra && adduser -S solra -G solra
WORKDIR /app
COPY build/libs/saf-service-*.jar app.jar
USER solra
EXPOSE 8084 9084
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
