FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
WORKDIR /app/backend
RUN mvn -q -e -DskipTests dependency:go-offline
COPY backend/src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/backend/target/portal-backend-1.0.0.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8787"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]