# Sistema de Formación Práctica PEM-UBB

Sistema web para evaluar y monitorear las 6 prácticas de Pedagogía en Educación Matemática (Universidad del Bío-Bío).
Backend en **Java 17 + Spring Boot 3.5 + MySQL 8.4/26.x**, con front-end integrado en HTML5 + CSS3 + Vanilla JavaScript (ES6).

## Requisitos
- **Java**: JDK 17 o superior (`java -version`)
- **Maven**: No requiere instalación global; usa el wrapper incluido (`.\mvnw.cmd -v`)
- **MySQL**: 8.4 LTS o 26.x (`mysql --version`) o Docker con Docker Compose

## Configuración
1. **Variables de entorno:** Copiar `.env.example` como `.env` y configurar las credenciales:
   - `DB_PASSWORD`: Contraseña del usuario `isw_app` de MySQL.
   - `ISW_DB_PASSWORD`: Misma contraseña de aplicación (para compatibilidad de scripts locales).
   - `ISW_DB_ROOT_PASSWORD` (opcional): Solo necesaria si se usa `db\bd.ps1` para inicializar la base localmente.
2. **Base de datos:**
   - **Opción con Docker (recomendada):**
     ```powershell
     docker compose up -d
     ```
   - **Opción sin Docker (MySQL local ya instalado):**
     ```powershell
     .\db\bd.ps1
     ```
   *(Flyway aplica automáticamente las migraciones versionadas en `backend/src/main/resources/db/migration/`).*

## Ejecutar
- **Backend (Desarrollo):**
  ```powershell
  .\mvnw.cmd -f backend/pom.xml spring-boot:run
  ```
  *(o con perfil específico: `.\mvnw.cmd -f backend/pom.xml spring-boot:run "-Dspring-boot.run.profiles=dev"`)*
- **Backend (Empaquetado JAR):**
  ```powershell
  .\mvnw.cmd clean package -DskipTests
  java -jar backend\target\practicas-0.1.0-SNAPSHOT.jar
  ```
- **Front-end:** Con el backend arriba, abrir en el navegador:
  - Aplicación Web: <http://localhost:8080>
  - Swagger UI / OpenAPI: <http://localhost:8080/swagger-ui.html>

## Pruebas
- Ejecutar suite de pruebas unitarias y de servicios:
  ```powershell
  .\mvnw.cmd test "-Dtest=*ServiceTest,JwtServiceTest"
  ```
- Ejecutar suite completa (con base de datos activa):
  ```powershell
  .\mvnw.cmd test
  ```

## Versiones entregadas
- **v1.0.0**: Incremento 1 (Sprint 1: Backend 5 HU, 42 endpoints, Front-end SPA, DB 16 tablas y 67+ pruebas automáticas).

---

### Documentación adicional
- [`RESUMEN.md`](RESUMEN.md): Contexto completo del proyecto, reglas de negocio y supuestos de carrera.
- [`CLAUDE.md`](CLAUDE.md): Pautas de arquitectura y desarrollo.
- [`backend/README.md`](backend/README.md): Detalle técnico del backend, perfiles y endpoints.
- [`Sprint1/`](Sprint1/): Product Backlog, Sprint Backlog y modelos BPMN/MER.
