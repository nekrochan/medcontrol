FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY docker/med_control-1.0-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]