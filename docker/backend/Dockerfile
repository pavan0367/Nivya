# ==============================================================================
# Multi-Stage Dockerfile for Nivya Spring Boot Backend (Java 21)
# ==============================================================================

# Stage 1: Build Stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache Maven dependencies layer
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production artifact
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Secure Runtime Stage
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Install curl for reliable container health checks and clean up apt cache
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create dedicated unprivileged service group and user
RUN groupadd -r nivya && useradd -r -g nivya -m -d /home/nivya nivya

# Copy compiled JAR from builder stage with proper ownership
COPY --from=builder --chown=nivya:nivya /app/target/*.jar app.jar

# Run as non-root user
USER nivya:nivya

# Expose internal backend port
EXPOSE 8080

# Health check via Nivya health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Production JVM flags for container awareness and performance
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Duser.timezone=UTC", \
  "-jar", "app.jar"]
