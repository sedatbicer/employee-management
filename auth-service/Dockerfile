
# Mehrstufiger Docker-Build für den Auth-Service.
# Die Trennung von Build- und Runtime-Image reduziert
# die Größe und Angriffsfläche des finalen Images.

# --------------------------------------------------
# Stage 1: Build der Spring-Boot-Anwendung
# --------------------------------------------------

# Maven mit Eclipse Temurin Java 21 als Build-Umgebung.
FROM maven:3.9-eclipse-temurin-21 AS build

# Arbeitsverzeichnis innerhalb des Build-Containers.
WORKDIR /app

# Maven-Projektdefinition und Quellcode kopieren.
COPY pom.xml .
COPY src ./src

# Spring-Boot-Anwendung paketieren.
# Tests werden hier übersprungen und separat ausgeführt.
RUN mvn -B clean package -DskipTests


# --------------------------------------------------
# Stage 2: Runtime-Image
# --------------------------------------------------

# Schlankes Java-21-Runtime-Image ohne Maven.
FROM eclipse-temurin:21-jre

# Arbeitsverzeichnis für die Anwendung.
WORKDIR /app

# Dedizierten Systembenutzer für den Anwendungsbetrieb erstellen.
# Die Anwendung wird nicht mit Root-Rechten ausgeführt.
RUN useradd --system --uid 1001 appuser

# Erstelltes Spring-Boot-JAR aus der Build-Stage übernehmen.
COPY --from=build /app/target/*.jar app.jar

# Container-Prozesse mit eingeschränkten Benutzerrechten ausführen.
USER appuser

# HTTP-Port des Auth-Service dokumentieren.
EXPOSE 8082

# Spring-Boot-Anwendung starten.
ENTRYPOINT ["java", "-jar", "app.jar"]
