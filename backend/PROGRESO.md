# PROGRESO — Fase C (backend Spring Boot), Fase E (documentos, nota de voz, frontend), Fase F (datos de demo) y Fase H (pruebas)

> Se actualiza después de cada historia. Si retomas el trabajo, lee esto primero.
> **Última actualización: 2026-09-22 — S-47: enviar una planificación a revisión ahora también exige al menos un
> documento adjunto (la planificación formal en Word o PDF, HU-21); los cinco campos del formulario quedan como
> resumen. Encontrado y corregido durante la ronda de pruebas de la Fase H. Sobre la Fase F (actividad real de
> demostración), la Fase E (documentos, nota de voz, menú limpio, búsqueda), el Sprint 1 (2026-09-20) y el
> front-end web (Fase D, 2026-09-21).**

## Estado

| Historia | Estado | Pruebas automáticas | Prueba real (HTTP + MySQL) |
|---|---|---|---|
| Esqueleto (pom, config, seguridad, entidades, errores) | TERMINADO | — | arranca en :8080, `ddl-auto=validate` OK |
| HU-01 Autenticación con roles | TERMINADA | 21 (11 integración + 6 JwtService + 4 AuthService) | 32 de 32 OK |
| HU-02 Estudiantes e inscripción | TERMINADA | 20 (integración; conteos actualizados para 20 estudiantes el 22-sep) + 15 Rut + 9 Inscripción + 3 Estudiante + 2 Usuario | 30 de 30 OK |
| HU-03 Evaluar con la pauta digital | TERMINADA | 24 (11 integración + 13 servicio) | 24 de 24 OK |
| HU-04 Planificación con flujo de aprobación | TERMINADA | 34 (14 integración + 20 servicio; +2 el 22-sep por S-47) | 27 de 27 OK |
| HU-05 Historial del estudiante | TERMINADA | 15 (10 integración + 5 servicio) | 18 de 18 OK |
| HU-11, HU-12 Nota de voz y transcripción (Fase E) | TERMINADA | 6 (integración) | grabar, consultar, corregir y 403 ajeno: OK |
| HU-21, HU-22 Documentos adjuntos (Fase E) | TERMINADA | 7 (integración) | subir, listar, descargar, extensión y tamaño rechazados, permisos: OK |
| Transversal: control de acceso (`AccesoService`) | TERMINADO | 5 | — |
| Guía de defensa (`docs/GUIA_DEFENSA.md`) y `backend/README.md` | TERMINADAS | — | guion completo ensayado por HTTP: 59 de 59 OK |

**Total: 161 pruebas automáticas, 0 fallos** (`mvn clean package`: 146 del Sprint 1 + 13 de la Fase E + 2 del 22-sep por S-47). Más 131 comprobaciones por HTTP real contra MySQL, el ensayo del guion de defensa (59), 11 comprobaciones de las reglas del 2026-09-20, y las pruebas por HTTP de documentos y nota de voz del 22-sep (ver "Cómo se probó").

**Cambios del 2026-09-20 por las respuestas de un estudiante** (ver `RESUMEN.md` §13): modalidad `GRUPO`, `requiere_planificacion`, sin guía en el ciclo inicial, planificación sin guía directo al profesor del curso, 3 días de anticipación. Detalle en S-34 a S-38 y en "Migraciones".

**Cambios del 2026-09-22 (Fase E):** documentos adjuntos en planificaciones y evaluaciones (HU-21, HU-22), nota de voz con transcripción (HU-11, HU-12), 20 estudiantes de demostración (antes 5), menú lateral sin sprints/HU/"Próximamente", accesos rápidos dentro del menú y búsqueda de estudiantes por nombre/RUT/correo (HU-23, HU-24). Detalle en la sección "Fase E" más abajo y en S-39 a S-46.

**Cambios del 2026-09-22 (Fase F, sin migración):** `db/seed.sql` ahora da actividad real a los ~20 estudiantes y a los 6 profesores guía y 4 profesores del curso: 10 planificaciones más (19 en total, los 5 estados representados) y 8 evaluaciones más (11 en total, 2 en BORRADOR). Se reasignó el guía de 6 inscripciones de Trabajo Colaborativo de la generación completa para que los 4 guías nuevos (Fase E) también evaluaran. `db/verificacion.sql` suma un bloque que confirma que nadie activo queda en cero registros relacionados. Detalle en la sección "Fase F" más abajo.

**Cambios del 2026-09-22 (Fase H, ronda de pruebas):** se recorrieron las 5 HU y las adelantadas de la Fase E por HTTP y en el navegador (Claude in Chrome); único hallazgo real: enviar una planificación sin documento adjunto no daba error (S-47, ver arriba), corregido en `PlanificacionService.enviar` (exige `documentos.existsByPlanificacionId`) y en el formulario del front-end (campo de archivo opcional-si-ya-existe-uno, sube antes de enviar). 2 pruebas nuevas (`PlanificacionServiceTest`, `HU04PlanificacionIntegracionTest`) y 4 pruebas existentes de `HU04PlanificacionIntegracionTest` ajustadas para adjuntar un documento antes de un envío que espera éxito.

**Siguiente:** nada pendiente de lo que se pidió en esta fase. Ver "Pendientes y mejoras" abajo. Antes de la defensa: ejecutar `.\db\bd.ps1` (datos limpios) y arrancar el jar.

## Front-end web (Fase D, 2026-09-21, rama `frontend`)

Interfaz en HTML + CSS + JavaScript puro (sin frameworks, sin npm, sin CDN) en `src/main/resources/static/`, servida por la misma aplicación en <http://localhost:8080> (mismo origen: sin CORS). El backend no cambió salvo `SecurityConfig` (ver abajo). **No es una historia nueva:** es la capa que permite mostrar las 5 HU sin Swagger.

| Archivo | Qué hace |
|---|---|
| `index.html`, `css/login.css`, `js/login.js` | Login (logo institucional, errores 400/401, si ya hay sesión pasa a la app) |
| `app.html`, `css/app.css`, `js/app.js` | Estructura fija: cabecera con el escudo, barra lateral por rol (colapsable) y zona principal con enrutador por hash |
| `js/menu.js` | Las funciones a las que cada rol tiene acceso (sin mencionar sprints ni HU, desde el 22-sep) y los accesos rápidos por rol |
| `js/api.js` | `fetch` con `Authorization: Bearer`; JWT en `sessionStorage`; un 401 borra la sesión y vuelve al login. Desde el 22-sep también `subir()` (multipart) y `archivo()` (descarga/streaming binario con el token) |
| `js/ui.js` | Plantilla `html` que escapa todo (contra XSS), iconos SVG, toasts, modales, validación de formularios en español y traducción de los errores 400/401/403/404/409 |
| `js/vistas/*.js`, `css/vistas.css` | `inicio`, `cuenta` (HU-01), `gestion` (HU-02), `evaluaciones` (HU-03), `planificaciones` (HU-04), `historial` (HU-05, con buscador desde el 22-sep) |
| `js/documentos.js`, `js/notaVoz.js` (22-sep, Fase E) | Secciones reutilizables "Documentos" y "Retroalimentación en voz"/"Transcripción a texto", montadas en el detalle de planificación y de evaluación |

- **Rutas:** `#/inicio`, `#/cuenta`, `#/estudiantes` (y `#/profesores`, `#/inscripciones`), `#/evaluaciones[/nueva|/{id}]`, `#/planificaciones[/nueva|/{id}|/{id}/editar]`, `#/historial[/{estudianteId}]`.
- **`SecurityConfig`:** se permiten solo los recursos estáticos (`/`, `/index.html`, `/app.html`, `/css/**`, `/js/**`, `/img/**`, `/favicon.ico`). `/api/**` sigue exigiendo token, salvo `/api/auth/login`. Sin cambios de esquema ni de reglas de negocio; las 146 pruebas siguen en verde. Los endpoints de documentos y nota de voz (22-sep) no necesitaron tocar `SecurityConfig`: heredan la misma exigencia de token de cualquier `/api/**`.
- **Imágenes** (en `static/img/`, nombres exactos): `LogoInstitucionalPNG.png` solo en el login; `EscudoUBB.png` fijo en la esquina superior izquierda de la cabecera (el PNG trae mucho margen transparente y un texto blanco, así que se recorta a solo el escudo dentro de un marco blanco redondeado); `LaCastilla.jpg` en la pantalla de inicio. Si falta un archivo hay un degradado de respaldo.
- **Cómo se verificó (20/21-sep):** compilación y 146 pruebas; 200 en `/` y `/app.html`, 401 en `/api/**` sin token; recorrido de todas las vistas con los 5 roles en Chrome sin errores de consola; flujos completos (evaluación creada, respondida y finalizada; planificación aprobada por el guía y autorizada por el profesor del curso; alta de estudiante e inscripción; cierre de inscripción; errores 400, 403, 404 y 409; token inválido → login).
- **Cómo se verificó lo del 22-sep (Fase E):** sin navegador disponible en esa sesión, se probó por HTTP con `curl` (login, subir/listar/descargar documento, 403 de quien no corresponde, grabar/consultar/corregir nota de voz, 403 de otro guía) y se revisó el código de cada vista a mano; falta el recorrido visual en Chrome antes de la defensa.
- **Limitaciones:** no hay editor de pautas ni cambio de contraseña (fuera del Sprint 1) y el menú solo muestra las funciones ya construidas. **SUPUESTO de presentación (no del cliente):** para los roles distintos del coordinador, HU-02 se muestra como "Mis inscripciones" (solo lectura).

## Endpoints creados (42 operaciones; todas visibles en Swagger)

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
| HU-21 | `POST /api/planificaciones/{id}/documentos` (multipart, Word o PDF, 10 MB) | ESTUDIANTE (dueño de la planificación) |
| HU-21 | `GET /api/planificaciones/{id}/documentos` | quien tenga relación con la inscripción (dueño, guía, curso, coordinador) |
| HU-21 | `GET /api/planificaciones/{id}/documentos/{docId}` (descarga) | quien tenga relación con la inscripción |
| HU-22 | `POST /api/evaluaciones/{id}/documentos` (multipart, Word/Excel/PDF, 10 MB) | PROFESOR_GUIA (el evaluador de esa evaluación) |
| HU-22 | `GET /api/evaluaciones/{id}/documentos` | quien pueda ver la evaluación (reglas de HU-03) |
| HU-22 | `GET /api/evaluaciones/{id}/documentos/{docId}` (descarga) | quien pueda ver la evaluación |
| HU-11, HU-12 | `POST /api/evaluaciones/{id}/nota-voz` (multipart: audio + transcripción inicial) | PROFESOR_GUIA (el evaluador; 409 si ya existe una) |
| HU-12 | `PUT /api/evaluaciones/{id}/nota-voz` (corrige la transcripción) | PROFESOR_GUIA (el evaluador) |
| HU-11, HU-12 | `GET /api/evaluaciones/{id}/nota-voz` (metadatos y transcripción) | quien pueda ver la evaluación |
| HU-11 | `GET /api/evaluaciones/{id}/nota-voz/audio` (streaming del audio) | quien pueda ver la evaluación |

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
- **Fase E (22-sep):** `AlmacenamientoArchivosService` guarda los archivos en disco bajo `app.adjuntos.directorio` (por defecto `adjuntos/`, relativo a donde se arranca el jar) con un nombre único (UUID + extensión); valida extensión y tamaño antes de guardar y nunca confía en la ruta que venga del cliente (`Path.resolve(...).normalize()` + comprobación de que sigue dentro de la carpeta base). `spring.servlet.multipart.max-file-size/max-request-size=30MB` en `application.properties`. `GlobalExceptionHandler` traduce `MaxUploadSizeExceededException` y `MissingServletRequestPartException` a 400 en español.
- `DocumentoEvaluacionService` y `NotaVozService` reutilizan el método `puedeVer(Evaluacion, actor)` de `EvaluacionService` (visible por paquete, no público) en vez de duplicar la regla de acceso de HU-03.

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

## SUPUESTOS de la Fase E (2026-09-22; se suman a los de arriba, ninguno está confirmado)

| ID | Supuesto | Historia |
|---|---|---|
| S-39 | Generación completa de 20 estudiantes de demostración (`db/seed.sql`, PARTE 2B): perfiles de avance y a qué guía o profesor del curso se asigna cada uno son datos inventados por el equipo. Los ids 1-18 no se tocaron: las pruebas de integración existentes siguen intactas | `db/seed.sql` |
| S-40 | La transcripción inicial de la nota de voz la genera el navegador del evaluador (Web Speech API de Chrome, `SpeechRecognition`), sin costo y sin instalar nada; el texto llega ya hecho en el `POST` y el backend solo lo guarda. Nadie ha confirmado con el cliente qué herramienta de transcripción usar | HU-12 |
| S-41 | El documento formal de la evaluación (HU-22) se puede subir con la evaluación en `BORRADOR` o ya `FINALIZADA`: no se exige un estado particular, porque el cliente no lo especificó | HU-22 |
| S-42 | La nota de voz (HU-11) se puede grabar con la evaluación en `BORRADOR` o ya `FINALIZADA`, porque es retroalimentación complementaria: no cambia el puntaje ni rompe la inmutabilidad de una evaluación finalizada (ni la aplicación ni la base lo impiden) | HU-11 |
| S-43 | Tamaño y formatos de archivo: documentos hasta 10 MB (`.docx`/`.pdf` en planificaciones; `.docx`/`.xlsx`/`.pdf` en evaluaciones, `AlmacenamientoArchivosService.TAMANO_MAXIMO_DOCUMENTO`) y audio hasta 25 MB (`.webm`/`.ogg`/`.mp3`/`.wav`/`.m4a`, `TAMANO_MAXIMO_AUDIO`). Son decisiones propias, no confirmadas por el cliente | HU-11, HU-21, HU-22 |
| S-44 | Se agregaron endpoints no pedidos explícitamente: `GET .../documentos` (listar, además de descargar uno) y `GET .../nota-voz/audio` (el audio en una ruta aparte de los metadatos), necesarios para que el front-end funcione | HU-11, HU-21, HU-22 |
| S-45 | `planificacion` sigue con los cinco elementos de la entrevista (no los del formato oficial real del `.docx`, ver S-09 en `product-backlog.md`): migrar la entidad, los DTO, el formulario y las 146 pruebas existentes queda para un sprint futuro | HU-04 |
| S-46 | HU-15, HU-16, HU-17 y HU-20 se reescribieron en `product-backlog.md` con el contenido real de "RetroPlani Mate" (`ChatBot (1).rar`), pero siguen sin implementarse en este backend | HU-15, HU-16, HU-17, HU-20 |
| S-47 | **Añadido el 22-sep (Fase H):** `PlanificacionService.enviar` exige `DocumentoPlanificacionRepository.existsByPlanificacionId`: sin al menos un documento adjunto, enviar a revisión da 400 (se suma a `elementosFaltantes()` como `"documentoFormal"`). Basta con que exista uno; no hay que volver a subirlo en cada reenvío. Decisión propia, no confirmada por el cliente | HU-04, HU-21 |

## Fase F: datos de demostración con actividad real (2026-09-22, sin migración)

Motivo: la generación completa de la Fase E (20 estudiantes) concentraba casi toda la actividad (evaluaciones,
planificaciones) en 4-5 estudiantes y en un solo guía (Marcos Bravo). Se amplió `db/seed.sql` para que la base se
vea como un sistema realmente en uso, sin tocar el esquema.

- **Reasignación de guías (6 inscripciones de Trabajo Colaborativo de la generación completa):** las inscripciones
  34, 37, 40 y 43 pasaron a los 4 guías nuevos de la Fase E (11, 12, 13 y 14, uno cada una) para que también
  pudieran evaluar (solo hay pauta digital para Trabajo Colaborativo, S-07); la 49 pasó de guía 3 a 12 y la 53 de
  guía 12 a 3, para que el guía 12 (Carolina) tuviera 2 estudiantes en vez de 1. Los ids de las inscripciones y sus
  demás columnas no cambiaron, solo `profesor_guia_id`.
- **10 planificaciones nuevas (ids 10 a 19):** 4 en Ambientes de Aprendizaje (sin guía, decide directo el profesor
  del curso) y 6 en Trabajo Colaborativo (con guía). Reparten los 5 estados: 2 BORRADOR, 2 EN_AUTORIZACION,
  4 CON_OBSERVACIONES, 8 AUTORIZADA en total (sumando las 9 del Sprint 1).
- **11 revisiones nuevas (ids 13 a 23):** decisiones de guía y de curso sobre esas planificaciones, calculadas para
  que los 4 guías nuevos y los 2 profesores del curso nuevos (Fase E) queden con al menos una revisión cada uno.
- **8 evaluaciones nuevas (ids 4 a 11):** una por cada estudiante de Trabajo Colaborativo que todavía no tenía
  ninguna (incluidas Camila y Diego, que ya tenían planificaciones pero ninguna evaluación). 6 se finalizan con los
  20 indicadores (mismo patrón `ELT` que las 3 del Sprint 1: se insertan en BORRADOR y una única `UPDATE` las pasa a
  FINALIZADA); 2 quedan en BORRADOR con un avance parcial (6 de 20 indicadores), para mostrar también ese estado.
- **`db/verificacion.sql`, bloque 18:** confirma en una sola consulta que ningún estudiante en Trabajo Colaborativo
  se queda sin evaluación, ningún estudiante en una asignatura que pide planificación se queda sin ninguna, y
  ningún profesor guía o del curso activo se queda sin evaluación/revisión (o con menos de 2 o más de 20
  estudiantes asignados, en el caso del guía). El bloque 19 muestra la actividad de cada uno para revisar a ojo.
- **Pruebas de integración ajustadas (4):** `HU02EstudiantesInscripcionIntegracionTest.cadaRolVeSoloSusInscripciones`,
  `HU03EvaluacionIntegracionTest.lasEvaluacionesFinalizadasDelSeedSeVenSegunElRol` y `.unBorradorEsPrivadoDelEvaluadorYElCoordinador`,
  y `HU04PlanificacionIntegracionTest.cadaRolVeLasPlanificacionesDeSusInscripcionesEnCualquierEstado` contaban
  inscripciones, evaluaciones o planificaciones exactas de Marcos Bravo (guía 2), Lorena Vidal (guía 3) o Ricardo
  Núñez (curso 1): se actualizaron con los ids nuevos que ahora también les pertenecen. Las 159 pruebas siguen en
  verde (no se agregó ninguna prueba nueva en esta fase, solo datos).
- **Resultado (verificado):** 10 estudiantes distintos con evaluación (antes 2: Sebastián y Valentina) y 14
  estudiantes distintos con planificación (antes 4: Camila, Diego, Valentina y Sebastián). Los 6 guías quedaron con
  2 a 4 estudiantes, 1 a 5 evaluaciones y 1 a 6 revisiones; los 4 profesores del curso con 8 a 13 estudiantes y 1 a
  4 revisiones. Nadie en cero.

## Problemas conocidos y limitaciones

- **Las pruebas de integración asumen los datos exactos de `db/seed.sql`** (cuentan filas). Si la base tiene datos extra (por ejemplo tras probar a mano en Swagger), fallan algunas: reinstala con `.\db\bd.ps1` y repite. Las pruebas en sí no modifican la base (rollback).
- Un estudiante de Investigación en el Aula no se puede evaluar todavía: solo hay pauta para Trabajo Colaborativo (S-07).
- Si el cliente confirma que el flujo de planificación es en paralelo (S-37) hay que rehacer la máquina de estados de HU-04.
- Sin control de concurrencia optimista (`@Version`): dos modificaciones simultáneas del mismo registro se resuelven por orden de llegada (la base sigue impidiendo estados inválidos).
- Sin límite de intentos de login, cambio ni recuperación de contraseña (HU-26, pendiente), refresco de token ni paginación.
- Swagger UI y `/v3/api-docs` están abiertos (necesario para la demo); en un despliegue real se desactivan con `springdoc.api-docs.enabled=false`.
- El secreto del JWT es temporal si no se define `ISW_JWT_SECRET`: tras reiniciar la app hay que iniciar sesión de nuevo (documentado).
- **Fase E:** los archivos se guardan en el disco local del servidor (`backend/adjuntos/`), no en un almacenamiento externo; en un despliegue con varias instancias habría que moverlos a un storage compartido. La transcripción de la nota de voz depende del navegador del evaluador (Web Speech API): no funciona en todos los navegadores (S-40). No se probó todavía en un navegador real por falta de esa herramienta en esta sesión (ver "Cómo se verificó lo del 22-sep" arriba); falta ese recorrido visual antes de la defensa.

## Pendientes y mejoras (para sprints siguientes; nada bloquea la entrega)

- Confirmar con el cliente/profesora los supuestos S-01 a S-46 (sobre todo S-05, S-09, S-10, S-21, S-29, S-40, S-45).
- Sprint 2: configurar pautas (HU-06), pautas de Práctica Pedagógica/Profesional y tutor (HU-07), informes (HU-08), nota final (HU-09), duplas (HU-10), cambiar la contraseña (HU-26), gestión de archivos de pautas formales (HU-25), configurar parámetros del sistema (HU-27), exportar el historial en PDF (HU-28).
- Sprints 3 y 4: panel del coordinador, chatbot y su base de conocimiento (usará `GET /api/estudiantes/{id}/historial` como contexto; HU-15 a HU-17, HU-29, HU-30).
- Agregar `@Version` en `Planificacion` y `Evaluacion` y paginación en los listados.
- Migrar `Planificacion` al formato oficial real (S-45), cuando se decida el impacto en el formulario y en las 146 pruebas que ya lo usan.

## Migraciones de base de datos

**`db/migraciones/001_grupo_y_planificacion_por_asignatura.sql`** (2026-09-20, por las respuestas de un estudiante): agrega la modalidad `GRUPO` al CHECK de `asignatura_practica` y la columna `requiere_planificacion` (BOOLEAN, por defecto TRUE); Aproximación pasa a `GRUPO` sin planificaciones y Ambientes a `DUPLA`.
- Se ejecuta **una sola vez y como root** (`isw_app` no puede alterar tablas). Probada sobre la base anterior y comparada con una instalación nueva: el resultado es idéntico.
- `db/schema.sql`, `db/seed.sql` y `db/verificacion.sql` ya incluyen el cambio: una base nueva (`.\db\bd.ps1`) no necesita la migración.
- `verificacion.sql`: el control "EN_AUTORIZACION sin aprobación del guía" ignora las inscripciones sin guía y hay un control nuevo (planificaciones en asignaturas que no las usan).

**`db/migraciones/002_documentos_adjuntos.sql`** (2026-09-22, Fase E): agrega las tablas `documento_planificacion` y `documento_evaluacion` (no toca `nota_voz`, que ya existía con todas las columnas que necesitaba la API). Igual que la 001, se ejecuta una sola vez y como root; `db/schema.sql` ya la incluye para una base nueva.

**Fase F (2026-09-22): sin migración nueva.** Solo se amplió `db/seed.sql` (más planificaciones y evaluaciones) y `db/verificacion.sql` (bloque 18 y 19); el esquema no cambió.

## Cómo se probó (para repetirlo)

1. `.\db\bd.ps1` (base limpia) → `cd backend` → `mvn clean package` (159 pruebas).
2. Arrancar `java -jar target\practicas-0.1.0-SNAPSHOT.jar` y probar con Swagger según `docs/GUIA_DEFENSA.md`.
3. **Fase E (22-sep):** además, con la app arrancada y sin navegador disponible, se probó por HTTP con `curl -F` (multipart): login, `POST` y `GET` de documentos en una planificación y en una evaluación, descarga con las cabeceras `Content-Disposition` correctas, 403 de quien no corresponde; `POST`, `GET` y `PUT` de nota de voz, `GET` del audio, 409 al grabar una segunda vez y 403 de otro guía. Después se reinstaló la base (`.\db\bd.ps1`) para dejarla limpia.
4. **Fase F (22-sep):** `.\db\bd.ps1` → revisar el bloque 18 y 19 de `db/verificacion.sql` (0 violaciones, actividad por guía y por profesor del curso) → `mvn clean package` (159 pruebas) → arrancar la app y comprobar por HTTP (`GET /api/evaluaciones` y `GET /api/planificaciones` como coordinador) que aparecen 10 y 14 estudiantes distintos. Se reinstaló la base al final para dejarla limpia.
