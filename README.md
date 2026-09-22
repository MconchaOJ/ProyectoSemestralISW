# Sistema de Formación Práctica PEM-UBB

Proyecto semestral de Ingeniería de Software (UBB, Pedagogía en Educación Matemática): sistema web para evaluar y
monitorear las 6 prácticas de cada estudiante. Backend en **Java 17 + Spring Boot 3 + MySQL 8**, con un front-end
web en HTML + CSS + JavaScript puro servido por la misma aplicación.

Contexto completo del proyecto (requerimientos del cliente, decisiones y supuestos): [`RESUMEN.md`](RESUMEN.md).
Instrucciones detalladas para trabajar en el código: [`CLAUDE.md`](CLAUDE.md).

## Arrancar todo en 5 minutos

**Requisitos:** JDK 17, Maven 3.9 y MySQL 8 (servicio encendido). Windows con PowerShell.

1. **Variables de entorno** (una sola vez; abre una terminal nueva después). Las claves nunca van en archivos del
   repositorio, solo en variables de entorno de Windows — ver [`.env.example`](.env.example) para los nombres y el
   detalle:
   - `ISW_DB_ROOT_PASSWORD`: clave de `root` de MySQL (solo para crear la base).
   - `ISW_DB_PASSWORD`: clave del usuario `isw_app` (la usa la aplicación). Si no existe, `db\bd.ps1` la genera sola.
   - `ISW_JWT_SECRET` (opcional): 32 caracteres o más; si falta, se genera uno temporal al arrancar.

2. **Base de datos** (crea `isw_practicas`, sus 16 tablas y los datos de demostración):
   ```powershell
   .\db\bd.ps1
   ```

3. **Backend** (compila, corre las 161 pruebas y deja el `.jar`):
   ```powershell
   cd backend
   mvn clean package
   java -jar target\practicas-0.1.0-SNAPSHOT.jar
   ```

4. **Front-end:** con la aplicación arriba, abre <http://localhost:8080> (usuarios de demostración y su clave en el
   encabezado de `db/seed.sql`). Swagger UI para probar la API directamente: <http://localhost:8080/swagger-ui.html>.

Detalle completo (variables de entorno, estructura del código, endpoints, problemas frecuentes): [`backend/README.md`](backend/README.md).

## Estructura del repositorio

```
backend/     API Spring Boot + front-end estático (src/main/resources/static/) + pruebas
db/          schema.sql, seed.sql, verificacion.sql, migraciones/ y el script bd.ps1
Sprint1/     Product Backlog, Sprint Backlog, MER, diagramas BPMN (as-is/to-be) y retrospectiva
```

## Estado del proyecto

Backend, base de datos y front-end del Sprint 1 están terminados y probados (161 pruebas automáticas). El detalle de
cada historia, los endpoints y los supuestos abiertos están en [`backend/PROGRESO.md`](backend/PROGRESO.md); el
Product Backlog y el Sprint Backlog, en [`Sprint1/`](Sprint1/).
