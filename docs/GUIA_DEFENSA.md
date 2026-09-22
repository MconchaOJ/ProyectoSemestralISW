# Guía de defensa — Sprint 1 (backend)

Sistema de Formación Práctica PEM-UBB · Java 17 · Spring Boot 3 · MySQL 8 · entrega del viernes 25 de septiembre de 2026.

> **Cómo usar esta guía:** la sección 1 es la explicación de arquitectura (1 página). La sección 4 trae, por cada historia,
> qué hace, sus endpoints, sus reglas y un **guion para demostrarla en Swagger** con los usuarios de prueba. La sección 5 son
> las preguntas probables con su respuesta. La sección 8 es el guion para demostrar todo con el **front-end web** (<http://localhost:8080>).
> Lo que se asumió sin confirmar está marcado como **SUPUESTO** (lista completa en `backend/PROGRESO.md`).

---

## 0. Antes de la defensa (5 minutos)

1. MySQL encendido: `Get-Service MySQL84` → *Running* (si no: `Start-Service MySQL84`).
2. **Partir de datos limpios** (para que la demo salga como el guion): desde la raíz del proyecto, `.\db\bd.ps1`.
3. Arrancar la API: `cd backend` y `java -jar target\practicas-0.1.0-SNAPSHOT.jar` (si no existe el jar: `mvn clean package -DskipTests`).
4. Abrir el front-end en <http://localhost:8080> (guion en la sección 8) y, si hace falta, Swagger en <http://localhost:8080/swagger-ui.html>.
5. Tener a mano la clave de los usuarios de demostración (encabezado de `db/seed.sql`).

Si un ensayo dejó datos extra (estudiantes, evaluaciones, planificaciones nuevas), repite el paso 2.

---

## 1. Arquitectura en 1 página

```
   Swagger UI / Postman / (front-end estático futuro, CORS abierto a http://localhost:*)
                              │  HTTP + JSON  (Authorization: Bearer <token>)
                              ▼
 ┌────────────────────────────────────────────────────────────────────────────────┐
 │ Spring Security (sin estado)                                                   │
 │   JwtAuthenticationFilter: valida el token y carga al usuario desde la base    │
 │   401 = sin token / token inválido o vencido · 403 = rol sin permiso           │
 ├────────────────────────────────────────────────────────────────────────────────┤
 │ controller   endpoints REST. @PreAuthorize("hasRole(...)") restringe por rol   │
 │              @Valid valida el JSON de entrada (mensajes en español)            │
 ├────────────────────────────────────────────────────────────────────────────────┤
 │ service      reglas de negocio y @Transactional (una petición = una transacción)│
 │              AccesoService: "¿este usuario tiene relación con esta inscripción?"│
 ├────────────────────────────────────────────────────────────────────────────────┤
 │ repository   Spring Data JPA (consultas parametrizadas: sin inyección SQL)     │
 ├────────────────────────────────────────────────────────────────────────────────┤
 │ entity       12 entidades JPA = tablas de db/schema.sql                        │
 └───────────────────────────────────────┬────────────────────────────────────────┘
                                         │ JDBC (usuario isw_app, no root)
                                         ▼
                 MySQL 8.4 · base isw_practicas · 14 tablas, 20 FK, 32 CHECK,
                 triggers (puntaje dentro de la escala, evaluación finalizada inmutable)
```

**Capas y por qué.** El *controller* solo recibe, valida la forma y delega; el *service* concentra las reglas de negocio
(así se prueban sin HTTP y sin base); el *repository* solo habla con MySQL; las *entity* espejan las tablas.
Hacia afuera solo salen **DTO** (`record`), nunca entidades: así no se filtra el hash de la contraseña ni se acopla la API al modelo de la base.
Los errores los convierte un `@RestControllerAdvice` en un JSON uniforme y en español
(`{timestamp, status, error, mensaje, ruta, errores[]}`) con el código HTTP correcto: 400 datos/regla, 401, 403, 404, 409 conflicto de estado.

**Seguridad.**
- *Autenticación:* `POST /api/auth/login` compara la contraseña con **BCrypt** (los hashes de `seed.sql` son `$2a$10$`, el formato de Spring) y entrega un **JWT** firmado (HS256) que contiene el id del usuario y su rol y **expira a las 8 horas** (S-06).
- *Sin estado:* el servidor no guarda sesiones; cada petición trae su token.
- *El rol se lee de la base en cada petición, no del token*: si el coordinador desactiva a un usuario, pierde el acceso al instante (aunque su token no haya vencido).
- *Autorización en dos niveles:* (1) por **rol** con `@PreAuthorize` en cada endpoint; (2) por **relación con los datos** en el servicio (por ejemplo, un guía solo evalúa a los estudiantes que tiene asignados). Lo primero da 403 antes de entrar al servicio; lo segundo da 403 dentro de él.
- El login **no revela** si falló el correo o la contraseña, ni si la cuenta está inactiva (mismo 401 y mismo mensaje), y compara siempre con un hash para tardar lo mismo.

**Conexión con la base.** La aplicación se conecta como **`isw_app`**, un usuario que solo tiene `SELECT, INSERT, UPDATE, DELETE` sobre `isw_practicas`: no puede borrar tablas ni cambiar la estructura.
La clave llega **solo** por la variable de entorno `ISW_DB_PASSWORD` (`${ISW_DB_PASSWORD}` en `application.properties`, sin valor por defecto: si falta, la app no arranca).
`spring.jpa.hibernate.ddl-auto=validate`: Hibernate **verifica** que las entidades coincidan con las tablas existentes, pero nunca las crea ni las altera (el esquema es de `db/schema.sql`).
Las reglas críticas están **también en la base** (CHECK, UNIQUE, FK y triggers), así que aunque alguien saltara la aplicación, los datos no quedan inconsistentes.

---

## 2. Cómo arrancar el proyecto desde cero

Requisitos: JDK 17, Maven 3.9 y MySQL 8 (ya instalados en esta máquina). Detalle completo en `backend/README.md`.

```powershell
# 1) Variables de entorno (una sola vez; abrir terminal nueva después): ISW_DB_ROOT_PASSWORD (root) e ISW_DB_PASSWORD (isw_app)
# 2) Base de datos: crea la base, las 14 tablas, los datos de demo y el usuario isw_app
.\db\bd.ps1
# 3) Compilar y probar (146 pruebas)
cd backend
mvn clean package
# 4) Arrancar
java -jar target\practicas-0.1.0-SNAPSHOT.jar
# 5) Abrir  http://localhost:8080/swagger-ui.html
```

---

## 3. Usuarios de demostración

Todos son ficticios (dominio `@demo.example`) y comparten la clave de demostración que está en el encabezado de `db/seed.sql`.

| Correo | Rol | Datos que tiene |
|---|---|---|
| `coordinador@demo.example` | COORDINADOR | Andrea Fuentes: ve y administra todo |
| `guia1@demo.example` | PROFESOR_GUIA | **Marcos Bravo** (usuario 2): guía de Camila, Valentina y Sebastián |
| `guia2@demo.example` | PROFESOR_GUIA | **Lorena Vidal** (usuario 3): guía de Diego, Valentina y Sebastián |
| `curso1@demo.example` | PROFESOR_CURSO | **Ricardo Núñez** (usuario 4): profesor del curso de Camila y Diego (Trabajo Colaborativo) |
| `curso2@demo.example` | PROFESOR_CURSO | **Patricia Contreras** (usuario 5): profesor del curso de Javiera y de las prácticas finales |
| `camila.rojas@demo.example` | ESTUDIANTE | Estudiante 1. **Inscripción 3** (Trabajo Colaborativo 2026-2). Planificaciones: 3 autorizada, 4 con observaciones, 5 borrador |
| `diego.munoz@demo.example` | ESTUDIANTE | Estudiante 2. Inscripción 6. Planificaciones: 6 en revisión del guía, 7 en autorización |
| `valentina.perez@demo.example` | ESTUDIANTE | Estudiante 3. Cursa Práctica Pedagógica; 2 evaluaciones cuyo promedio **sube** (4,80 → 5,90) |
| `sebastian.vargas@demo.example` | ESTUDIANTE | Estudiante 4. Recorrido completo: 5 prácticas aprobadas y la sexta en curso |
| `javiera.herrera@demo.example` | ESTUDIANTE | Estudiante 5. Recién inicia (Aproximación al Sistema Escolar) |

**Generación completa (Fase E, 22-sep, S-39):** 15 estudiantes más (**20 en total**, `matias.fernandez@demo.example` a `agustin.navarro@demo.example`, misma clave), y 4 profesores guía más (`guia3@demo.example` a `guia6@demo.example`) y 2 profesores del curso más (`curso3@demo.example`, `curso4@demo.example`), para que ningún guía supere los 15-20 estudiantes reportados por un estudiante de PEM. No cambian los usuarios de la tabla de arriba ni sus datos: siguen sirviendo igual para el guion de cada historia.

**Cómo autorizarse en Swagger:** `POST /api/auth/login` → *Try it out* → `{"email":"…","password":"…"}` → copiar el `token` de la respuesta → botón **Authorize** → pegarlo (sin "Bearer") → *Authorize*.
Para cambiar de usuario: *Authorize → Logout*, y repetir con otro correo.

---

## 4. Las 5 historias

### HU-01 — Autenticación con roles

**Qué hace.** Inicio de sesión con correo y contraseña, token JWT de 8 horas y 4 roles (tabla `rol`). Cada endpoint restringe qué rol puede usarlo.

| Método y ruta | Rol permitido |
|---|---|
| `POST /api/auth/login` | público |
| `GET /api/auth/me` | cualquier rol autenticado |
| `GET /api/roles` | COORDINADOR |

**Reglas de negocio.** Contraseñas solo como hash BCrypt (nunca en texto ni en respuestas) · credenciales incorrectas → 401 con el mismo mensaje para correo o clave · usuario inactivo no entra · sin token → 401 · rol sin permiso → 403 · token vencido o manipulado → 401.

**Guion en Swagger (3 min).**
1. `POST /api/auth/login` con `coordinador@demo.example` y la clave de demostración → 200 con `token`, `expiraEnSegundos: 28800` y `usuario.rol: COORDINADOR`. Mostrar que **no aparece ningún hash**.
2. Pegar el token en **Authorize**. `GET /api/auth/me` → 200 con su perfil. `GET /api/roles` → 200 con los 4 roles.
3. *Authorize → Logout*. `GET /api/roles` → **401** ("Debes iniciar sesión…").
4. Entrar como `camila.rojas@demo.example`, autorizar y `GET /api/roles` → **403** ("Tu rol no tiene permiso…"). Es el mismo endpoint: cambia el resultado según el rol.
5. `POST /api/auth/login` con clave incorrecta → 401 "Correo o contraseña incorrectos." Repetir con un correo que no existe → **el mismo** 401 y mensaje.

### HU-02 — Estudiantes e inscripción en asignaturas de práctica

**Qué hace.** El coordinador registra estudiantes y profesores, consulta las 6 asignaturas e inscribe a cada estudiante en una asignatura por periodo, asignándole su profesor guía y su profesor del curso.

| Método y ruta | Rol permitido |
|---|---|
| `POST /api/estudiantes` · `GET /api/estudiantes?buscar=` · `GET /api/estudiantes/{id}` | COORDINADOR |
| `GET /api/estudiantes/me` | ESTUDIANTE |
| `POST /api/profesores` · `GET /api/profesores?rol=` | COORDINADOR |
| `GET /api/asignaturas` · `GET /api/asignaturas/{id}` | cualquier rol |
| `POST /api/inscripciones` | COORDINADOR |
| `GET /api/inscripciones?estudianteId=&asignaturaId=&periodo=` | cualquier rol (cada uno ve solo lo suyo) |
| `GET /api/inscripciones/{id}` | quien tenga relación con esa inscripción |
| `PUT /api/inscripciones/{id}/cierre` | COORDINADOR |

**Reglas de negocio.**
- Correo y RUT únicos → 409. El RUT se valida con su **dígito verificador** (módulo 11) → 400 (S-24).
- Al registrar un estudiante se crean a la vez el usuario (rol ESTUDIANTE, clave BCrypt) y su ficha, en **una transacción**.
- No se inscribe dos veces al mismo estudiante en la misma asignatura y periodo → 409.
- El guía debe tener rol `PROFESOR_GUIA` y el profesor del curso `PROFESOR_CURSO` → 400. El guía es el **colaborador del colegio**: es obligatorio si la asignatura va a un colegio, y **no existe en el ciclo inicial** (Aproximación y Ambientes no van a ningún colegio), donde asignarlo da 400 (S-25, S-34).
- Modalidades (S-16, según un estudiante, pendiente de confirmar): Aproximación al Sistema Escolar es en **grupos de 4** y no entrega planificaciones; Ambientes de Aprendizaje es en **parejas** y sí las entrega. Por eso `GET /api/asignaturas` trae `modalidad` (INDIVIDUAL, DUPLA o GRUPO) y `requierePlanificacion`.
- Buscar ignora mayúsculas y tildes ("sebastian" encuentra "Sebastián": lo hace la colación de MySQL).
- Extra (S-22): `PUT …/cierre` cierra una inscripción con APROBADA/REPROBADA y nota 1,0 a 7,0; así el coordinador carga las asignaturas aprobadas que usará el chatbot (S-14).

**Guion en Swagger (4 min), como `coordinador@demo.example`.**
1. `POST /api/estudiantes`:
   ```json
   {"nombre":"Ana","apellidos":"Soto Reyes","email":"ana.soto@demo.example","password":"clave-inicial-1",
    "rut":"12.345.678-5","semestreActual":5,"anioIngreso":2024}
   ```
   → **201**; el RUT queda como `12345678-5`. Anotar el `id`.
2. Repetir el mismo cuerpo → **409** (correo duplicado). Cambiar el RUT a `12345678-4` → **400** ("dígito verificador"). Poner `semestreActual: 20` → **400** con el detalle por campo en `errores`.
3. `GET /api/estudiantes?buscar=sebastian` → encuentra a *Sebastián* (sin tilde). `GET /api/asignaturas` → 6 asignaturas; `chatbotHabilitado: true` **solo** en las dos últimas; Aproximación es `GRUPO` con `requierePlanificacion: false`.
4. `GET /api/profesores?rol=PROFESOR_GUIA` → ids 2 y 3; `?rol=PROFESOR_CURSO` → ids 4 y 5.
5. `POST /api/inscripciones` con el `id` de Ana:
   ```json
   {"estudianteId":<id de Ana>,"asignaturaId":3,"periodo":"2026-2","profesorGuiaId":2,"profesorCursoId":4}
   ```
   → **201** `EN_CURSO`. Repetirlo → **409**. Cambiar `profesorGuiaId` a 4 → **400** (es profesor del curso, no guía). Quitar `profesorGuiaId` → **400** (Trabajo Colaborativo va a un colegio). Con `asignaturaId: 2` (Ambientes, ciclo inicial) y sin guía → **201**; con guía → **400** (no va a un colegio, no hay colaborador).
6. Autorizar como `guia1@demo.example` y repetir `POST /api/estudiantes` → **403**.

### HU-03 — Evaluar con la pauta digital (indicadores 1 a 7)

**Qué hace.** El profesor guía completa en el sistema la pauta de observación de clase (20 indicadores en 3 dimensiones, escala 1 a 7) que hoy se escribe a mano; el sistema calcula el puntaje total y el promedio.

| Método y ruta | Rol permitido |
|---|---|
| `GET /api/pautas?asignaturaId=` · `GET /api/pautas/{id}` | cualquier rol (S-27) |
| `POST /api/evaluaciones` | PROFESOR_GUIA (solo el asignado a la inscripción) |
| `PUT /api/evaluaciones/{id}` (cabecera y observaciones, solo BORRADOR) | PROFESOR_GUIA (el autor) |
| `PUT /api/evaluaciones/{id}/respuestas` (uno o varios indicadores) | PROFESOR_GUIA (el autor) |
| `POST /api/evaluaciones/{id}/finalizar` | PROFESOR_GUIA (el autor) |
| `GET /api/evaluaciones/{id}` · `GET /api/evaluaciones?inscripcionId=&estado=` | según relación (ver reglas) |

**Reglas de negocio.**
- La **pauta es un dato** (tablas `pauta`, `dimension_pauta`, `indicador`), no código: el rango 1–7 sale de `escala_min/escala_max` de la pauta.
- Puntaje entero dentro de la escala, e indicador de **esa** pauta → si no, 400. Se valida todo antes de guardar nada. Un valor como `4.5` también se rechaza (S-31).
- Flujo de estados: `BORRADOR → FINALIZADA`. **Finalizar exige los 20 indicadores** (400 con los números que faltan), calcula `puntajeTotal` (máx. 140) y `promedio` (total ÷ 20, 2 decimales) y **congela** la evaluación (409 si se intenta modificar). La base lo refuerza con un trigger y un CHECK.
- Solo el guía **asignado a esa inscripción** puede evaluarla; otro guía → 403. La inscripción debe estar EN_CURSO, la pauta ser de su asignatura y la planificación vinculada (opcional) ser de la misma inscripción y estar AUTORIZADA (S-30).
- Un BORRADOR es privado de su autor y del coordinador; el estudiante evaluado y su profesor del curso ven solo las FINALIZADAS (S-29).
- Fuera de alcance: convertir el puntaje a nota oficial (S-08).
- **Ampliado en la Fase E (HU-11, HU-12, HU-22, 22-sep):** el evaluador puede grabar una **nota de voz** (`POST/GET/PUT /api/evaluaciones/{id}/nota-voz`, `GET .../nota-voz/audio`) con su transcripción, y subir el **documento formal** de la pauta (`POST/GET /api/evaluaciones/{id}/documentos`). Ambas se pueden hacer con la evaluación en BORRADOR o ya FINALIZADA (S-41, S-42): son retroalimentación complementaria, no cambian el puntaje.

**Guion en Swagger (5 min), como `guia1@demo.example` (Marcos).**
1. `GET /api/pautas?asignaturaId=3` → la pauta con sus 3 dimensiones y 20 indicadores en orden, escala 1–7. Los ids de los indicadores son 1 a 20.
2. `GET /api/inscripciones` → solo las suyas (4 del seed, más la de Ana si antes hiciste la demo de HU-02); la **3** es Camila en Trabajo Colaborativo 2026-2.
3. `POST /api/evaluaciones`:
   ```json
   {"inscripcionId":3,"pautaId":1,"planificacionId":3,"fechaClase":"2026-09-22","establecimiento":"Liceo Bicentenario Ejemplo",
    "curso":"1° Medio B","horaInicio":"10:15:00","horaTermino":"11:45:00",
    "objetivoClase":"Plantear y resolver ecuaciones de primer grado.","observacionesGenerales":"Fortalezas: buen clima de aula."}
   ```
   → **201** `BORRADOR`. Anotar el `id`.
4. `PUT /api/evaluaciones/{id}/respuestas` con `{"respuestas":[{"indicadorId":1,"puntaje":8}]}` → **400** ("entre 1 y 7"). Con `{"indicadorId":999,"puntaje":5}` → **400** ("no pertenece a la pauta").
5. Mandar solo 3 indicadores (`{"respuestas":[{"indicadorId":1,"puntaje":6},{"indicadorId":2,"puntaje":5},{"indicadorId":3,"puntaje":6}]}`) y `POST …/finalizar` → **400** "faltan 17 de 20 indicadores (números [4, 5, …])".
6. Completar los 20 (esto **reemplaza** los 3 anteriores):
   ```json
   {"respuestas":[{"indicadorId":1,"puntaje":6},{"indicadorId":2,"puntaje":5},{"indicadorId":3,"puntaje":6},{"indicadorId":4,"puntaje":5},
    {"indicadorId":5,"puntaje":4},{"indicadorId":6,"puntaje":5},{"indicadorId":7,"puntaje":6},{"indicadorId":8,"puntaje":6},
    {"indicadorId":9,"puntaje":5},{"indicadorId":10,"puntaje":4},{"indicadorId":11,"puntaje":5},{"indicadorId":12,"puntaje":5},
    {"indicadorId":13,"puntaje":6},{"indicadorId":14,"puntaje":6},{"indicadorId":15,"puntaje":5},{"indicadorId":16,"puntaje":5},
    {"indicadorId":17,"puntaje":7},{"indicadorId":18,"puntaje":5},{"indicadorId":19,"puntaje":6},{"indicadorId":20,"puntaje":5}]}
   ```
   → 200 con `indicadoresRespondidos: 20`. `POST …/finalizar` → **200** `FINALIZADA`, `puntajeTotal: 107` (de 140), `promedio: 5.35`.
7. Intentar de nuevo `PUT …/respuestas` → **409** (finalizada). `POST …/finalizar` otra vez → **409**.
8. Autorizar como `guia2@demo.example` (Lorena) y `POST /api/evaluaciones` sobre la inscripción 3 → **403** (no es su estudiante).
9. Autorizar como `camila.rojas@demo.example` y `GET /api/evaluaciones/{id}` → 200 con el puntaje de cada indicador. (Antes de finalizar habría dado 403: los borradores son del evaluador.)

**Guion de la nota de voz (Fase E, HU-11 y HU-12), en el front-end o en Swagger (3 min), como `guia1@demo.example` (Marcos), sobre la evaluación recién finalizada.**
1. En el front-end: abrir `#/evaluaciones/{id}` y bajar hasta **"Retroalimentación en voz"** (entre la tarjeta de puntaje/promedio y "Indicadores de la pauta"). Pulsar **Grabar nota de voz**, decir un par de frases y pulsar **Detener grabación**: aparece el reproductor y, si el navegador soporta `SpeechRecognition` (Chrome), el cuadro **Transcripción a texto** se va llenando solo. Pulsar **Guardar nota de voz**.
2. Recargar la página: la sección ahora muestra el audio grabado y el texto, con la etiqueta "Transcripción automática, sin corregir". Corregir el texto a mano y pulsar **Guardar transcripción corregida**: la etiqueta cambia a "Transcripción corregida por el evaluador".
3. En Swagger, lo mismo por HTTP: `POST /api/evaluaciones/{id}/nota-voz` (multipart: `audio`, `transcripcion`, `duracionSegundos`) → **201**. Repetirlo → **409** ("ya tiene una nota de voz… usa PUT"). `PUT /api/evaluaciones/{id}/nota-voz` con `{"transcripcion":"texto corregido"}` → **200**, `transcripcionCorregida: true`.
4. Autorizar como `guia2@demo.example` (Lorena, no es la evaluadora de esta clase) e intentar `POST` o `PUT` sobre la misma nota de voz → **403** en ambos.
5. Autorizar como `camila.rojas@demo.example` (o el estudiante evaluado) y `GET /api/evaluaciones/{id}/nota-voz` → **200**: también puede escucharla y leer la transcripción (mismas reglas de acceso que ver la evaluación).

### HU-04 — Planificación de clase con flujo de aprobación

**Qué hace.** Toda clase requiere una planificación escrita con el formato de la carrera. El estudiante la redacta, la presenta a su **profesor guía**, que la retroalimenta; con la mejora la presenta al **profesor del curso**, que autoriza la clase. Cada decisión queda registrada.

| Método y ruta | Rol permitido |
|---|---|
| `POST /api/planificaciones` | ESTUDIANTE (dueño de la inscripción EN_CURSO) |
| `PUT /api/planificaciones/{id}` | ESTUDIANTE (dueño), solo en BORRADOR o CON_OBSERVACIONES |
| `POST /api/planificaciones/{id}/enviar` | ESTUDIANTE (dueño) |
| `POST /api/planificaciones/{id}/revision-guia` | PROFESOR_GUIA (el asignado) |
| `POST /api/planificaciones/{id}/revision-curso` | PROFESOR_CURSO (el asignado) |
| `GET /api/planificaciones/{id}` (con historial) · `GET /api/planificaciones?inscripcionId=&estado=` | según relación con la inscripción |

**Máquina de estados** (una transición fuera de este diagrama responde **409**; el actor equivocado, **403**):

```
BORRADOR ──enviar──▶ EN_REVISION_GUIA ──guía aprueba──▶ EN_AUTORIZACION ──curso autoriza──▶ AUTORIZADA (final)
   ▲                       │                                  │
   │      guía devuelve    │        curso devuelve            │
   └── CON_OBSERVACIONES ◀─┴──────────────────────────────────┘
        (el estudiante corrige y REENVÍA: siempre vuelve al guía, S-10)
```

**Reglas de negocio.**
- Formato de la carrera (S-09): título, fecha, **objetivo, recursos, tiempos** (con duración), **evaluación del aprendizaje** e **interacción de los participantes con los recursos**. Un borrador puede estar incompleto (solo el título es obligatorio); **para enviarlo, todo es obligatorio** (400 con los elementos que faltan). La base lo refuerza con un CHECK.
- Devolver con observaciones **exige comentario** (400 si falta), tanto al guía como al profesor del curso.
- **Sin guía** (Ambientes de Aprendizaje, que no va a un colegio) la planificación va **directo al profesor del curso**: al enviarla queda `EN_AUTORIZACION` y ningún guía puede revisarla (S-34).
- **Aproximación al Sistema Escolar no entrega planificaciones** (solo trabajos en clase): crear una da 409 (S-36).
- Se entrega con **al menos 3 días de anticipación** a la clase ("como mínimo unos 3 días antes", según un estudiante); si no, al enviar da 400. Es configurable: `app.planificacion.dias-anticipacion-minimos` (S-35).
- Una `AUTORIZADA` no se modifica ni se reenvía ni se revisa otra vez (409).
- Cada decisión (guía o curso) genera una fila en `revision_planificacion` con **autor, fecha, etapa, decisión y comentario**, y se lista en orden cronológico dentro del detalle.
- El guía ve todas las planificaciones de sus estudiantes en cualquier estado; el profesor del curso, las de sus inscripciones; el estudiante, las suyas; un estudiante ajeno, nada (403).

**Guion en Swagger (6 min).**
1. Como `camila.rojas@demo.example`: `POST /api/planificaciones` (la `fechaClase` debe ser al menos 3 días después de hoy; con la demo del 25 de septiembre, `2026-10-14` sirve):
   ```json
   {"inscripcionId":3,"titulo":"Sistemas de ecuaciones 2x2","fechaClase":"2026-10-14",
    "objetivo":"Resolver sistemas de ecuaciones lineales de 2x2 por sustitución e igualación.",
    "recursos":"Guía de problemas, pizarra y tarjetas de estrategias.","tiempos":"Inicio 10 min; desarrollo 65 min; cierre 15 min.",
    "duracionMinutos":90,"evaluacionAprendizaje":"Problema de cierre resuelto por cada estudiante.",
    "interaccionParticipantes":"Los estudiantes comparan estrategias en grupos y presentan una al curso."}
   ```
   → **201** `BORRADOR` (`pendienteDe: ESTUDIANTE`). Anotar el `id`. (Para mostrar el 400: crear otra con solo `{"inscripcionId":3,"titulo":"Borrador"}` e intentar `enviar`.)
2. `POST /api/planificaciones/{id}/enviar` → **200** `EN_REVISION_GUIA` (`pendienteDe: PROFESOR_GUIA`). `PUT /api/planificaciones/{id}` ahora → **409** (en revisión no se edita).
3. Como `guia1@demo.example` (Marcos): `GET /api/planificaciones?estado=EN_REVISION_GUIA` (su bandeja). `POST …/revision-guia` con `{"decision":"OBSERVADA"}` → **400** (falta comentario); con `{"decision":"OBSERVADA","comentario":"Incluir el uso de los errores como devolución."}` → **200** `CON_OBSERVACIONES`.
4. Como Camila: `PUT /api/planificaciones/{id}` (mismo cuerpo sin `inscripcionId`, cambiando `evaluacionAprendizaje`) → 200; `POST …/enviar` → `EN_REVISION_GUIA` otra vez.
5. Como Marcos: `POST …/revision-guia` con `{"decision":"APROBADA","comentario":"Ahora sí."}` → **200** `EN_AUTORIZACION` (`pendienteDe: PROFESOR_CURSO`).
6. Como `curso2@demo.example` (Patricia): `POST …/revision-curso` `{"decision":"APROBADA"}` → **403** (no es su alumna). Como `curso1@demo.example` (Ricardo) → **200** `AUTORIZADA`.
7. Como Camila: `PUT /api/planificaciones/{id}` → **409** (autorizada no se modifica). `GET /api/planificaciones/{id}` (o como Marcos) → `revisiones` en orden: guía observa → guía aprueba → curso autoriza, con autor, fecha y comentario.
8. *(Opcional, regla nueva)* Sin guía: como coordinador, inscribe a Javiera (`estudianteId` 5) en `asignaturaId` 2 (Ambientes), periodo `2026-2`, **sin** `profesorGuiaId` y con `profesorCursoId` 5. Como `javiera.herrera@demo.example`, crea una planificación completa en esa inscripción y `enviar` → **`EN_AUTORIZACION`** directo. `curso2@demo.example` (Patricia) la autoriza; un guía recibe 403. Con `fechaClase` a 2 días de hoy, `enviar` da **400** (3 días de anticipación). Y en la inscripción 18 (Aproximación) crear una planificación da **409**.
9. *(Opcional, con datos del seed)* Como `guia2@demo.example` (Lorena): la planificación **6** de Diego está en su bandeja → aprobar. Como `curso1@demo.example`: la **7** ya espera su autorización → autorizar.

### HU-05 — Historial (recorrido) del estudiante

**Qué hace.** Devuelve el recorrido completo de un estudiante por sus prácticas para ver su **evolución en el tiempo** y no solo una nota final (criterio 3 de acreditación). Es solo lectura: junta lo que ya existe.

| Método y ruta | Rol permitido |
|---|---|
| `GET /api/estudiantes/{id}/historial` | el propio estudiante, su guía o profesor del curso asignados, y el coordinador (si no, 403) |
| `GET /api/estudiantes/me/historial` | ESTUDIANTE (su propio historial, sin conocer su id) |
| `GET /api/evaluaciones/{id}` (detalle indicador por indicador) | ver HU-03 |

**Reglas de negocio.**
- Inscripciones en **orden cronológico** (periodo y luego orden de la práctica) con asignatura, ciclo, periodo, estado y nota final.
- Cada inscripción trae sus evaluaciones **finalizadas** (fecha, pauta, puntaje total, promedio) y **todas** sus planificaciones con su estado actual.
- Incluye el **semestre actual** y la lista de **asignaturas aprobadas**: es el contexto que necesitará el chatbot en un sprint posterior (S-14).
- Un profesor guía o del curso ve el historial **completo** de un estudiante si está asignado a él en **alguna** inscripción, para poder ver la evolución (S-21); si no lo está, 403.
- Eficiencia: se arma con 4 consultas (no una por inscripción).

**Guion en Swagger (4 min).**
1. Como `coordinador@demo.example`: `GET /api/estudiantes/4/historial` (Sebastián) → **6 inscripciones** de 2023-2 a 2026-2 (5 aprobadas y la última en curso), `semestreActual: 10`, `asignaturasAprobadas` con 5. En Trabajo Colaborativo 2024-2 se ve su evaluación (85 puntos, promedio 4,25) y su planificación autorizada.
2. `GET /api/estudiantes/3/historial` (Valentina) → en Trabajo Colaborativo 2025-2 hay **2 evaluaciones: promedio 4,80 y luego 5,90** (la evolución que busca el cliente).
3. `GET /api/evaluaciones/3` → el detalle: cada uno de los 20 indicadores con su dimensión, descripción y puntaje.
4. Como `camila.rojas@demo.example`: `GET /api/estudiantes/me/historial` → 200 (3 inscripciones). `GET /api/estudiantes/2/historial` (Diego) → **403**.
5. Como `guia1@demo.example` (Marcos): `/api/estudiantes/4/historial` → 200; `/api/estudiantes/2/historial` → **403** (Diego es alumno de Lorena).
6. Como `curso2@demo.example`: `/api/estudiantes/5/historial` (Javiera) → 200 (es su alumna); como `curso1@demo.example` → 403.

---

## 5. Preguntas probables de defensa

**¿Por qué JWT y no sesiones?** El servidor no guarda estado: cada petición trae su token firmado, así la API escala y sirve igual a Swagger, a Postman o a un front-end estático. El costo es que un token no se puede "revocar" antes de vencer; por eso el filtro consulta al usuario en la base en cada petición y rechaza a los desactivados.

**¿Cómo se guardan las contraseñas?** Solo como hash **BCrypt** (con sal incorporada, costo 10). Nunca en texto plano, ni en logs, ni en respuestas. La base lo exige además con un CHECK (`ck_usuario_password_bcrypt`).

**¿Qué diferencia hay entre 401 y 403?** 401: no sabemos quién eres (sin token, token inválido o vencido). 403: sabemos quién eres pero tu rol, o tu relación con ese dato, no permite la operación.

**¿Por qué el rol se lee de la base y no del token?** Para que un cambio de rol o una desactivación surta efecto de inmediato. Cuesta una consulta por petición, aceptable a este volumen.

**¿Cómo evitan la inyección SQL?** Todas las consultas son de Spring Data JPA con parámetros enlazados (`:parametro`); nunca se concatena texto del usuario en SQL.

**¿Por qué usan DTO y no devuelven las entidades?** Para no exponer datos internos (el hash de la contraseña), evitar ciclos y cargas perezosas al serializar, y poder cambiar el modelo sin romper la API.

**¿Por qué `ddl-auto=validate` y por qué el usuario `isw_app`?** El esquema lo define `db/schema.sql` (con sus CHECK, FK y triggers) y la aplicación solo lo verifica. `isw_app` tiene el mínimo privilegio: no puede borrar tablas ni cambiar la estructura; si alguien comprometiera la aplicación, el daño está acotado. `root` no se usa en la aplicación.

**¿Dónde están las claves?** Solo en variables de entorno de Windows (`ISW_DB_PASSWORD`, opcional `ISW_JWT_SECRET`). En el código y en `application.properties` solo hay `${ISW_DB_PASSWORD}`. La clave de los usuarios de demostración solo existe para desarrollo y no es valor por defecto de nada.

**¿Cómo se valida que el puntaje esté entre 1 y 7?** En tres capas: Bean Validation (formato), el servicio (rango leído de la pauta y pertenencia del indicador → 400 con mensaje claro) y un trigger de MySQL (`sp_validar_respuesta_indicador`). Si mañana hay una pauta de 2 a 7, no se cambia código: cambia el dato `escala_min`.

**¿Por qué una evaluación finalizada no se puede modificar?** Es evidencia para la acreditación (criterio 3): debe ser trazable e inalterable. El servicio responde 409 y la base impide cambiar sus puntajes (trigger).

**¿Cómo se calcula el puntaje?** Suma de los 20 indicadores (máximo 140) y promedio = total ÷ 20 con 2 decimales (redondeo hacia arriba desde 5). Se guardan al finalizar para "congelar" el resultado. La conversión a nota oficial queda pendiente (S-08).

**Explique el flujo de la planificación.** Es una máquina de estados con 5 estados y 3 actores (ver el diagrama en HU-04). Cada transición valida quién actúa (403) y desde qué estado (409), y las decisiones del guía y del profesor del curso quedan como filas de `revision_planificacion` (autor, fecha, etapa, comentario): nada se sobrescribe. Si el profesor del curso devuelve, el estudiante corrige y el flujo **vuelve al guía** (S-10).

**¿Qué pasa si el profesor del curso "se salta" al guía?** No puede: `revision-curso` solo acepta planificaciones en `EN_AUTORIZACION` (409 en cualquier otro estado).

**¿Por qué `ESTUDIANTE` está separado de `USUARIO` y existe `INSCRIPCION`?** Porque solo los estudiantes tienen RUT, semestre y año de ingreso, y porque la relación estudiante–asignatura tiene datos propios (periodo, guía, profesor del curso, estado, nota). Toda la actividad cuelga de la inscripción; por eso el historial es "las inscripciones ordenadas en el tiempo".

**¿Cómo evitan consultas repetidas (N+1)?** Las relaciones son perezosas, los servicios son `@Transactional` y `hibernate.default_batch_fetch_size=32` agrupa las cargas. El historial usa 4 consultas fijas sin importar cuántas inscripciones tenga.

**¿Cómo lo probaron?** 146 pruebas automáticas: unitarias con Mockito (reglas de cada servicio, sin base) y de integración (una clase por historia) que levantan la aplicación completa contra la MySQL real y verifican también 401/403. Corren dentro de una transacción que se revierte, así no ensucian la base. Además se probó cada historia por HTTP real contra la base.

**¿Qué pasa si dos guías o dos usuarios modifican lo mismo a la vez?** No hay control de concurrencia optimista (`@Version`) en el Sprint 1; las reglas de estado se validan al momento de cada petición y la base impide los estados inválidos. Es una mejora identificada para un sprint posterior.

**¿Qué pasa en el ciclo inicial, que no va a un colegio?** No existe el profesor colaborador (guía). Según un estudiante, Aproximación se hace en grupos de 4 y no entrega planificaciones, y Ambientes se hace en parejas y sí las entrega. Por eso una inscripción sin guía es válida solo en esas asignaturas, y su planificación va directo al profesor del curso. Esto lo cambiamos **después** de las respuestas del estudiante (migración `db/migraciones/001`): es un buen ejemplo de que el modelo se adapta al cliente. Sigue "pendiente de confirmar".

**¿De dónde salieron la regla de los 3 días y el resto?** De las respuestas de un estudiante de PEM (2026-09-20): "se debe entregar como mínimo unos 3 días antes". Es una regla configurable, no fija, y está marcada como supuesto.

**¿El profesor del curso y el guía revisan a la vez o uno después del otro?** La entrevista con el cliente dice en secuencia (guía y luego profesor del curso) y eso implementamos. Un estudiante contó que la planificación se manda "al profe del ramo y al colaborador". Es una diferencia **sin resolver** (S-37): si el cliente confirma el envío en paralelo, cambiaría la máquina de estados de HU-04. Está anotada como riesgo.

**¿Quién puede leer los chats del chatbot?** Es un tema de sprints posteriores (HU-16 y HU-20), pero hay que estar preparados. El jefe de carrera pidió verlos para llevar registro y saber cómo aportó el chatbot; un estudiante contó que le incomodaría que su guía lea todo lo que conversa. Propuesta: aviso visible al estudiante, acceso del jefe de carrera de solo lectura, y decidir con el cliente si el guía ve el chat completo, un resumen o solo lo que el estudiante comparta (S-38). En el modelo, los mensajes ya se guardan por inscripción (`MENSAJE_CHATBOT`).

**¿De dónde salen el semestre y los ramos aprobados?** Hoy la carrera los ve en la malla de la Intranet (dato de un estudiante). En el Sprint 1 los guarda el coordinador en esta base (S-14); importar desde la Intranet es una mejora futura.

**¿Por qué no hay interfaz gráfica?** Supuesto S-01: el Sprint 1 entrega backend y base de datos, demostrados con Swagger. El CORS ya está habilitado para `http://localhost:*` para cuando llegue un front-end estático.

**¿Por qué el guía ve el historial completo del estudiante y no solo su práctica?** Decisión S-21: el objetivo es ver la evolución y eso exige el recorrido completo. Si el cliente prefiere lo contrario, se cambia en un solo método (`AccesoService.puedeVerHistorial`).

**¿Cómo se agregarían las otras pautas (Práctica Pedagógica y Profesional, tutor)?** Como datos nuevos en `pauta`, `dimension_pauta` e `indicador` (HU-06 y HU-07), sin rehacer el modelo. Falta enseñar al servicio la regla evaluador–tipo de pauta para el tutor (S-12).

**¿Qué NO está en este sprint?** El chatbot (S-02, sprint 4), otras pautas y tutor (S-12), duplas (S-11, HU-10), informes y nota final ponderada (HU-08 y HU-09), notificaciones. La **nota de voz sí está** (se adelantó del sprint 3 el 22-sep, HU-11 y HU-12). Tampoco hay: límite de intentos de login, cambio o recuperación de contraseña (HU-26), refresco de token, paginación de listados (S-26) ni control de concurrencia.

---

## 6. Supuestos que conviene nombrar

Los S-01 a S-16 y S-34 a S-46 están en `Sprint1/sprint-backlog.md`. Los que se sumaron al programar están en `backend/PROGRESO.md` (S-17 a S-46). Un estudiante de PEM respondió varias preguntas el 2026-09-20 (registradas en `RESUMEN.md`, sección 13): son **orientativas, no del cliente**. Los que más conviene confirmar con el cliente:

| Supuesto | Qué se asumió |
|---|---|
| S-05 | El "profesor del curso" que autoriza es el académico UBB (es un rol propio, así que si fuera del colegio no cambia la estructura) |
| S-09 | **Resuelto el 22-sep:** el formato oficial ya se conoce (`.docx` entregado por la carrera); el Sprint 1 sigue con los 5 elementos de la entrevista, migrar `planificacion` queda para un sprint futuro (S-45) |
| S-10 | Si el profesor del curso devuelve, se repite el flujo desde el guía |
| S-21 | El guía y el profesor del curso ven el historial completo del estudiante asignado |
| S-22 | El coordinador cierra las inscripciones con nota (endpoint extra) |
| S-16 | Aproximación en grupos de 4 sin planificaciones; Ambientes en parejas con planificaciones; ninguna va a un colegio (sin guía) |
| S-37 | El flujo de planificación es en secuencia (guía → profesor del curso) aunque un estudiante habló de enviarla a ambos |
| S-38 | El jefe de carrera ve los chats (rol COORDINADOR); el acceso del guía al chat está por decidir por privacidad |
| S-29 | Los borradores de evaluación son privados del evaluador |
| S-40 | La transcripción de la nota de voz la genera el navegador del evaluador (Web Speech API de Chrome), sin costo y sin instalar nada; nadie ha confirmado con el cliente qué herramienta usar |
| S-42 | La nota de voz se puede grabar con la evaluación en BORRADOR o FINALIZADA: es retroalimentación complementaria, no cambia el puntaje |

---

## 7. Cifras para mencionar

- 5 historias del Sprint 1 (26 puntos) más 6 adelantadas el 22-sep (HU-11, HU-12, HU-21 a HU-24) · **42 endpoints** documentados en Swagger · 4 roles.
- 15 entidades JPA con API sobre **16 tablas** (24 FK, 34 CHECK, 13 UNIQUE, 2 triggers); solo `MENSAJE_CHATBOT` (chatbot) queda sin API.
- **159 pruebas automáticas** (146 del Sprint 1 + 13 de la Fase E) + prueba HTTP real de cada historia del Sprint 1 (32 + 30 + 24 + 27 + 18 comprobaciones), el ensayo completo del guion (59), las reglas del ciclo inicial y de los 3 días (11), y las comprobaciones por `curl` de documentos y nota de voz del 22-sep.
- **20 estudiantes** de demostración (antes 5), 6 profesores guía y 4 profesores del curso, ningún guía con más de 5 estudiantes asignados.
- Product Backlog con **30 historias de usuario** (20 originales + 6 adelantadas + 4 nuevas para sprints futuros).
- Pauta real de 20 indicadores en 3 dimensiones (fuente: pauta de observación de Trabajo Colaborativo 2026).

---

## 8. Demo con el front-end

El front-end es HTML + CSS + JavaScript puro (sin frameworks ni librerías externas). Vive en `backend/src/main/resources/static/` y lo entrega la misma aplicación Spring Boot, así que **no hay CORS**: todo corre en <http://localhost:8080>. Habla con la API con `fetch`, guarda el token JWT en `sessionStorage` (solo esa pestaña; se borra al cerrar sesión) y, si la API responde 401, vuelve al login.

**Preparación:** `.\db\bd.ps1` → `java -jar target\practicas-0.1.0-SNAPSHOT.jar` → abrir <http://localhost:8080>. La clave de demostración de los usuarios está en el encabezado de `db/seed.sql`. Para cambiar de usuario: *Cerrar sesión* y volver a ingresar (o una ventana de incógnito por rol, así se ven dos roles a la vez).

**Qué mostrar de la pantalla:** el escudo queda fijo en la esquina superior izquierda en todas las vistas; la barra lateral cambia según el rol y se contrae con el botón de las tres líneas. Desde la Fase E (22-sep) el menú **ya no menciona sprints ni historias de usuario**: solo aparecen los nombres de las funciones a las que el rol tiene acceso, y bajo ellas una sección **"Accesos rápidos"** con los mismos atajos que hoy están en Inicio (útil para saltar de una función a otra sin volver primero a `#/inicio`). El inicio (`#/inicio`) trae los accesos rápidos del rol y la foto del campus.

### Guion por historia (usuario y rol)

| HU | Usuario (rol) | Qué hacer y qué debe pasar |
|---|---|---|
| **HU-01** Iniciar sesión con roles | Ingresar con una clave mala; luego `coordinador@demo.example` (COORDINADOR) | Clave mala → "Correo o contraseña incorrectos." Entrando, el menú muestra las funciones del coordinador. En **Mi cuenta** se ven sus datos, cuándo vence la sesión y la tabla de roles. Luego `camila.rojas@demo.example` (ESTUDIANTE): escribir a mano `#/estudiantes` en la barra de direcciones → "No tienes permiso para esta acción." (el servidor también responde 403) |
| **HU-02** Registrar estudiantes e inscribirlos | `coordinador@demo.example` (COORDINADOR) | **Estudiantes → Registrar estudiante** con datos ficticios (RUT válido, por ejemplo `12345678-5`); con un RUT mal escrito sale el error del servidor dentro del formulario. **Profesores** → registrar uno. **Inscripciones → Nueva inscripción**: al elegir *Aproximación al Sistema Escolar* el profesor guía se deshabilita ("no lleva guía"); con *Trabajo Colaborativo* es obligatorio. Repetir la misma inscripción → 409 con el mensaje del servidor. **Cerrar** una inscripción EN_CURSO con resultado y nota |
| **HU-04** Planificar y aprobar en dos etapas | 1) `camila.rojas@demo.example` (ESTUDIANTE) · 2) `guia2@demo.example` (PROFESOR_GUIA) · 3) `curso1@demo.example` (PROFESOR_CURSO) | 1) En **Planificar una clase…** abrir el borrador *Funciones lineales*, **Enviar a revisión** → error: faltan elementos. Editarlo, completar los cinco elementos y enviarlo. La planificación *Proporcionalidad directa* está *Con observaciones*: se ve el comentario del guía, se edita y se reenvía. 2) Lorena Vidal ve las que le esperan ("Requiere tu acción"), abre la de Diego, intenta **Devolver con observaciones** sin comentario (no deja) y luego aprueba. 3) Ricardo Núñez autoriza la de Diego (*Fracciones equivalentes*, ya en autorización). El estado del flujo y el historial de revisiones se actualizan en la misma pantalla |
| **HU-21** Documentos en la planificación (Fase E) | `camila.rojas@demo.example` (ESTUDIANTE) y después `guia1@demo.example` | Camila abre cualquiera de sus planificaciones y baja hasta **"Documentos"**: sube un PDF con el botón **Subir documento** (aparece en la lista con su nombre, tamaño y fecha). Probar con un `.exe` → error de extensión no permitida. Como Marcos Bravo (su guía), abrir la misma planificación y **descargar** el documento que subió Camila |
| **HU-22** Documento formal de la evaluación (Fase E) | `guia1@demo.example` (PROFESOR_GUIA) | En el detalle de una evaluación (propia), bajar hasta **"Documentos"** (al final de la pantalla) y subir un `.docx` o `.xlsx` con la pauta firmada. Como `guia2@demo.example` (Lorena, no es la evaluadora) intentar entrar a esa misma evaluación → sin acceso |
| **HU-03 + HU-11/HU-12** Evaluar con la pauta digital y grabar la nota de voz | `guia1@demo.example` (PROFESOR_GUIA) y después `camila.rojas@demo.example` (ESTUDIANTE) | Marcos Bravo: **Evaluar una clase… → Nueva evaluación** para Camila (inscripción de Trabajo Colaborativo); la pauta se elige sola. Completar datos de la clase, marcar los 20 indicadores (el contador y el puntaje parcial se actualizan), **Guardar avance**, intentar **Finalizar** con indicadores sin responder (quedan resaltados) y luego finalizar: aparece el promedio y ya no se puede editar. Justo debajo del promedio, en **"Retroalimentación en voz"**, grabar una nota de voz corta y mostrar cómo se llena sola la **"Transcripción a texto"** (si el navegador lo permite); corregirla y **Guardar transcripción corregida**. Camila ve la evaluación finalizada, con el audio y la transcripción, pero nunca el borrador. La evaluación de Camila queda creada: para repetir el guion, `.\db\bd.ps1` |
| **HU-05 + HU-23** Historial del estudiante y buscarlo | `sebastian.vargas@demo.example` (ESTUDIANTE), `guia1@demo.example` y `coordinador@demo.example` | Sebastián ve **su** recorrido (5 prácticas aprobadas y la sexta en curso). El coordinador entra a **Historial del estudiante** y escribe en el buscador de arriba (por ejemplo "valentina", "33333333" o parte del correo): la lista se filtra al instante, sin distinguir mayúsculas ni tildes. Elige a Valentina Pérez: sus dos evaluaciones muestran que el promedio **sube** de 4,80 a 5,90. El guía Marcos Bravo elige a uno de sus estudiantes; probar `#/historial/5` (Javiera, que no es suya) → "No tienes permiso…" |

### Qué decir si preguntan por el front-end

- **¿Por qué sin frameworks?** El sprint era de backend; el front es una capa fina y verificable que muestra las 5 historias. Menos dependencias, nada que instalar y se explica línea por línea.
- **¿Es seguro guardar el token en el navegador?** Se guarda en `sessionStorage` (no sobrevive al cerrar la pestaña) y dura 8 horas. Toda la seguridad real está en el servidor: aunque alguien cambie el menú o la URL, cada endpoint valida el rol (403). El menú por rol es solo comodidad.
- **¿Y el XSS?** Todo texto que viene de la API se escapa antes de insertarse en la página (plantilla `html` de `js/ui.js`).
- **¿Qué pasa si vence la sesión?** Cualquier 401 borra la sesión y vuelve al login con un aviso.
- **¿Qué archivos de Spring Security se tocaron?** Solo `SecurityConfig`: se permiten `/`, `/index.html`, `/app.html`, `/css/**`, `/js/**`, `/img/**` y `/favicon.ico`. Esos archivos no tienen datos; `/api/**` sigue exigiendo token (salvo el login).
- **Rutas del front:** `#/inicio`, `#/cuenta`, `#/estudiantes` (con `#/profesores` e `#/inscripciones`), `#/evaluaciones`, `#/planificaciones` y `#/historial`.
- **¿Por qué el menú ya no dice "Sprint 1" ni "HU-03"?** Porque el sistema se presenta terminado: al usuario no le importa en qué sprint se construyó cada función, solo qué puede hacer con ella.
- **¿Dónde se guardan los documentos y el audio de la nota de voz?** En disco, en `backend/adjuntos/` (fuera de git); la base de datos solo guarda la ruta, el nombre, el tipo y quién lo subió. Un `<a href>` o un `<audio src>` normal no pueden llevar el token, así que el front-end descarga el archivo con `fetch` (con el `Authorization`) y arma un enlace o un audio temporal en el navegador.
- **¿Qué pasa si el navegador no graba audio o no transcribe?** La sección avisa con un mensaje claro y el resto de la pantalla de evaluación sigue funcionando igual (Chrome de escritorio es el navegador de referencia para probar esto).
- **Nota de esta sesión:** la Fase E (documentos, nota de voz, menú y búsqueda) se probó por HTTP con `curl` porque no había un navegador disponible; falta el recorrido visual en Chrome antes de la defensa.

