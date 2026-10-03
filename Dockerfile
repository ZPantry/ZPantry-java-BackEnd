# syntax=docker/dockerfile:1

FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies separately so ordinary source edits reuse this layer.
COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# The application has no need to run as root inside the container.
RUN groupadd --system zpantry && useradd --system --gid zpantry --home-dir /app zpantry
COPY --from=build --chown=zpantry:zpantry /workspace/target/z_pantry_backend-*.jar /app/app.jar

USER zpantry
EXPOSE 8080

# Runtime secrets and deployment configuration are supplied as environment variables.
ENTRYPOINT ["java","-Xmx256m","-XX:MaxMetaspaceSize=128m","-XX:+UseSerialGC","-XX:TieredStopAtLevel=1","-Xss512k","-jar","app.jar"]
