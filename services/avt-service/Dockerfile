FROM eclipse-temurin:17-jre-alpine
LABEL org.opencontainers.image.title="Solra AVT Service"
RUN addgroup -S solra && adduser -S solra -G solra
WORKDIR /app
COPY build/libs/avt-service-*.jar app.jar
USER solra
EXPOSE 8082 9082
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
