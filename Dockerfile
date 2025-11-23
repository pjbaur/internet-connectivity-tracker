# === Build stage ===
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies first
COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline --no-transfer-progress

# Copy the rest of the source code
COPY src ./src

# Build the JAR (reproducible, layered, fast)
RUN mvn -B -q -e package -DskipTests --no-transfer-progress

# === Runtime stage ===
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built application
COPY --from=build /app/target/*.jar app.jar

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Helpful optimization (optional): smaller DNS cache
ENV JAVA_OPTS="-Xms256m -Xmx256m"

EXPOSE 8080

# Use JSON array ENTRYPOINT (no shell needed)
ENTRYPOINT ["java", "-jar", "app.jar"]
