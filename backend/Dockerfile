FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn -q -e -pl framework -am install -DskipTests
WORKDIR /app/backend
RUN mvn -q -e -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=build /app/backend/target/portal-backend-1.0.0.jar app.jar

# Set permissions
RUN chown appuser:appuser /app/app.jar

USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080

# Removed JAVA_TOOL_OPTIONS (JDWP) for production security
# Use exec to replace shell with java process
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]