# Stage 1: Maven build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: JRE runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S mos && adduser -S mos -G mos \
    && apk add --no-cache curl

COPY --from=build /app/target/*.jar app.jar

USER mos
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
