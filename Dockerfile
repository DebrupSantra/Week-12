FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
ARG SERVICE
COPY pom.xml .
COPY common common
COPY services services
RUN mvn -B -ntp -pl services/${SERVICE} -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ARG SERVICE
COPY --from=build /workspace/services/${SERVICE}/target/${SERVICE}-1.0.0.jar /app/app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
