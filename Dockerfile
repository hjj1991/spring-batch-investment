FROM arm64v8/openjdk:25-slim
COPY build/libs/spring-batch-investment.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
