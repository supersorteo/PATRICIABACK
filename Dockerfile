FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/exellsior-0.0.1-SNAPSHOT.jar java-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "java-app.jar"]
