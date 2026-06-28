FROM solra/java-base:17
LABEL service="soc-service"
COPY build/libs/*.jar app.jar
EXPOSE 8091 9091
ENTRYPOINT ["java", "-jar", "/app.jar"]
