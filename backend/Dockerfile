# syntax=docker/dockerfile:1.7
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system bridgeflow \
    && useradd --system --gid bridgeflow --home-dir /app --shell /usr/sbin/nologin bridgeflow
WORKDIR /app
COPY --from=build /workspace/target/bridgeflow-api-*.jar /app/bridgeflow-api.jar
RUN mkdir -p /var/lib/bridgeflow/documents && chown -R bridgeflow:bridgeflow /app /var/lib/bridgeflow
USER bridgeflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/bridgeflow-api.jar"]
