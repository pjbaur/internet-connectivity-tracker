# === Build stage ===
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# === Runtime stage ===
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/internet-connectivity-tracker-0.1.0-SNAPSHOT.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
