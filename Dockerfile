FROM openjdk:17-jdk-slim as build
WORKDIR /opt
COPY . .
RUN ./mvnw clean package
RUN mv ./target/zookeeper-client.jar /opt/app.jar

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /opt/app.jar /opt/
ENTRYPOINT ["java", "-jar", "/opt/app.jar"]
