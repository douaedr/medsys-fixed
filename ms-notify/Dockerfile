FROM openjdk:21-jdk-slim AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn -q -DskipTests package

FROM openjdk:21-jdk-slim
WORKDIR /app
RUN groupadd -r medsys && useradd -r -g medsys medsys
COPY --from=builder /app/target/*.jar app.jar
USER medsys
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
