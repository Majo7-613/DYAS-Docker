# syntax=docker/dockerfile:1

# Etapa 1: compilar el .jar con Maven (las pruebas ya corrieron en el pipeline de CI)
FROM maven:3.9.16-eclipse-temurin-25-noble AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -DskipTests \
 && cp target/tareas-*.jar /app.jar

# Etapa 2: imagen final solo con el JRE y el .jar
FROM eclipse-temurin:25.0.4.1_1-jre-noble
# Las imágenes de Temurin 25 ya no traen curl: se instala solo para el HEALTHCHECK
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /app.jar app.jar
USER app
EXPOSE 8080

# Docker y Compose consultan este endpoint para saber si la aplicación está lista
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

# La JVM usa como máximo el 75 % de la memoria que el contenedor tenga asignada
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
