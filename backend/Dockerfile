FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy dependency definitions first for layer caching
COPY pom.xml .
COPY framework/pom.xml framework/
COPY backend/pom.xml backend/
COPY frontend/pom.xml frontend/

# Install framework module (requires source) & resolve backend dependencies (cached layer)
COPY framework/src framework/src
RUN mvn -q -e -pl framework -am install -DskipTests
WORKDIR /app/backend
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy source last — only this layer rebuilds on code changes
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/backend/target/app.jar app.jar

# Set permissions
RUN chown appuser:appuser /app/app.jar

USER appuser

# Empty default — override via docker-compose.yml or orchestrator
ENV JAVA_OPTS=""
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -sf http://localhost:${APP_PORT:-8080}/api/actuator/health || exit 1

# Use exec to replace shell with java process
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]