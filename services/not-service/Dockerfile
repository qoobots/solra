FROM solra/java-base:17
LABEL service="not-service"
COPY build/libs/*.jar app.jar
EXPOSE 8093 9093
ENTRYPOINT ["java", "-jar", "/app.jar"]
