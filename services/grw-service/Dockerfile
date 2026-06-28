FROM solra/java-base:17
LABEL service="grw-service"
COPY build/libs/*.jar app.jar
EXPOSE 8092 9092
ENTRYPOINT ["java", "-jar", "/app.jar"]
