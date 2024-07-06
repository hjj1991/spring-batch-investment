FROM arm64v8/openjdk:21-slim
COPY build/libs/spring-batch-investment-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
