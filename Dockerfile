# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
ARG SERVICE

COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY services/api-gateway/pom.xml services/api-gateway/pom.xml
COPY services/config-server/pom.xml services/config-server/pom.xml
COPY services/discovery-server/pom.xml services/discovery-server/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/payment-service/pom.xml services/payment-service/pom.xml
COPY services/product-service/pom.xml services/product-service/pom.xml
COPY services/user-service/pom.xml services/user-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress \
    --projects "services/${SERVICE}" --also-make \
    dependency:go-offline

COPY common/src common/src
COPY services services

RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress \
    --projects "services/${SERVICE}" --also-make \
    package -DskipTests \
    && java -Djarmode=tools \
       -jar "services/${SERVICE}/target/${SERVICE}-1.0.0.jar" \
       extract --layers --destination /workspace/extracted

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
ARG SERVICE_PORT=8080
RUN apk add --no-cache wget
USER 10001:10001

COPY --from=build --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/extracted/application/ ./

EXPOSE ${SERVICE_PORT}
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
