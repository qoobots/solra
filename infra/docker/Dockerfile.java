# Solra - Standard Java microservice Dockerfile
# Usage: Place in service root and build with Gradle shadow JAR

FROM eclipse-temurin:17-jre-alpine

LABEL org.opencontainers.image.authors="Solra Team"
LABEL org.opencontainers.image.vendor="Solra Project"

# Security: non-root user
RUN addgroup -S solra && adduser -S solra -G solra

WORKDIR /app

# Copy the fat JAR (built by Gradle bootJar)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# Create writable directories for temp/upload
RUN mkdir -p /app/tmp /app/uploads && chown -R solra:solra /app

USER solra

# Health check via Spring Actuator
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM Flags:
# - ZGC: low-latency GC suitable for services
# - MaxRAMPercentage: container-aware heap sizing
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
