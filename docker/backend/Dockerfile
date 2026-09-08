# Multi-stage Dockerfile for Nivya Spring Boot Backend
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY backend/pom.xml .
# Download dependencies for faster cached rebuilds
# RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Build application package skipping tests in docker build
# RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create unprivileged application user for security
RUN groupadd -r nivya && useradd -r -g nivya -m -d /home/nivya nivya
USER nivya:nivya

# Health check endpoint via Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# The JAR will be produced in subsequent build phases
# COPY --from=builder /app/target/nivya-backend-*.jar app.jar
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
