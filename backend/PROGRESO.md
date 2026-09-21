# PROGRESO — Fase C (backend Spring Boot)

> Se actualiza después de cada historia. Si retomas el trabajo, lee esto primero.
> **Última actualización: 2026-09-20 — las 5 historias del Sprint 1 están terminadas y probadas.**

## Estado

| Historia | Estado | Pruebas automáticas | Prueba real (HTTP + MySQL) |
|---|---|---|---|
| Esqueleto (pom, config, seguridad, entidades, errores) | TERMINADO | — | arranca en :8080, `ddl-auto=validate` OK |
| HU-01 Autenticación con roles | TERMINADA | 21 (11 integración + 6 JwtService + 4 AuthService) | 32 de 32 OK |
| HU-02 Estudiantes e inscripción | TERMINADA | 49 (20 integración + 15 Rut + 9 Inscripción + 3 Estudiante + 2 Usuario) | 30 de 30 OK |
| HU-03 Evaluar con la pauta digital | TERMINADA | 24 (11 integración + 13 servicio) | 24 de 24 OK |
| HU-04 Planificación con flujo de aprobación | TERMINADA | 32 (13 integración + 19 servicio) | 27 de 27 OK |
| HU-05 Historial del estudiante | TERMINADA | 15 (10 integración + 5 servicio) | 18 de 18 OK |
| Transversal: control de acceso (`AccesoService`) | TERMINADO | 5 | — |
| Guía de defensa (`docs/GUIA_DEFENSA.md`) y `backend/README.md` | TERMINADAS | — | guion completo ensayado por HTTP: 59 de 59 OK |

**Total: 146 pruebas automáticas, 0 fallos** (`mvn clean package`). Más 131 comprobaciones por HTTP real contra MySQL, el ensayo del guion de defensa (59) y 11 comprobaciones de las reglas nuevas del 2026-09-20.

**Cambios del 2026-09-20 por las respuestas de un estudiante** (ver `RESUMEN.md` §13): modalidad `GRUPO`, `requiere_planificacion`, sin guía en el ciclo inicial, planificación sin guía directo al profesor del curso, 3 días de anticipación. Detalle en S-34 a S-38 y en "Migraciones".

**Siguiente:** nada pendiente del Sprint 1. Ver "Pendientes y mejoras" abajo. Antes de la defensa: ejecutar `.\db\bd.ps1` (datos limpios) y arrancar el jar.

## Endpoints creados (32 operaciones; todas visibles en Swagger)

| HU | Método y ruta | Rol permitido |
|---|---|---|
| HU-01 | `POST /api/auth/login` | público |
| HU-01 | `GET /api/auth/me` | cualquier rol autenticado |
| HU-01 | `GET /api/roles` | COORDINADOR |
| HU-02 | `POST /api/estudiantes` | COORDINADOR |
| HU-02 | `GET /api/estudiantes?buscar=` | COORDINADOR |
| HU-02 | `GET /api/estudiantes/{id}` | COORDINADOR |
| HU-02 | `GET /api/estudiantes/me` | ESTUDIANTE |
| HU-02 | `POST /api/profesores` | COORDINADOR |
| HU-02 | `GET /api/profesores?rol=` | COORDINADOR |
| HU-02 | `GET /api/asignaturas`, `GET /api/asignaturas/{id}` | cualquier rol autenticado |
| HU-02 | `POST /api/inscripciones` | COORDINADOR |
| HU-02 | `GET /api/inscripciones?estudianteId=&asignaturaId=&periodo=` | cualquier rol (cada uno ve solo lo suyo) |
| HU-02 | `GET /api/inscripciones/{id}` | cualquier rol con relación con la inscripción (si no, 403) |
| HU-02 | `PUT /api/inscripciones/{id}/cierre` | COORDINADOR |
| HU-03 | `GET /api/pautas?asignaturaId=`, `GET /api/pautas/{id}` | cualquier rol autenticado |
| HU-03 | `POST /api/evaluaciones` | PROFESOR_GUIA (solo el asignado a la inscripción) |
| HU-03 | `PUT /api/evaluaciones/{id}` (cabecera y observaciones, solo BORRADOR) | PROFESOR_GUIA (el autor) |
| HU-03 | `PUT /api/evaluaciones/{id}/respuestas` (puntajes 1 a 7, uno o varios indicadores) | PROFESOR_GUIA (el autor) |
| HU-03 | `POST /api/evaluaciones/{id}/finalizar` (calcula total y promedio) | PROFESOR_GUIA (el autor) |
| HU-03 | `GET /api/evaluaciones/{id}` (detalle con cada indicador) | quien tenga relación con la inscripción; borrador solo autor y coordinador |
| HU-03 | `GET /api/evaluaciones?inscripcionId=&estado=` | cualquier rol (cada uno ve solo lo suyo) |
| HU-04 | `POST /api/planificaciones` (queda BORRADOR) | ESTUDIANTE (dueño de la inscripción EN_CURSO) |
| HU-04 | `PUT /api/planificaciones/{id}` (solo BORRADOR o CON_OBSERVACIONES) | ESTUDIANTE (dueño) |
| HU-04 | `POST /api/planificaciones/{id}/enviar` (elementos completos obligatorios) | ESTUDIANTE (dueño) |
| HU-04 | `POST /api/planificaciones/{id}/revision-guia` (APROBADA u OBSERVADA + comentario) | PROFESOR_GUIA (el asignado) |
| HU-04 | `POST /api/planificaciones/{id}/revision-curso` (APROBADA u OBSERVADA + comentario) | PROFESOR_CURSO (el asignado) |
| HU-04 | `GET /api/planificaciones/{id}` (con historial de revisiones) | quien tenga relación con la inscripción |
| HU-04 | `GET /api/planificaciones?inscripcionId=&estado=` | cualquier rol (cada uno ve solo lo suyo) |
| HU-05 | `GET /api/estudiantes/{id}/historial` | el propio estudiante, su guía o profesor del curso asignados, coordinador |
| HU-05 | `GET /api/estudiantes/me/historial` | ESTUDIANTE |

## Decisiones técnicas (para la defensa)

- Paquete base `cl.ubb.pem.practicas`; capas `controller, service, repository, entity, dto, security, exception, config`.
- El filtro JWT consulta al usuario en la base en cada petición (un usuario desactivado pierde el acceso al instante) y toma el rol de la base, no del token.
- `JwtAuthenticationFilter` NO es `@Component` (Spring Boot lo registraría también como filtro de servlet y correría dos veces); lo crea `SecurityConfig`.
- Los enums se guardan como texto con `@Enumerated(STRING)` + `@JdbcTypeCode(VARCHAR)`: sin esto Hibernate 6 espera una columna `enum` de MySQL y `validate` falla contra el esquema real (VARCHAR + CHECK).
- Las columnas `TEXT` llevan `columnDefinition = "text"` para que `validate` las acepte.
- Se excluyó `UserDetailsServiceAutoConfiguration` (evita la clave aleatoria "generated security password" en el log; no se usa).
- `hibernate.default_batch_fetch_size=32` y `open-in-view=false`: los DTO se arman dentro de servicios `@Transactional`; el historial usa 4 consultas fijas.
- `AccesoService` centraliza "quién ve qué" y falla hacia el lado seguro: un rol desconocido no ve nada.
- Las pruebas de integración corren contra la MySQL real con `@Transactional` (rollback): no ensucian los datos de demo. Los tokens de prueba se generan con `JwtService`, sin usar la clave de demostración; las pruebas de login crean usuarios con claves aleatorias.
- Jackson con `accept-float-as-int=false` (un puntaje `4.5` no se trunca a 4: se rechaza).
- Mensajes de validación en español en `ValidationMessages.properties` y errores JSON uniformes (`ApiError`).

## SUPUESTOS (se suman a S-01..S-16 de `Sprint1/sprint-backlog.md`; ninguno está confirmado)

| ID | Supuesto | Historia |
|---|---|---|
| S-17 | El secreto de firma del JWT viene de la variable de entorno opcional `ISW_JWT_SECRET` (mínimo 32 caracteres). Si no existe se genera uno aleatorio al arrancar: los tokens dejan de valer al reiniciar. No hay valor por defecto en el código | HU-01 |
| S-18 | Un usuario inactivo recibe 401 con el mismo mensaje genérico que una clave incorrecta (no se revela que la cuenta existe) | HU-01 |
| S-19 | Se agrega `GET /api/auth/me` (no estaba en el backlog) para que la demo muestre quién es el usuario del token | HU-01 |
| S-20 | `GET /api/roles` (solo coordinador) se agrega también para poder demostrar 403 con los demás roles desde el primer día | HU-01 |
| S-21 | Un profesor guía o del curso ve el historial COMPLETO de un estudiante si está asignado a él en al menos una inscripción (no solo la práctica que dirige), porque el fin es ver la evolución en el tiempo. Si se prefiere más estricto, se cambia solo `AccesoService.puedeVerHistorial` | HU-05 |
| S-22 | Se agrega `PUT /api/inscripciones/{id}/cierre` (no estaba en el backlog): el coordinador cierra una inscripción con APROBADA/REPROBADA y nota 1,0 a 7,0, para cargar las asignaturas aprobadas (S-14). No se valida que la nota sea coherente con el estado (la nota mínima de aprobación no está confirmada) | HU-02 |
| S-23 | El coordinador define la contraseña inicial de cada estudiante y profesor al registrarlo (8 a 72 caracteres). No hay flujo de recuperación ni cambio de clave en el Sprint 1 | HU-02 |
| S-24 | El RUT se valida con su dígito verificador (módulo 11), se normaliza (sin puntos, con guion, K mayúscula) y se guarda así | HU-02 |
| S-25 | El guía es obligatorio cuando la asignatura tiene `requiere_centro_educativo = true`. Solo pueden asignarse profesores activos. *(La parte "en el ciclo inicial es opcional" fue reemplazada por S-34: en el ciclo inicial no existe guía)* | HU-02 |
| S-26 | Los listados no tienen paginación (los volúmenes del Sprint 1 son pequeños) | HU-02 |
| S-27 | Cualquier rol autenticado puede consultar las pautas (el estudiante también, para saber con qué criterios lo evalúan) | HU-03 |
| S-28 | La fecha de la clase evaluada no se restringe (puede ser futura o pasada) | HU-03 |
| S-29 | Una evaluación en BORRADOR es privada de su autor y del coordinador; el estudiante y el profesor del curso ven solo las FINALIZADAS | HU-03 |
| S-30 | Solo se puede evaluar una inscripción EN_CURSO; la pauta debe ser de la asignatura de la inscripción y estar activa; el evaluador debe corresponder al tipo de la pauta (COLABORADOR = PROFESOR_GUIA); la planificación vinculada es opcional pero debe ser de la misma inscripción y estar AUTORIZADA | HU-03 |
| S-31 | El puntaje debe ser un entero. El promedio es total ÷ cantidad de indicadores, con 2 decimales (HALF_UP) | HU-03 |
| S-32 | El estudiante, su guía, su profesor del curso y el coordinador ven la planificación en cualquier estado (incluido BORRADOR). El envío y el reenvío del estudiante no generan fila en `revision_planificacion` (solo las decisiones del guía y del curso, como dice el MER) | HU-04 |
| S-33 | *(La parte "sin guía, 409" fue reemplazada por S-34: sin guía la planificación va directo al profesor del curso.)* Los textos en blanco se guardan como vacíos (NULL). No se restringe que la fecha de la clase sea futura | HU-04 |

| S-34 | **Cambia S-25.** Donde no se va a un colegio (ciclo inicial) no existe profesor guía: asignarlo da 400. Si la inscripción no tiene guía, al enviar la planificación pasa **directo a `EN_AUTORIZACION`** (la autoriza el profesor del curso) y ningún guía puede revisarla (403). Reemplaza la regla anterior (S-33: 409 si no había guía) | HU-02, HU-04 |
| S-35 | La planificación se envía con al menos 3 días de anticipación a la clase (`app.planificacion.dias-anticipacion-minimos=3`; con 0 se puede enviar hasta el mismo día). Se valida al enviar y reenviar, no al crear ni editar el borrador. Fuente: un estudiante ("como mínimo unos 3 días antes"), pendiente de confirmar | HU-04 |
| S-36 | Aproximación al Sistema Escolar no entrega planificaciones: crear una responde 409 (columna `requiere_planificacion`). Fuente: un estudiante, pendiente de confirmar | HU-04 |
| S-37 | **Sin resolver:** se mantiene el flujo en secuencia de la entrevista (guía → profesor del curso) aunque un estudiante dice que se manda a ambos "para que la aprueben o hagan sus observaciones". Si el cliente confirma el envío en paralelo, hay que cambiar la máquina de estados (`PlanificacionService`) | HU-04 |
| S-38 | El jefe de carrera pidió ver los chats (HU-20, Sprint 4); se asume el rol COORDINADOR. El acceso del guía al chat queda por decidir por la privacidad que reclama un estudiante. No afecta al código del Sprint 1 | HU-16, HU-20 |

## Problemas conocidos y limitaciones

- **Las pruebas de integración asumen los datos exactos de `db/seed.sql`** (cuentan filas). Si la base tiene datos extra (por ejemplo tras probar a mano en Swagger), fallan algunas: reinstala con `.\db\bd.ps1` y repite. Las pruebas en sí no modifican la base (rollback).
- Un estudiante de Investigación en el Aula no se puede evaluar todavía: solo hay pauta para Trabajo Colaborativo (S-07).
- Si el cliente confirma que el flujo de planificación es en paralelo (S-37) hay que rehacer la máquina de estados de HU-04.
- Sin control de concurrencia optimista (`@Version`): dos modificaciones simultáneas del mismo registro se resuelven por orden de llegada (la base sigue impidiendo estados inválidos).
- Sin límite de intentos de login, cambio ni recuperación de contraseña, refresco de token ni paginación.
- Swagger UI y `/v3/api-docs` están abiertos (necesario para la demo); en un despliegue real se desactivan con `springdoc.api-docs.enabled=false`.
- El secreto del JWT es temporal si no se define `ISW_JWT_SECRET`: tras reiniciar la app hay que iniciar sesión de nuevo (documentado).

## Pendientes y mejoras (para sprints siguientes; nada bloquea la entrega)

- Confirmar con el cliente/profesora los supuestos S-01 a S-33 (sobre todo S-05, S-09, S-10, S-21, S-29).
- Sprint 2: configurar pautas (HU-06), pautas de Práctica Pedagógica/Profesional y tutor (HU-07), informes (HU-08), nota final (HU-09), duplas (HU-10).
- Sprints 3 y 4: nota de voz y transcripción, panel del coordinador, chatbot (usará `GET /api/estudiantes/{id}/historial` como contexto).
- Agregar `@Version` en `Planificacion` y `Evaluacion` y paginación en los listados.

## Migraciones de base de datos

**`db/migraciones/001_grupo_y_planificacion_por_asignatura.sql`** (2026-09-20, por las respuestas de un estudiante): agrega la modalidad `GRUPO` al CHECK de `asignatura_practica` y la columna `requiere_planificacion` (BOOLEAN, por defecto TRUE); Aproximación pasa a `GRUPO` sin planificaciones y Ambientes a `DUPLA`.
- Se ejecuta **una sola vez y como root** (`isw_app` no puede alterar tablas). Probada sobre la base anterior y comparada con una instalación nueva: el resultado es idéntico.
- `db/schema.sql`, `db/seed.sql` y `db/verificacion.sql` ya incluyen el cambio: una base nueva (`.\db\bd.ps1`) no necesita la migración.
- `verificacion.sql`: el control "EN_AUTORIZACION sin aprobación del guía" ignora las inscripciones sin guía y hay un control nuevo (planificaciones en asignaturas que no las usan).

## Cómo se probó (para repetirlo)

1. `.\db\bd.ps1` (base limpia) → `cd backend` → `mvn clean package` (146 pruebas).
2. Arrancar `java -jar target\practicas-0.1.0-SNAPSHOT.jar` y probar con Swagger según `docs/GUIA_DEFENSA.md`.
