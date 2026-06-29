FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="solra-team"
LABEL service="mon-service"

RUN addgroup -S solra && adduser -S solra -G solra

WORKDIR /app

COPY build/libs/solra-mon-service-*.jar app.jar

USER solra
EXPOSE 8095

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8095/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
