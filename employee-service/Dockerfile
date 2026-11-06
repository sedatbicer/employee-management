
# Stage 1: Build der Spring-Boot-Anwendung
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Maven-Konfiguration und Quellcode kopieren
COPY pom.xml .
COPY src ./src

# Anwendung bauen
RUN mvn -B clean package -DskipTests


# Stage 2: Runtime-Image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Nicht als Root ausführen
RUN useradd --system --uid 1001 appuser

# Fertiges JAR aus der Build-Stage übernehmen
COPY --from=build /app/target/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
