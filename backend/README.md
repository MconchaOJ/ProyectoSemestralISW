# Backend — Sistema de Formación Práctica PEM-UBB (Sprint 1)

API REST en **Java 17 + Spring Boot 3 + MySQL 8** con las 5 historias del Sprint 1:

| HU | Qué hace |
|---|---|
| HU-01 | Inicio de sesión con token JWT y 4 roles (ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR) |
| HU-02 | Registro de estudiantes y profesores, las 6 asignaturas de práctica e inscripciones por periodo |
| HU-03 | Pauta de observación de clase digital (20 indicadores, escala 1 a 7) y evaluación con puntaje automático |
| HU-04 | Planificación de clase con flujo estudiante → profesor guía → profesor del curso |
| HU-05 | Historial (recorrido) del estudiante con sus evaluaciones y planificaciones |

Además trae un **front-end web** (HTML + CSS + JavaScript puro) que se abre en <http://localhost:8080> una vez arrancada la aplicación (ver la sección 5).

Estado del trabajo, endpoints y supuestos: [`PROGRESO.md`](PROGRESO.md).

## 1. Requisitos

| Herramienta | Versión | Cómo comprobarla |
|---|---|---|
| JDK | 17 o superior | `java -version` |
| Maven | 3.9 | `mvn -version` |
| MySQL | 8.x, servicio encendido en `localhost:3306` | `Get-Service MySQL84` |

Si falta alguno, instálalo antes (por ejemplo `winget install EclipseAdoptium.Temurin.17.JDK`) y abre una terminal nueva.

## 2. Variables de entorno (nunca escribas las claves en archivos)

Las claves viven solo en variables de entorno de Windows. **No hay ninguna clave en este repositorio.**

| Variable | Para qué | Obligatoria |
|---|---|---|
| `ISW_DB_PASSWORD` | Clave del usuario `isw_app` de MySQL (la aplicación se conecta con él, nunca como `root`) | Sí |
| `ISW_DB_ROOT_PASSWORD` | Clave de `root`. **Solo** la usa `db\bd.ps1` para crear la base | Solo para crearla |
| `ISW_JWT_SECRET` | Secreto de firma del token (32 caracteres o más). Si falta, se genera uno al arrancar y los tokens dejan de valer al reiniciar | No |
| `ISW_DB_HOST`, `ISW_DB_PORT` | Servidor y puerto de MySQL (por defecto `127.0.0.1` y `3306`) | No |

Cómo definir una (PowerShell; no deja la clave en el historial):

```powershell
$s = Read-Host 'Clave' -AsSecureString
$clave = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($s))
[Environment]::SetEnvironmentVariable('ISW_DB_PASSWORD', $clave, 'User')
```

Luego **abre una terminal nueva** para que la variable quede disponible. La plantilla `.env.example` (en la raíz) solo trae los nombres.

## 3. Crear la base de datos (una sola vez)

Desde la raíz del proyecto:

```powershell
.\db\bd.ps1                    # crea la base isw_practicas, las 17 tablas, los datos de demo y el usuario isw_app
.\db\bd.ps1 -Accion Verificar  # solo verifica (23 pruebas negativas incluidas)
```

`bd.ps1` **borra y recrea** las tablas: úsalo también para volver a los datos de demostración originales.
La aplicación no crea ni modifica tablas (`spring.jpa.hibernate.ddl-auto=validate`: solo comprueba que el modelo Java coincida con la base).

## 4. Compilar, probar y ejecutar

```powershell
cd backend
mvn clean package              # compila y corre las 161 pruebas (necesitan la base encendida y con los datos de demo)
java -jar target\practicas-0.1.0-SNAPSHOT.jar
```

o, en desarrollo, `mvn spring-boot:run`. La API queda en <http://localhost:8080>.

- **Front-end web:** <http://localhost:8080> (entra con un usuario de demostración; el menú cambia según el rol)
- **Swagger UI (para probar y demostrar):** <http://localhost:8080/swagger-ui.html>
- Especificación OpenAPI: <http://localhost:8080/v3/api-docs>
- Para detenerla: `Ctrl+C` en la terminal donde corre.

Para compilar sin correr las pruebas: `mvn clean package -DskipTests`.

## 5. Abrir el front-end y probar con Swagger

**Front-end:** con la aplicación en marcha, abre <http://localhost:8080> e ingresa con un usuario de demostración (tabla de abajo; la clave está en el encabezado de `db/seed.sql`). No hay nada que instalar: son archivos estáticos en `src/main/resources/static/` (`index.html` es el login y `app.html` la aplicación) que sirve la misma aplicación Spring Boot, por eso no hay CORS. Si editas un archivo del front, hay que recompilar (`mvn clean package -DskipTests`) y reiniciar, porque van dentro del jar.

**Swagger (botón Authorize):**

1. Abre `POST /api/auth/login`, pulsa *Try it out* y entra con un usuario de demostración, por ejemplo
   `{"email": "coordinador@demo.example", "password": "<clave de demostración>"}`.
   La clave de los usuarios ficticios de demostración está en el encabezado de `db/seed.sql`
   (es solo para desarrollo; no debe usarse en ningún sistema real).
2. Copia el campo `token` de la respuesta.
3. Pulsa **Authorize** (arriba a la derecha), pega el token (sin escribir "Bearer") y **Authorize**.
4. Ya puedes ejecutar cualquier endpoint permitido para ese rol. Para cambiar de usuario: *Authorize → Logout* y repite.

Usuarios de demostración (todos con el mismo dominio ficticio `@demo.example`):

| Correo | Rol | Quién es |
|---|---|---|
| `coordinador@demo.example` | COORDINADOR | Andrea Fuentes |
| `guia1@demo.example` | PROFESOR_GUIA | Marcos Bravo (guía de Camila, Valentina y Sebastián) |
| `guia2@demo.example` | PROFESOR_GUIA | Lorena Vidal (guía de Diego, Valentina y Sebastián) |
| `curso1@demo.example` | PROFESOR_CURSO | Ricardo Núñez |
| `curso2@demo.example` | PROFESOR_CURSO | Patricia Contreras |
| `camila.rojas@demo.example` | ESTUDIANTE | Cursa Trabajo Colaborativo |
| `diego.munoz@demo.example` | ESTUDIANTE | Cursa Trabajo Colaborativo |
| `valentina.perez@demo.example` | ESTUDIANTE | Cursa Práctica Pedagógica (4 prácticas aprobadas) |
| `sebastian.vargas@demo.example` | ESTUDIANTE | Recorrido completo de las 6 prácticas |
| `javiera.herrera@demo.example` | ESTUDIANTE | Recién inicia la primera práctica |

## 6. Estructura del código

```
backend/
├── pom.xml
├── PROGRESO.md                    estado, endpoints y SUPUESTOS
└── src/
    ├── main/java/cl/ubb/pem/practicas/
    │   ├── controller/            endpoints REST (delgados: validan y delegan)
    │   ├── service/               reglas de negocio y transacciones
    │   ├── repository/            acceso a MySQL (Spring Data JPA)
    │   ├── entity/                15 entidades JPA (mapean las tablas de db/schema.sql)
    │   ├── dto/                   lo que entra y sale por la API (no se exponen entidades)
    │   ├── security/              JWT, filtro de autenticación, 401 y 403 en JSON
    │   ├── exception/             errores de negocio y manejador global (@RestControllerAdvice)
    │   └── config/                seguridad (CORS, BCrypt) y Swagger
    ├── main/resources/            application.properties y mensajes de validación en español
    │   └── static/                front-end: index.html (login), app.html, css/, js/ (vistas/), img/
    └── test/                      pruebas unitarias (Mockito) y de integración (MockMvc + MySQL real)
```

## 7. Pruebas automáticas

- **Unitarias** (sin base de datos, con Mockito): reglas de cada servicio.
- **De integración** (una clase por historia, `HU0xIntegracionTest`): levantan la aplicación completa contra la MySQL real
  con los datos de `db/seed.sql`. Cada prueba corre en una transacción que se revierte, así que **no ensucian la base**.
- Las pruebas de integración **asumen los datos exactos del seed**. Si tocaste la base a mano (por ejemplo probando en
  Swagger), algunas pueden fallar: ejecuta `.\db\bd.ps1` para volver al estado original y repite.

## 8. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `Could not resolve placeholder 'ISW_DB_PASSWORD'` | Falta la variable de entorno o la terminal es anterior a definirla. Defínela y abre una terminal nueva |
| `Access denied for user 'isw_app'` | La clave de la variable no es la del usuario. Ejecuta `.\db\bd.ps1` (vuelve a asignar esa clave al usuario en MySQL) |
| `Communications link failure` / `Connection refused` | MySQL apagado: `Start-Service MySQL84` |
| `Schema-validation: missing table` | La base no está creada o está desactualizada: `.\db\bd.ps1` |
| `Port 8080 was already in use` | Otra instancia corriendo. Ciérrala o usa `--server.port=8081` |
| Todo da 401 en Swagger | Falta pegar el token en **Authorize**, o expiró (dura 8 horas) o la app se reinició sin `ISW_JWT_SECRET` |
| Algunas pruebas de integración fallan | La base no tiene los datos exactos del seed: `.\db\bd.ps1` |
