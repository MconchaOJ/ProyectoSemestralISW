# =============================================================================
# Dockerfile - Backend Spring Boot (isw-practicas)
#
# Multi-stage build:
#   1) builder  → descarga dependencias (cacheado) + compila el JAR
#   2) runtime  → imagen ligera JRE-only para ejecutar la aplicación
#
# Versiones fijadas al proyecto:
#   Java 17  (pom.xml → java.version)
#   Maven 3.9.16  (backend/.mvn/wrapper/maven-wrapper.properties)
# =============================================================================

# ── Etapa 1: compilación ─────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1a. Copiar solo los archivos de definición de dependencias.
#     Docker cachea esta capa: si pom.xml no cambia, no re-descarga nada.
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# 1b. Copiar el código fuente y compilar (sin tests para agilizar la imagen).
COPY src ./src
RUN mvn package -B -DskipTests \
    && mv target/practicas-*.jar target/app.jar

# ── Etapa 2: runtime ligero ──────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy AS runtime

# Metadatos de la imagen
LABEL maintainer="equipo-isw"
LABEL description="ISW Prácticas - Backend API (Spring Boot 3.5)"

# Usuario no-root por seguridad
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

WORKDIR /app

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /build/target/app.jar ./app.jar

# Crear la carpeta de adjuntos con permisos correctos
RUN mkdir -p /app/adjuntos && chown -R appuser:appgroup /app

USER appuser

# Puerto por defecto de Spring Boot
EXPOSE 8080

# Healthcheck: verifica que Tomcat y Spring respondan HTTP 200 en la raíz
HEALTHCHECK --interval=20s --timeout=5s --start-period=35s --retries=3 \
    CMD curl -f -s http://localhost:8080/ || exit 1

# Arranque con buenas prácticas JVM para contenedores
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
