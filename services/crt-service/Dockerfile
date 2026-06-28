FROM solra/java-base:17
LABEL service="crt-service"
COPY build/libs/*.jar app.jar
EXPOSE 8094 9094
ENTRYPOINT ["java", "-jar", "/app.jar"]
