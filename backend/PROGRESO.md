# PROGRESO — Fase C (backend Spring Boot), Fase E (documentos, nota de voz, frontend), Fase F (datos de demo), Fase H (pruebas) y Sprint 2 (HU-31)

> Se actualiza después de cada historia. Si retomas el trabajo, lee esto primero.
> **2026-10-10:** el Sprint 1 se entregó y presentó el 25-sep; el código no ha cambiado desde el 22-sep. El Sprint 2
> parte por publicar el sistema en un servidor de la universidad (ver `DESPLIEGUE.md` en la raíz). `docs/GUIA_DEFENSA.md`,
> citado más abajo, ya no está en el repositorio: el material de la defensa quedó en `GUIA_ESTUDIO_SPRINT1.md`.
> **2026-10-10 (Sprint 2): HU-31 Cartola de prácticas** construida y probada, con un **rol nuevo, `JEFE_CARRERA`** (ahora
> son **5 roles**), la columna `inscripcion.fecha_cierre` (migración 003) y OpenPDF 2.0.5. **43 endpoints y 175 pruebas**.
> Resuelve la parte de S-38 sobre el rol del jefe de carrera. Detalle en la sección "Sprint 2: HU-31".
> **2026-10-11 (Sprint 2): perfil de producción** (`application-prod.properties`, `ISW_JWT_SECRET` obligatoria en prod,
> `db/datos-iniciales.sql` sin datos de demo y `GeneradorHashClave`). **177 pruebas.** Detalle en "Sprint 2: perfil de producción".
> **2026-10-11 (Sprint 2, CONFIRMADO POR EL CLIENTE): segundo rol opcional ("birol") y jefe de carrera con lectura total.**
> La persona que es jefe de carrera también evalúa como guía, y su cuenta es una sola (RUT y clave de la universidad).
> Columna `usuario.rol_secundario_id` (migración 004); los permisos son la **suma** de los dos roles; `JEFE_CARRERA` lee
> todo como el coordinador (sin escribir nada por ese rol); el menú muestra la unión de pantallas y se agregó la pantalla
> de la cartola. **Ya no aplica S-11 ("un solo rol por usuario") para cuentas especiales**, y queda reemplazada la decisión
> del 10-oct ("jefe de carrera de solo lectura, su único acceso es la cartola"). **201 pruebas.** Detalle en "Sprint 2: segundo rol".
> **2026-10-11 (Sprint 2): carpeta `deploy/`** (instalación Linux, nginx, systemd, respaldo y guía del equipo; probada en
> Ubuntu 26.04 en WSL) y **HU-06 Configurar pautas de evaluación** con versiones (migración 005). **210 pruebas.**
> **2026-10-26 (Sprint 2, rama `feature/chatbot`, tanda 1 de 3): HU-15 chatbot de retroalimentación (PARCIAL) y HU-16
> el guía lee el chat (TERMINADA).** Proveedor Groq con la clave en `ISW_GROQ_API_KEY` (S-59; hasta el mismo 26-oct era
> Anthropic, ver "Cambio de proveedor a Groq"), migración 007,
> **52 endpoints y 239 pruebas** (tras el cambio a Groq). Supuestos S-59 a S-64. Detalle en la sección "Sprint 2: chatbot, tanda 1 de 3".
> **2026-10-26 (tanda 2 de 3): HU-17 el coordinador calibra el estilo del chatbot (TERMINADA) y HU-20 el jefe de carrera
> lee los chats (PARCIAL: sin aviso al estudiante ni registro de accesos en la base).** Migración 008 (tabla 17,
> `instrucciones_chatbot`), **54 endpoints y 250 pruebas**. Supuestos S-65 a S-68. Probado con el modelo real de Groq.
> Detalle en la sección "Sprint 2: chatbot, tanda 2 de 3 - HU-17 y HU-20".
> **2026-10-26 (tanda 3 y cierre final): las 6 HU del chatbot COMPLETAS** (HU-15, 16, 17, 20, 29 y 30). Tanda 3: HU-29
> base de conocimiento y HU-30 validadores (migración 009). Cierre final: regla en código para la planificación pegada
> (HU-15, S-72), aviso y registro de lecturas del jefe (HU-20, migración 010), ranking TF-IDF (HU-29, S-74) y Escala SUS
> y rúbrica dentro del sistema (HU-30, migración 011, S-75). **22 tablas, 11 migraciones, 64 endpoints, 280 pruebas,
> 31 pruebas negativas.** Detalle en "Sprint 2: chatbot, cierre final".
> **Última actualización anterior: 2026-09-22 — S-47: enviar una planificación a revisión ahora también exige al menos un
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
| HU-31 Cartola de prácticas y rol JEFE_CARRERA (Sprint 2) | TERMINADA | 14 (8 integración + 6 servicio) | PDF 200 para el jefe, 403 para los otros 4 roles, 400 con fechas invertidas: OK |
| Segundo rol opcional y jefe de carrera con lectura total (Sprint 2, confirmado por el cliente) | TERMINADO | 9 (6 integración + 3 `AccesoService`) | por HTTP con `jefe.carrera@demo.example`: perfil con los 2 roles, lecturas 200, `/api/profesores` 403, cartola 200 |
| HU-06 Configurar pautas de evaluación (Sprint 2) | TERMINADA | 9 (integración) | pantalla del coordinador recorrida en Chrome headless: crear, error de escala, editar una pauta usada (versión 2), lista con versiones; sin errores de JavaScript |
| HU-15 Chatbot de retroalimentación (Sprint 2, 26-oct) | PARCIAL (criterios 3 a 8 por instrucciones al modelo, sin probar con el modelo real) | 8 integración + 5 `ModeloLenguajeGroq` + 2 `TextoDocx` + 2 `TextoPdf` | sin clave del proveedor: 503 claro y nada guardado; navegador con mensajes de ejemplo |
| HU-16 El guía lee el chat (Sprint 2, 26-oct) | TERMINADA | 3 integración + 1 `AccesoService` | guía 200 solo lectura, coordinador 403 |
| Carpeta `deploy/` (Sprint 2) | TERMINADA | — | instalación completa en Ubuntu 26.04 (WSL): reinicio tras `kill -9`, 29 MB pasa y 31 MB da 413, 80 → 301 a HTTPS, respaldo con fecha y restaurado en una base de prueba, reinstalación sin tocar datos |
| Transversal: control de acceso (`AccesoService`) | TERMINADO | 8 | — |
| Guía de defensa (`docs/GUIA_DEFENSA.md`) y `backend/README.md` | TERMINADAS | — | guion completo ensayado por HTTP: 59 de 59 OK |

**Total: 175 pruebas automáticas, 0 fallos** (`mvn clean package`: 146 del Sprint 1 + 13 de la Fase E + 2 del 22-sep por S-47 + 14 de HU-31 el 10-oct). Más 131 comprobaciones por HTTP real contra MySQL, el ensayo del guion de defensa (59), 11 comprobaciones de las reglas del 2026-09-20, y las pruebas por HTTP de documentos y nota de voz del 22-sep (ver "Cómo se probó").

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

## Endpoints creados (43 operaciones; todas visibles en Swagger)

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
| HU-31 | `GET /api/reportes/cartola-practicas?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` (PDF descargable; 400 si `desde` > `hasta`) | JEFE_CARRERA |

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
| S-29 | Una evaluación en BORRADOR es privada de su autor y del coordinador (desde el 11-oct también del jefe de carrera, que tiene el mismo alcance de lectura que el coordinador); el estudiante y el profesor del curso ven solo las FINALIZADAS | HU-03 |
| S-30 | Solo se puede evaluar una inscripción EN_CURSO; la pauta debe ser de la asignatura de la inscripción y estar activa; el evaluador debe corresponder al tipo de la pauta (COLABORADOR = PROFESOR_GUIA); la planificación vinculada es opcional pero debe ser de la misma inscripción y estar AUTORIZADA | HU-03 |
| S-31 | El puntaje debe ser un entero. El promedio es total ÷ cantidad de indicadores, con 2 decimales (HALF_UP) | HU-03 |
| S-32 | El estudiante, su guía, su profesor del curso y el coordinador ven la planificación en cualquier estado (incluido BORRADOR). El envío y el reenvío del estudiante no generan fila en `revision_planificacion` (solo las decisiones del guía y del curso, como dice el MER) | HU-04 |
| S-33 | *(La parte "sin guía, 409" fue reemplazada por S-34: sin guía la planificación va directo al profesor del curso.)* Los textos en blanco se guardan como vacíos (NULL). No se restringe que la fecha de la clase sea futura | HU-04 |

| S-34 | **Cambia S-25.** Donde no se va a un colegio (ciclo inicial) no existe profesor guía: asignarlo da 400. Si la inscripción no tiene guía, al enviar la planificación pasa **directo a `EN_AUTORIZACION`** (la autoriza el profesor del curso) y ningún guía puede revisarla (403). Reemplaza la regla anterior (S-33: 409 si no había guía) | HU-02, HU-04 |
| S-35 | La planificación se envía con al menos 3 días de anticipación a la clase (`app.planificacion.dias-anticipacion-minimos=3`; con 0 se puede enviar hasta el mismo día). Se valida al enviar y reenviar, no al crear ni editar el borrador. Fuente: un estudiante ("como mínimo unos 3 días antes"), pendiente de confirmar | HU-04 |
| S-36 | Aproximación al Sistema Escolar no entrega planificaciones: crear una responde 409 (columna `requiere_planificacion`). Fuente: un estudiante, pendiente de confirmar | HU-04 |
| S-37 | **Sin resolver:** se mantiene el flujo en secuencia de la entrevista (guía → profesor del curso) aunque un estudiante dice que se manda a ambos "para que la aprueben o hagan sus observaciones". Si el cliente confirma el envío en paralelo, hay que cambiar la máquina de estados (`PlanificacionService`) | HU-04 |
| S-38 | El jefe de carrera pidió ver los chats (HU-20, Sprint 4). ~~Se asume el rol COORDINADOR.~~ **Resuelto el 10-oct: el equipo decidió un rol propio, `JEFE_CARRERA`, de solo lectura (HU-31). Ampliado el 11-oct, confirmado por el cliente: lee todo como el coordinador y puede tener PROFESOR_GUIA como segundo rol.** El acceso del guía al chat queda por decidir por la privacidad que reclama un estudiante. No afecta al código del Sprint 1 | HU-16, HU-20 |

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

## Sprint 2: HU-31 cartola de prácticas y rol JEFE_CARRERA (2026-10-10)

**Qué es:** un PDF al estilo de una cartola bancaria con las inscripciones cerradas en un rango de fechas, para el jefe de carrera, como evidencia del criterio 3 de acreditación. Es solo lectura.

**Rol nuevo `JEFE_CARRERA` (5 roles):** fila 5 del catálogo `rol`, constante `Roles.JEFE_CARRERA`. Es de solo lectura: solo `ReporteController` lo nombra en `@PreAuthorize`. No se tocó ninguna regla de los otros 4 roles: en `AccesoService` cae en el `default` de cada `switch`, así que en los listados y detalles recibe 403 ("rol desconocido = no ve nada"). Resuelve la parte de **S-38** sobre su rol: el equipo decidió que el jefe de carrera no es el coordinador, porque no hace el trabajo operativo (registrar estudiantes, inscribir). Usuario de demostración: **Gonzalo Riquelme Sanhueza, `jefe.carrera@demo.example`** (id 32, misma clave de demostración que los demás). No hay API para crear usuarios de este rol: se crean con SQL, igual que el coordinador.

**Fecha de cierre:** la inscripción no guardaba cuándo se cerraba, así que se agregó `inscripcion.fecha_cierre` (DATE, nula mientras está `EN_CURSO`, con CHECK `ck_inscripcion_fecha_cierre` e índice). `InscripcionService.cerrar` ahora guarda `LocalDate.now(reloj)`; la regla de cierre no cambió. `InscripcionDto` suma el campo `fechaCierre`. A las inscripciones ya cerradas se les dedujo la fecha a partir del periodo (**S-48**).

**Piezas:** `ReporteController` (`GET /api/reportes/cartola-practicas`, sin `produces` a propósito para que los errores sigan en JSON), `CartolaPracticasService` (valida el rango, consulta `InscripcionRepository.findCerradasEntre` con `join fetch` y reutiliza `EvaluacionRepository.findPorInscripciones` para contar las evaluaciones finalizadas y promediar sus promedios), `CartolaFilaDto` (una fila) y `CartolaPdfGenerador` (OpenPDF: A4 horizontal, encabezado con rango y fecha de generación, tabla con encabezado repetido en cada página y número de página al pie).

**Librería nueva: OpenPDF 2.0.5** (`com.github.librepdf:openpdf`, LGPL/MPL). No había ningún generador de PDF en el proyecto; se eligió con el usuario. Es la última línea de OpenPDF compatible con Java 17 (la 2.1 y posteriores exigen Java 21). HU-28 (historial en PDF) puede reutilizarla.

**Pruebas (14):** `CartolaPracticasServiceTest` (6: rango vacío, con resultados, fechas invertidas, un solo día, contenido del PDF y PDF de un rango vacío) y `HU31CartolaPracticasIntegracionTest` (8: PDF 200 para el jefe con su texto, 16 filas de 2025 con el resumen de la inscripción 9, rango sin cierres, cerrar una inscripción guarda la fecha y aparece en la cartola, 403 para los otros 4 roles, 401 sin token, 400 con fechas invertidas o mal escritas, y 403 del jefe en las 7 rutas operativas). También cambiaron `HU01AutenticacionIntegracionTest` (5 roles) e `InscripcionServiceTest` (fecha de cierre).

## Sprint 2: segundo rol opcional y jefe de carrera con lectura total (2026-10-11, confirmado por el cliente)

**Por qué:** el cliente confirmó que la persona que es jefe de carrera también evalúa prácticas y acompaña estudiantes
como profesor guía. Su cuenta vive con el RUT y la clave de la universidad: no puede tener dos cuentas. Además pidió para
el jefe de carrera acceso máximo de lectura a todo. Reemplaza dos cosas anteriores:
- **S-11 ("cada usuario tiene un solo rol") ya no aplica para cuentas especiales.** Sigue valiendo para todas las demás.
- La decisión del 10-oct ("`JEFE_CARRERA` es de solo lectura y su único acceso es la cartola") queda reemplazada por esta.

**Segundo rol (a propósito acotado, no un sistema de N roles):** columna nullable `usuario.rol_secundario_id` (FK a `rol`,
CHECK `ck_usuario_rol_secundario_distinto`: no puede repetir el principal), migración 004. `Usuario.getRolSecundario()`,
`getNombresRoles()` y `tieneRol()`. `UsuarioAutenticado` lleva `rolSecundario` (el constructor de 3 argumentos sigue
sirviendo para cuentas normales), `roles()` y `tieneRol()` mira los dos. El filtro JWT da **una autoridad por rol**, así
que `@PreAuthorize("hasRole(...)")` acepta cualquiera de los dos. El token suma el claim informativo `roles` (los permisos
se siguen leyendo de la base en cada petición). `UsuarioDto` (login y `/api/auth/me`) suma `rolSecundario` (nulo si no tiene).

**Permisos = suma de los dos roles (`AccesoService`):** `veTodo` (coordinador o jefe de carrera; estático porque solo
depende de los roles), y `puedeVerInscripcion` / `puedeVerHistorial` aceptan si cualquiera de los roles acepta.
`alcanceDe`: si ve todo, sin filtro; con un solo rol restringido, su filtro de siempre; con dos roles restringidos (por
ejemplo guía + profesor del curso) la consulta no puede expresar el "o", así que se pide sin filtro y los listados de
inscripciones, planificaciones y evaluaciones filtran fila por fila con `puedeVerInscripcion` (lo hacen siempre; para los
4 roles normales el resultado no cambia).
**Escribir nunca depende de "puede verla":** crear o editar evaluaciones, revisar planificaciones o subir archivos sigue
exigiendo ser el estudiante dueño, el guía asignado o el evaluador (por id). Por eso el jefe de carrera que también es
guía lee todo, pero solo evalúa y revisa donde de verdad es el guía.

**`JEFE_CARRERA` lee como el coordinador:** inscripciones, planificaciones, evaluaciones (también los borradores, como el
coordinador: S-29), historiales y la ficha de cualquier estudiante (`GET /api/estudiantes` y `/{id}` pasan a
`hasAnyRole('COORDINADOR','JEFE_CARRERA')`). Sigue sin crear, editar, cerrar, aprobar ni evaluar nada **por ese rol**, y
sin `/api/roles` ni `/api/profesores` (administración). Conserva la cartola. Los otros 4 roles no cambian.

**Asignar un guía con segundo rol:** `InscripcionService` acepta como guía (o profesor del curso) a quien tenga ese rol
como principal **o** como segundo rol, y `GET /api/profesores` los incluye (por eso la prueba de profesores pasó de 6 a 7
guías y de 10 a 11 profesores: el jefe aparece como guía; no es un cambio de permisos).

**Datos de demo:** `jefe.carrera@demo.example` (id 32) tiene `PROFESOR_GUIA` como segundo rol y es el guía de la
**inscripción 40** (Isidora Bustos, Trabajo Colaborativo 2026-2; antes guía 13, Héctor Jara: cambio de guía a mitad de
semestre). Su evaluación FINALIZADA (8) y la revisión de su planificación (16) quedan a nombre del guía 13, como registro
histórico. Solo una inscripción (no dos) para no dejar a otro guía con menos de 2 estudiantes (control 18 de
`verificacion.sql`). Para la demostración: el jefe evalúa a Isidora con la pauta (puede vincular la planificación 16,
AUTORIZADA); para mostrar la revisión, Isidora crea una planificación, le sube el documento y la envía, y el jefe la revisa.

**`verificacion.sql`:** los controles de rol miran principal o segundo rol; restricciones 25 FK y 36 CHECK; la evaluación
de un guía que ya no es el de la inscripción solo es error si está en BORRADOR (no podría terminarla); las revisiones se
controlan por el rol de su etapa (y por el guía actual, salvo la histórica de la inscripción 40); el control "menos de 2
estudiantes" mira solo guías de rol principal; bloque 21 nuevo (cuentas con segundo rol: esperado 1). Todo en 0.

**Front-end (unión de pantallas, sin pantallas nuevas salvo la cartola):** `menu.js` arma el menú y los accesos rápidos
con la **unión** de los roles de la cuenta (`rolesDe`, `visiblesPara`, `accesosPara`, sin repetir rutas). El contexto de
cada vista suma `roles`, `tiene(rol)`, `veTodo` y `actuaComo(rol, inscripción)`: las vistas ya no comparan `ctx.rol`. Las
acciones de guía (evaluar, aprobar u observar, "Requiere tu acción", el contador de pendientes del inicio) aparecen solo
en las inscripciones donde la cuenta es el guía asignado. Pantalla nueva **"Cartola de prácticas"** (`vistas/cartola.js`):
dos fechas y "Descargar PDF" (resuelve el pendiente del 11-oct anotado más abajo). La cabecera, el inicio, "Mi cuenta" y
la pestaña Profesores muestran los dos roles.

**Pruebas (201, +9):** `RolSecundarioIntegracionTest` (6: perfil con los dos roles; lee todo como el coordinador y sigue
con la cartola; no crea ni modifica nada como jefe; como guía crea una evaluación en la 40 pero recibe 403 en la 3, en un
borrador ajeno y en una planificación de otro guía; el coordinador puede asignarlo como guía pero no como profesor del
curso; los demás guías siguen viendo solo lo suyo) y 3 nuevas en `AccesoServiceTest`. La prueba del 10-oct "el jefe es
solo lectura y no ve nada operativo" se reemplazó por `elJefeDeCarreraNoAdministraNada` (roles, profesores, cierre: 403).
**No probado en el navegador:** la extensión de Chrome no estaba conectada; se probó por HTTP (login, perfil, lecturas y
cartola) y se revisó el código del front-end. Falta recorrer las pantallas con `jefe.carrera@demo.example` antes del 28.

## Sprint 2: carpeta deploy/ para publicar en el servidor (2026-10-11)

Completa el punto 7 de `DESPLIEGUE.md`. Archivos (sin claves): `deploy/install.sh`, `deploy/isw-practicas.service`,
`deploy/nginx.conf`, `deploy/backup.sh`, `deploy/GUIA_DESPLIEGUE_EQUIPO.md` (pasos en orden para el equipo) y
`deploy/.gitattributes` (fuerza fin de línea LF: un `.sh` con CRLF no corre en Linux).

- **`install.sh`** (Ubuntu/Debian, con `sudo`): instala Java 17 JRE, nginx, cron y (con `--base-local`) MySQL; crea el
  usuario de sistema `isw` sin login, `/opt/isw-practicas`, `/var/lib/isw-practicas/adjuntos` (fuera de la app),
  `/var/log/isw-practicas` y `/etc/isw-practicas/isw-practicas.env` (root:isw 640, con clave de `isw_app` y
  `ISW_JWT_SECRET` aleatorias). Con base local y la base vacía: `schema.sql`, pide los datos del coordinador inicial, su
  clave sin eco (llega a `GeneradorHashClave` por la entrada estándar) y carga `datos-iniciales.sql`; crea `isw_app`
  solo con SELECT, INSERT, UPDATE, DELETE. Instala el servicio, el sitio de nginx (con un certificado **autofirmado
  provisorio de 30 días**) y el respaldo diario (cron 03:15). Al final comprueba que la app responda.
  **Es idempotente:** nunca vuelve a correr `schema.sql` sobre una base con tablas, nunca sobrescribe el archivo de
  claves ni el certificado; guarda el `.jar` anterior como `practicas.jar.anterior` (para volver atrás).
  Con `--base-externa` no instala MySQL ni arranca la app hasta completar `ISW_DB_HOST` e `ISW_DB_PASSWORD`.
- **`isw-practicas.service`:** `Restart=always`, `RestartSec=5`, perfil `prod`, usuario `isw`, `ProtectSystem=strict`
  (solo escribe en sus carpetas de datos y registros).
- **`nginx.conf`:** 80 → 301 a HTTPS (salvo `/.well-known/acme-challenge/` para Let's Encrypt), proxy a
  `127.0.0.1:8080` con las cabeceras `X-Forwarded-*`, `client_max_body_size 30M`, TLS 1.2/1.3. HSTS, `nosniff` y
  `X-Frame-Options` NO se repiten en nginx porque ya los envía Spring Security (se vio duplicado en la prueba).
- **`backup.sh`:** `mysqldump --single-transaction --routines --triggers --databases` a
  `/var/backups/isw-practicas/isw_practicas_AAAA-MM-DD_HHMM.sql` y `adjuntos_AAAA-MM-DD_HHMM.tar.gz` (permisos 600),
  comprueba que el volcado terminó ("Dump completed"), borra los de más de 30 días. Conecta como root por socket o, con
  base externa, con `/etc/isw-practicas/backup.cnf`.

**Probado en WSL (Ubuntu 26.04.1, MySQL 8.4.11, nginx 1.28.3, OpenJDK 17.0.20):** instalación desde cero con un
coordinador de prueba (la app respondió en unos 10 s y nginx por HTTPS); `kill -9` al proceso → systemd lo levantó
con PID nuevo; subida de 29 MB pasa nginx (401 de la app, sin sesión) y 31 MB da 413; `http://` → 301 a `https://`;
Swagger 404 en prod; login equivocado 401 (la app usa la base); el respaldo generó los dos archivos con la fecha del día
(16 tablas, el trigger y el procedimiento) y se restauró en una base aparte; volver a correr `install.sh` no tocó la
base ni las claves; `--base-externa` no arranca el servicio sin los datos. **Al terminar se detuvieron y deshabilitaron
`isw-practicas`, `nginx` y `mysql` dentro de WSL** (quedan instalados) para no ocupar los puertos 80, 443, 3306 y 8080
de Windows; para volver a usarlos: `wsl -d Ubuntu -u root -- systemctl enable --now mysql isw-practicas nginx`.
**No probado:** un servidor real de la UBB, el certificado definitivo (TI o Let's Encrypt) ni la opción B con una base
de TI.

## Sprint 2: HU-06 Configurar pautas de evaluación (2026-10-11)

**Qué:** el coordinador crea y edita pautas (nombre, asignatura, escala mínima y máxima, quién la aplica:
`COLABORADOR`, `TUTOR` o `ACADEMICO`; ámbito: `OBSERVACION_CLASE`, `ESPECIALIDAD` u `ORIENTACION`; dimensiones en orden
con sus indicadores). Criterios de `product-backlog.md`: (1) escala, tipo de evaluador y ámbito; (2) **si se modifica una
pauta que ya se usó, se crea una versión nueva y las evaluaciones anteriores conservan la suya**; (3) solo el coordinador.

**Base (migración 005):** `pauta.version`, `pauta.version_anterior_id` (FK a `pauta`, UNIQUE: una versión tiene a lo más
una siguiente), `pauta.fecha_creacion`; la unicidad pasa de (asignatura, nombre) a (asignatura, nombre, versión); CHECK:
la versión 1 es justo la que no tiene anterior. Probada sobre la base con datos (las pautas existentes quedan como
versión 1). Restricciones: 26 FK, 14 UNIQUE, 37 CHECK.

**Reglas (`PautaService`):**
- "Usada" = al menos una evaluación (borradores incluidos) la referencia: editarla en el lugar cambiaría los indicadores
  de esas evaluaciones.
- Editar una pauta **sin uso** la modifica en el mismo lugar (mismo id). Editar una pauta **usada** crea la versión
  siguiente (id nuevo, `version + 1`, `versionAnteriorId`), con el mismo estado activa/inactiva, y la anterior queda
  **inactiva e intacta**: sus evaluaciones conservan indicadores, escala y puntaje máximo (este se calcula siempre con la
  pauta de la evaluación). Si la petición no cambia nada, no se crea ninguna versión.
- Solo se edita, activa/desactiva o borra la **versión vigente** (la última): lo demás da 409.
- Una pauta usada **no se borra** (409 con el motivo); se desactiva o se edita. Borrar una versión 2+ sin uso la deshace
  (la anterior vuelve a ser la vigente). La asignatura de una pauta no se cambia (400). Nombre repetido en la misma
  asignatura (sin distinguir mayúsculas) → 409. Escala máxima ≤ mínima o dimensiones repetidas → 400.
- Los indicadores se numeran solos (1, 2, 3... a lo largo de toda la pauta), como en la pauta impresa.
- Un borrador hecho con la versión anterior se sigue completando y finalizando con su propia versión. Las evaluaciones
  nuevas solo pueden usar pautas activas, así que toman la versión vigente.

**Endpoints nuevos (5, todos `COORDINADOR`):** `GET /api/pautas/gestion` (todas las versiones, con `vigente` y cuántas
evaluaciones la usan), `POST /api/pautas`, `PUT /api/pautas/{id}`, `PUT /api/pautas/{id}/activa`,
`DELETE /api/pautas/{id}`. `GET /api/pautas` y `GET /api/pautas/{id}` siguen abiertos a todos (S-27) y ahora informan
`version` y `versionAnteriorId`. Total: **49 endpoints**.

**Front-end:** ítem "Pautas de evaluación" del coordinador (`vistas/pautas.js`): lista con filtro por asignatura y
casilla para ver versiones antiguas; crear y editar con dimensiones como tarjetas ("un indicador por línea"); al editar
una pauta usada avisa cuántas evaluaciones la usan y pide confirmar la versión nueva; activar/desactivar y borrar (solo
si nadie la usó). `api.del` agregado a `api.js`.

**Pruebas (9, `HU06PautasIntegracionTest`):** 403 a los otros 4 roles (crear, editar, activar, borrar, gestión) pero
lectura abierta; crear con numeración y puntaje máximo; 7 casos inválidos (escala, sin dimensiones, dimensión vacía,
dimensiones repetidas, tipo inventado, asignatura inexistente 404, nombre repetido 409); editar sin uso en el lugar y
asignatura que no cambia; **editar la pauta 1 (usada en 11 evaluaciones) crea la versión 2 y la evaluación 1 FINALIZADA
conserva su pauta, sus 20 respuestas y su puntaje máximo 140** (la versión 2 tiene 21 indicadores, máximo 147), el
borrador 4 se sigue completando con la versión 1, y la versión 1 ya no se edita ni se borra; guardar sin cambios no
versiona; una pauta en uso no se borra y una sin uso sí; borrar la versión 2 sin uso devuelve la 1 como vigente; una
pauta desactivada no se ofrece ni permite crear evaluaciones.

**Supuestos nuevos:** ver S-51 a S-53 en "SUPUESTOS del Sprint 2".

## Sprint 2: perfil de producción y datos iniciales (2026-10-11)

**Qué es:** el primer punto del checklist de `DESPLIEGUE.md` §4 (puntos 1, 4 y 5): lo necesario para que el jar pueda correr en el servidor de la UBB sin datos de demostración ni configuración de desarrollo. **Sin cambios de esquema ni de reglas de negocio; el perfil por defecto (desarrollo) no cambia.**

| Pieza | Qué hace |
|---|---|
| `src/main/resources/application-prod.properties` | Se activa con `--spring.profiles.active=prod` y solo sobrescribe lo distinto: `app.jwt.secreto-obligatorio=true`; Swagger desactivado (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`); `server.forward-headers-strategy=framework` (detrás de Nginx); adjuntos en ruta absoluta (`APP_ADJUNTOS_DIRECTORIO`, por defecto `/var/lib/isw-practicas/adjuntos`); logs a archivo (`ISW_LOG_FILE`, por defecto `/var/log/isw-practicas/isw-practicas.log`, rotación 10 MB y 30 días) con la consola limitada a `ERROR` y el banner al log |
| `JwtProperties` + `JwtService` | Propiedad nueva `app.jwt.secreto-obligatorio` (falsa por defecto). Si es verdadera y falta `ISW_JWT_SECRET`, el constructor lanza `IllegalStateException` ("Falta la variable de entorno ISW_JWT_SECRET ... Es obligatoria con el perfil prod") y la aplicación no arranca, en vez de generar un secreto temporal. Se hizo con una propiedad y no preguntando por el nombre del perfil para poder probarlo sin Spring |
| `db/datos-iniciales.sql` | Reemplaza a `seed.sql` en el servidor: los 5 roles, las 6 asignaturas, la pauta de Trabajo Colaborativo (3 dimensiones, 20 indicadores) con los mismos ids que la PARTE 1 de `seed.sql`, y **un** coordinador. Sus datos y el hash de su clave vienen de 4 variables de sesión de MySQL (`@coordinador_email`, `@coordinador_nombre`, `@coordinador_apellidos`, `@coordinador_hash`) que se definen al instalar: el archivo no tiene ninguna clave. Si falta una variable o el correo es `@demo.example`, se detiene antes de cargar nada (CHECK `faltan_variables_del_coordinador` en una tabla temporal); todo lo demás va en una transacción |
| `security/GeneradorHashClave` | Herramienta de instalación (no arranca Spring) que pide la clave sin mostrarla, dos veces, y escribe su hash BCrypt con el mismo codificador de la app; mínimo 10 caracteres. Se ejecuta desde el jar: `java -cp practicas-0.1.0-SNAPSHOT.jar -Dloader.main=cl.ubb.pem.practicas.security.GeneradorHashClave org.springframework.boot.loader.launch.PropertiesLauncher`. Si recibe la clave por tubería, descarta el BOM que antepone PowerShell (encontrado al probar) |

**Instalación en una base nueva** (como root, una sola vez): `schema.sql` → en la misma sesión `SET @coordinador_email = ...; SET @coordinador_nombre = ...; SET @coordinador_apellidos = ...; SET @coordinador_hash = '<salida de GeneradorHashClave>'; SOURCE db/datos-iniciales.sql;` → crear `isw_app` con SELECT, INSERT, UPDATE, DELETE. Variables del servicio: `ISW_DB_PASSWORD`, `ISW_JWT_SECRET` (obligatoria), opcionales `ISW_DB_HOST`, `APP_ADJUNTOS_DIRECTORIO`, `ISW_LOG_FILE`.

**Pruebas:** 2 nuevas en `JwtServiceTest` (sin secreto y obligatorio → no arranca; con secreto y obligatorio → funciona). **Total: 177 pruebas, 0 fallos** (las 175 de antes siguen igual). Prueba real en Windows contra MySQL local (2026-10-11, después se restauró la demo con `.\db\bd.ps1`):
- `schema.sql` + `datos-iniciales.sql` sin variables o con correo `@demo.example`: error 3819 y 0 filas cargadas. Con variables: 5 roles, 6 asignaturas, 3 dimensiones, 20 indicadores, 1 usuario (el coordinador), 0 usuarios `@demo.example`.
- Arranque con `prod` y `ISW_JWT_SECRET`: `/swagger-ui.html`, `/swagger-ui/index.html` y `/v3/api-docs` → 404; login del coordinador con la clave definida al instalar → 200 y su token lee las 6 asignaturas; login de un usuario de demo → 401; consola vacía y todo el registro en el archivo de `ISW_LOG_FILE`.
- Arranque con `prod` y sin `ISW_JWT_SECRET`: termina con código 1 y el mensaje "Falta la variable de entorno ISW_JWT_SECRET..." en consola y en el log; nunca aparece "secreto temporal".

**Falta (siguientes puntos de `DESPLIEGUE.md` §4; actualizado el mismo 11-oct: HU-26, el límite de login, `deploy/install.sh` y la prueba en Linux ya están hechos, ver más abajo):** HU-26 cambiar la contraseña (el coordinador inicial debería cambiar la suya al entrar), límite de intentos de login, script de instalación para Linux que automatice los pasos de arriba (hoy son manuales) y la carpeta `deploy/` (servicio `systemd`, Nginx, respaldo). `datos-iniciales.sql` duplica la PARTE 1 de `seed.sql`: si cambia el catálogo, cambiar los dos. No se probó en Linux todavía (rutas `/var/...`, permisos de la carpeta de logs y de adjuntos para el usuario del servicio).

## Sprint 2: HU-26 cambiar la contraseña y límite de intentos de login (2026-10-11)

**HU-26:** `PUT /api/auth/password` (cuerpo `CambioPasswordRequest`: `passwordActual`, `passwordNueva`), cualquier rol autenticado y solo su propia contraseña (el usuario sale del token, no del cuerpo). Respuestas: 204 si se cambió; 400 si la actual no es correcta, si la nueva no cumple la misma regla que al crear un usuario (`@NotBlank @Size(min = 8, max = 72)`) o si es igual a la actual; 401 sin token. La nueva se guarda solo como hash BCrypt. En el front-end, "Mi cuenta" (`js/vistas/cuenta.js`) tiene el formulario con la nueva repetida; las claves se envían sin recortar espacios, igual que en el login. Hacía falta una pantalla porque con el perfil `prod` Swagger está apagado. **44 endpoints.**

**Límite de intentos:** `BloqueoLoginService` cuenta los fallos **consecutivos** por correo (normalizado). Al llegar a `app.login.intentos-maximos` (5) el correo queda bloqueado `app.login.bloqueo-minutos` (15): durante el bloqueo el login responde **429** con la cabecera `Retry-After` y un mensaje con los minutos que faltan, **aunque la clave sea correcta**. Un login correcto reinicia la cuenta; al vencer el bloqueo, el correo vuelve a tener todos sus intentos. Los parámetros están en `application.properties` (`LoginProperties`; valores menores que 1 impiden arrancar). Se cuenta también un correo que no existe, así el 429 no revela qué correos están registrados. Equivocarse en la clave actual al cambiarla (HU-26) también cuenta como intento fallido. El front-end no cambió para esto: el login ya muestra el mensaje del servidor en los códigos que no son 400 ni 401.

**Pruebas (15):** `BloqueoLoginServiceTest` (4, con un reloj que se adelanta), `AuthServiceTest` (+6: bloqueo con la clave correcta, correo inexistente, cambio correcto, actual incorrecta, nueva igual a la actual y fallos al cambiarla) y `HU26CambioPasswordIntegracionTest` (5: cambio y login con la nueva, actual incorrecta, reglas de la nueva, 401 sin token y los 5 roles llegan al servicio, 429 con `Retry-After` tras 5 fallos). **Total: 192 pruebas, 0 fallos.** Durante las pruebas se corrigió un redondeo que decía "16 minutos" en un bloqueo de 15. El formulario de "Mi cuenta" todavía no se ha probado en un navegador.

**Falta:** recuperar la contraseña olvidada (no se pidió; hoy la restablece el coordinador... que tampoco tiene endpoint para eso), invalidar los tokens anteriores al cambiar la contraseña (S-50) y guardar los intentos en la base si algún día hay más de una instancia (S-49).

## Sprint 2: versión mínima de MySQL y prueba de "Mi cuenta" en el navegador (2026-10-11)

**Versión mínima: MySQL 8.0.16.** Desde esa versión MySQL evalúa los CHECK (antes los acepta y los ignora sin error) y existe `ALTER TABLE ... DROP CHECK` (migración 001). La collation `utf8mb4_0900_ai_ci` exige 8.0.1 o más, Hibernate 6.6 exige 8.0 y Connector/J 9.7.0 soporta MySQL 8.0 en adelante: MySQL 5.7 y MariaDB no sirven.
- **Pedirle a TI de la UBB específicamente MySQL 8.4 LTS, no 8.0:** la serie 8.0 salió del soporte de Oracle en abril de 2026. El desarrollo usa 8.4.9.
- `db/VersionMysql.ps1` (nuevo): `$VersionMinimaMysql` y `Test-VersionMysql`. `bd.ps1` lo carga y, justo después de conectarse y **antes de tocar la base**, se detiene con un mensaje claro si la versión es menor. También dejó de depender de la ruta fija `MySQL Server 8.4`: busca el cliente en el PATH o en cualquier `MySQL Server X.Y` instalado.
- `db/datos-iniciales.sql`: el mismo control en SQL, al comienzo. No usa un CHECK (justo lo que se ignora en versiones viejas): si la versión no alcanza, intenta un `sql_mode` inválido y el script se detiene con `Variable 'sql_mode' can't be set to the value of 'ERROR_se_necesita_MySQL_8_0_16_o_superior'`, sin cargar nada.
- `db/pruebas-version-mysql.ps1` (nuevo, prueba unitaria sin un MySQL viejo): 14 casos para `Test-VersionMysql` y 12 para el control de `datos-iniciales.sql`, que toma del archivo el texto real del control, cambia `VERSION()` por cada versión de ejemplo y lo ejecuta en el MySQL local como `isw_app`. Casos: 8.0.15, 8.0.4-rc, 5.7.44, MariaDB 10.11 y 11.4 se detienen; 8.0.16, 8.0.16-log, 8.0.36 de Ubuntu, 8.4.0, 8.4.9 y 9.1.0 pasan. **26 de 26 OK.** Se ejecuta con `.\db\pruebas-version-mysql.ps1` (no es parte de `mvn package`).
- Los `.ps1` nuevos se guardaron en UTF-8 **con BOM**: sin él, Windows PowerShell 5.1 lee mal las tildes y no puede analizar el script.

**"Mi cuenta" probada en Chrome con los 5 roles de demostración** (coordinador, guía, profesor del curso, estudiante y jefe de carrera): con la clave actual equivocada se ve el error, con la correcta se ve el éxito, después la clave vieja da "Correo o contraseña incorrectos." y la nueva entra. También se probaron las claves nuevas que no coinciden y el bloqueo (al 6.º intento se ve el mensaje de 15 minutos, en "Mi cuenta" y en el login). Al terminar se restauró la base con `.\db\bd.ps1`. Lo que se corrigió por no entenderse sin explicación:
- **El jefe de carrera no podía abrir "Mi cuenta"** (el menú del front-end no le permitía la ruta): `menu.js` ahora le da "Mi cuenta" y un acceso rápido (antes su inicio mostraba "Accesos rápidos" vacío). Su rol aparecía como `JEFE_CARRERA`: ahora "Jefe de carrera" (`ui.js`) y con su descripción en "Con tu rol puedes" (`datos.js`).
- Error de clave actual: además de la alerta, el campo queda marcado en rojo con "No es tu contraseña actual…", y se vacía. Igual con "debe ser distinta de la actual" en el campo de la nueva.
- Éxito: el aviso era un mensaje chico en una esquina que desaparecía a los 4 segundos. Ahora queda un aviso verde dentro del formulario ("Tu contraseña se cambió correctamente. Desde ahora ingresa con la contraseña nueva…"), más el mensaje de la esquina.
- La pantalla se titulaba "Iniciar sesión con roles" y mostraba "token JWT", `sessionStorage`, "responde 403" y `GET /api/roles`: ahora se titula "Mi cuenta", con textos para usuarios. "Repite la contraseña nueva" quedó al lado de "Contraseña nueva".
- La descripción del rol PROFESOR_CURSO decía "(supuesto S-05)" en pantalla: se cambió en `seed.sql` y `datos-iniciales.sql` a "...que autoriza las clases planificadas" (el supuesto sigue en el backlog).

**Pendiente encontrado (resuelto el mismo 11-oct con la pantalla "Cartola de prácticas"):** el jefe de carrera no tiene en la página ninguna forma de pedir la **cartola de prácticas** (HU-31 solo tiene el endpoint `GET /api/reportes/cartola-practicas`; con Swagger apagado en producción no la puede obtener). Falta una pantalla con las dos fechas y el botón de descarga.

## SUPUESTOS del Sprint 2 (2026-10-10; ninguno está confirmado)

| ID | Supuesto | Historia |
|---|---|---|
| S-48 | Las inscripciones cerradas antes de HU-31 no tenían fecha de cierre: la migración 003 y `db/seed.sql` la deducen del periodo (el semestre 1 cierra el 31 de julio y el 2 el 31 de diciembre). Desde el 10-oct, cerrar una inscripción guarda la fecha real del día | HU-31 |
| S-49 | Límite de login: 5 fallos seguidos y 15 minutos de bloqueo, por correo (valores elegidos por el equipo, configurables). El registro vive en memoria: se pierde al reiniciar y no sirve con varias instancias de la aplicación (hoy hay una sola). Bloquear por correo permite que alguien bloquee a otro a propósito escribiendo mal su clave; se aceptó por ser un sistema pequeño y con bloqueo temporal | Límite de login |
| S-51 | HU-06: una pauta cuenta como "usada" desde que existe una evaluación con ella, aunque sea un borrador (conservador: así un borrador nunca ve cambiar sus indicadores) | HU-06 |
| S-52 | HU-06: al crear una versión nueva, la anterior queda inactiva y la nueva hereda el estado activa/inactiva; no hay dos versiones activas de la misma pauta. Borrar una versión nueva sin uso "deshace" la edición | HU-06 |
| S-53 | HU-06: la pauta se edita completa (todas sus dimensiones e indicadores en una sola petición) y los indicadores se numeran solos; no hay edición de un indicador suelto. Los textos de los indicadores no se pueden "corregir" en una pauta usada sin crear una versión (una errata también versiona) | HU-06 |
| S-54 | (2026-10-16) El estudiante puede agregar documentos a su planificación en cualquier estado, también en revisión o ya autorizada (así funciona hoy; nadie lo pidió ni lo prohibió). Pendiente de preguntar | HU-04 / HU-21 |
| S-55 | (2026-10-16, HU-07) El número de observación (1.ª o 2.ª) es un dato de la **pauta**, no de la evaluación: en la carrera la primera y la segunda observación son instrumentos distintos (133/126 y 161 puntos). Cada evaluación lo muestra a través de su pauta. Las pautas de Especialidad y Orientación lo exigen; la de observación de clase no lo lleva. No se impide evaluar dos veces con la misma pauta (¿debería haber como máximo una evaluación por pauta e inscripción?) | HU-07 |
| S-56 | (2026-10-16, HU-07) Los indicadores de las pautas cargadas se numeran corrido (1 a 19), como lo hace el editor de HU-06; la pauta impresa numera de nuevo en cada dimensión (A1..A4, B1..B4...). La letra queda en el nombre de la dimensión ("A. Conocimiento y preparación de la enseñanza") | HU-07 |
| S-57 | (2026-10-16, HU-07) El tutor se asigna, cambia o quita solo con la inscripción EN_CURSO; las evaluaciones que ya hizo un tutor anterior quedan a su nombre (como el cambio de guía de la migración 004) | HU-07 |
| S-58 | (2026-10-16, HU-07) El tutor ve, solo lectura, las inscripciones, planificaciones, evaluaciones finalizadas (y sus propios borradores) e historial completo de los estudiantes de los que es tutor (como S-21 para guía y curso). No participa en el flujo de aprobación de planificaciones | HU-07 |
| S-50 | Al cambiar la contraseña (HU-26) el token en uso sigue valiendo hasta que expire (8 horas): el JWT no tiene estado y revocarlo exigiría guardar una versión del token por usuario | HU-26 |
| S-59 | (2026-10-26, HU-15; proveedor cambiado el mismo día por decisión del usuario: antes Anthropic, ahora Groq) El modelo de lenguaje lo provee **Groq** (capa gratuita, API compatible con "chat completions" de OpenAI; modelo en `app.chatbot.modelo`), con la clave solo en la variable de entorno `ISW_GROQ_API_KEY`. Lo eligió el equipo; el cliente no ha confirmado proveedor ni costo (S-46). La capa gratuita tiene límites de uso por minuto y por día: con muchos estudiantes a la vez el chatbot respondería "alcanzó su límite de uso" (503). **Privacidad:** cada mensaje envía a ese proveedor externo el nombre del estudiante, su historial de prácticas (notas, promedios) y la planificación con sus documentos. Hay que confirmarlo con el cliente antes de usarlo con estudiantes reales | HU-15 |
| S-60 | (2026-10-26, HU-15/HU-16) El chat lo leen **solo el estudiante dueño y el profesor guía de esa inscripción**. No lo leen el coordinador, el profesor del curso, el tutor ni el jefe de carrera (este último llega con HU-20). Es la opción más privada mientras S-38 siga abierto. El estudiante ve un aviso con el nombre de su guía (HU-16, criterio 2) | HU-15, HU-16 |
| S-61 | (2026-10-26, HU-15) "Adjuntar la planificación" = **elegir una de sus planificaciones del sistema** en el chat: el chatbot lee el formulario y los documentos ya subidos (HU-21). Word y PDF se convierten a texto, porque Groq solo recibe texto (se pierden imágenes, gráficos, tablas complejas y ecuaciones de Word; un PDF escaneado no tiene texto). Todos los documentos juntos se recortan a 14 000 caracteres por la cuota de tokens. No se sube un archivo nuevo en el chat ni se aceptan imágenes (.png/.jpg), porque las planificaciones solo aceptan Word y PDF. Si no eligió ninguna, el chatbot pide que la elija (criterio 2) | HU-15 |
| S-62 | (2026-10-26, HU-15) Las reglas del flujo de RetroPlani Mate (verificación documental, máximo 5 preguntas, no diseñar, nunca poner nota, máximo 2 retroalimentaciones por clase) van en las **instrucciones del modelo**: el código no puede garantizarlas al 100 %. El texto del mensaje fijo de límite (criterio 3) lo redactó el equipo (la propuesta original no está en el repositorio) | HU-15 |
| S-63 | (2026-10-26, HU-15) Se escribe al chatbot solo con la inscripción EN_CURSO; cerrada, la conversación queda para consulta (409 al escribir) | HU-15 |
| S-64 | (2026-10-26, HU-15) "Asignaturas aprobadas" del contexto = las **prácticas** aprobadas registradas en el sistema (S-14), no la malla completa de la carrera | HU-15 |
| S-65 | (2026-10-26, HU-17) El texto base del chatbot es la propuesta **"RetroPlani Mate" V3** del coordinador de prácticas (`Documents/ChatBot_extraido/ChatBot/PROPUESTA_DE_GPT_CHATBOT_RETROPLANI_MATE_V3_21-06 - Copia.pdf`, que no está en git) **resumida y adaptada por el equipo**: "adjuntar" = elegir la planificación en el sistema, solo Word o PDF (sin imágenes), y 9 de sus 10 respuestas modelo (la del cierre reflexivo quedó como regla del flujo). Se resumió porque el texto completo (~25 páginas) no cabe en la cuota por minuto de Groq. Reemplaza el mensaje fijo de límite que había redactado el equipo (S-62) por el de la propuesta. Las bases curriculares, la rúbrica y los demás archivos de referencia no se cargan (HU-29) | HU-15, HU-17 |
| S-66 | (2026-10-26, HU-20; ajustado en el cierre final) El jefe de carrera lee **todos** los chats de las asignaturas con chatbot, en solo lectura. **Desde el cierre final, el estudiante ve un aviso** de que el jefe puede leer y cuándo lo hizo por última vez (criterio 3), y **cada lectura queda en la tabla `lectura_chat_jefe_carrera`** (migración 010, criterio 4); antes no había aviso y la lectura quedaba solo en el log. No se registran las lecturas del guía. Sigue abierta la privacidad de los chats con el cliente (S-38) | HU-20 |
| S-67 | (2026-10-26, HU-17) Las instrucciones calibradas miden entre **200 y 12 000 caracteres** (400 si no): el mínimo evita guardar un texto vacío o de prueba; el máximo protege la cuota de Groq (el texto viaja en cada mensaje; el base tiene ~7 000) | HU-17 |
| S-68 | (2026-10-26, HU-17) Un solo texto para todo el sistema (no por asignatura). El cambio rige desde el **siguiente mensaje**, también en conversaciones en curso; los mensajes guardados no cambian (criterio 3). No se guarda historial de versiones: solo quién y cuándo editó por última vez. El contexto del estudiante lo sigue agregando el sistema, no se edita | HU-17 |
| S-69 | (2026-10-26, HU-29) Un documento vigente por tipo, con los 10 tipos de la lista de la propuesta RetroPlani Mate; al reemplazarlo, el archivo anterior queda en disco | HU-29 |
| S-70 | (2026-10-26, HU-29) Búsqueda por palabras con tope de 5 000 caracteres por mensaje, **sin embeddings** (la capa gratuita de Groq no los ofrece). Desde el cierre final el orden es TF-IDF (S-74), pero sigue sin ser búsqueda por significado | HU-29 |
| S-71 | (2026-10-26, HU-30) ~~La Escala SUS y la rúbrica de los jueces van fuera del sistema~~: **reemplazado por S-75** en el cierre final | HU-30 |
| S-72 | (2026-10-26, HU-15 criterio 2) Un mensaje "parece una planificación pegada" si tiene más de 400 caracteres y nombra al menos 2 partes distintas de una planificación (objetivo, recursos, actividad, evaluación, DUA, inicio, desarrollo, cierre, indicador, habilidad, OA, minutos). Solo se aplica si no hay planificación adjunta en la conversación; la respuesta es fija y no se llama al modelo; los dos mensajes se guardan pero no se reenvían al modelo | HU-15 |
| S-73 | (2026-10-26, HU-20) El estudiante ve la fecha de la **última** lectura del jefe, no la lista completa ni quién la hizo; la lista completa queda en la base para auditoría | HU-20 |
| S-74 | (2026-10-26, HU-29) El orden de los fragmentos es TF-IDF con coseno (TF = 1 + ln veces, IDF de BM25) y las palabras cuentan desde 3 letras (para "DUA") | HU-29 |
| S-75 | (2026-10-26, HU-30) La Escala SUS adaptada y la rúbrica de juicio experto (textos de la propuesta RetroPlani Mate V3, no inventados) las responde dentro del sistema cualquier **estudiante validador**, una vez cada una, con la validación abierta o cerrada; un juez experto usaría una cuenta de estudiante validador. En la propuesta son para los jueces; el instrumento de usuarios (formato de Andrés Seguel) no está en el material y no se construyó. No se guarda la identificación del juez (filiación, firma) | HU-30 |

## Problemas conocidos y limitaciones

- **Las pruebas de integración asumen los datos exactos de `db/seed.sql`** (cuentan filas). Si la base tiene datos extra (por ejemplo tras probar a mano en Swagger), fallan algunas: reinstala con `.\db\bd.ps1` y repite. Las pruebas en sí no modifican la base (rollback).
- Un estudiante de Investigación en el Aula no se puede evaluar todavía: solo hay pauta para Trabajo Colaborativo (S-07) y, desde HU-07 (16-oct), las de Especialidad 1.ª observación de Práctica Profesional.
- Si el cliente confirma que el flujo de planificación es en paralelo (S-37) hay que rehacer la máquina de estados de HU-04.
- Sin control de concurrencia optimista (`@Version`): dos modificaciones simultáneas del mismo registro se resuelven por orden de llegada (la base sigue impidiendo estados inválidos).
- Sin recuperación de contraseña olvidada, refresco de token ni paginación (el cambio de contraseña, HU-26, y el límite de intentos de login se agregaron el 11-oct).
- Swagger UI y `/v3/api-docs` están abiertos en desarrollo (necesario para la demo); con el perfil `prod` (2026-10-11) quedan desactivados.
- En desarrollo el secreto del JWT es temporal si no se define `ISW_JWT_SECRET`: tras reiniciar la app hay que iniciar sesión de nuevo. Con el perfil `prod` es obligatorio (2026-10-11).
- **Fase E:** los archivos se guardan en el disco local del servidor (`backend/adjuntos/`), no en un almacenamiento externo; en un despliegue con varias instancias habría que moverlos a un storage compartido. La transcripción de la nota de voz depende del navegador del evaluador (Web Speech API): no funciona en todos los navegadores (S-40). No se probó todavía en un navegador real por falta de esa herramienta en esta sesión (ver "Cómo se verificó lo del 22-sep" arriba); falta ese recorrido visual antes de la defensa.

- **Chatbot (HU-29, criterio 2):** que responda solo con los documentos de la base de conocimiento lo cumple el modelo por instrucciones; la búsqueda de fragmentos es por palabras (TF-IDF, S-70 y S-74), no por significado. Limitación conocida de la arquitectura, no pendiente de código.

## Pendientes y mejoras (para sprints siguientes; nada bloquea la entrega)

- Confirmar con el cliente/profesora los supuestos S-01 a S-46 (sobre todo S-05, S-09, S-10, S-21, S-29, S-40, S-45).
- Sprint 2: configurar pautas (HU-06), pautas de Práctica Pedagógica/Profesional y tutor (HU-07), informes (HU-08), nota final (HU-09), duplas (HU-10), cambiar la contraseña (HU-26), gestión de archivos de pautas formales (HU-25), configurar parámetros del sistema (HU-27), exportar el historial en PDF (HU-28).
- Sprints 3 y 4: panel del coordinador, chatbot y su base de conocimiento (usará `GET /api/estudiantes/{id}/historial` como contexto; HU-15 a HU-17, HU-29, HU-30).
- Agregar `@Version` en `Planificacion` y `Evaluacion` y paginación en los listados.
- Migrar `Planificacion` al formato oficial real (S-45), cuando se decida el impacto en el formulario y en las 146 pruebas que ya lo usan.

## Migraciones de base de datos

**`db/migraciones/011_escala_sus_y_rubrica_experto.sql`** (2026-10-26, HU-30): tablas `respuesta_sus` (10 ítems de 1 a 5,
puntaje SUS generado por la base), `respuesta_rubrica_experto` (4 preguntas abiertas) y `respuesta_rubrica_criterio` (9
criterios de 1 a 4 con comentario), una SUS y una rúbrica por validador (UNIQUE).

**`db/migraciones/010_lectura_chat_jefe_carrera.sql`** (2026-10-26, HU-20): tabla `lectura_chat_jefe_carrera`
(inscripción, usuario, fecha), una fila por cada lectura del jefe de carrera. **010 y 011 probadas sobre una base sin
ellas:** terminan sin errores y las restricciones quedan iguales a una instalación nueva (CHECK 47, FK 35, PK 22, UNIQUE 18).

**`db/migraciones/009_base_conocimiento_y_validacion.sql`** (2026-10-26, HU-29 y HU-30): tabla
`documento_base_conocimiento`, `estudiante.validador_chatbot` e `instrucciones_chatbot.validacion_abierta`.

**`db/migraciones/008_instrucciones_chatbot.sql`** (2026-10-26, HU-17): tabla `instrucciones_chatbot` (una fila).

**`db/migraciones/007_chatbot_planificacion.sql`** (2026-10-26, HU-15): columna `mensaje_chatbot.planificacion_id`
(nula, FK a `planificacion`, índice): la planificación que el estudiante adjuntó a ese mensaje. La tabla
`mensaje_chatbot` ya existía desde el Sprint 1 (vacía, sin API). Que la planificación sea de la misma inscripción lo
controlan `ChatbotService` y el bloque 24 nuevo de `verificacion.sql`. Una sola vez y como root; `schema.sql` ya la trae.
**Probada solo en una base nueva** (`.\db\bd.ps1`); en el servidor real todavía no hay base que migrar.

**`db/migraciones/001_grupo_y_planificacion_por_asignatura.sql`** (2026-09-20, por las respuestas de un estudiante): agrega la modalidad `GRUPO` al CHECK de `asignatura_practica` y la columna `requiere_planificacion` (BOOLEAN, por defecto TRUE); Aproximación pasa a `GRUPO` sin planificaciones y Ambientes a `DUPLA`.
- Se ejecuta **una sola vez y como root** (`isw_app` no puede alterar tablas). Probada sobre la base anterior y comparada con una instalación nueva: el resultado es idéntico.
- `db/schema.sql`, `db/seed.sql` y `db/verificacion.sql` ya incluyen el cambio: una base nueva (`.\db\bd.ps1`) no necesita la migración.
- `verificacion.sql`: el control "EN_AUTORIZACION sin aprobación del guía" ignora las inscripciones sin guía y hay un control nuevo (planificaciones en asignaturas que no las usan).

**`db/migraciones/002_documentos_adjuntos.sql`** (2026-09-22, Fase E): agrega las tablas `documento_planificacion` y `documento_evaluacion` (no toca `nota_voz`, que ya existía con todas las columnas que necesitaba la API). Igual que la 001, se ejecuta una sola vez y como root; `db/schema.sql` ya la incluye para una base nueva.

**`db/migraciones/006_tutor_y_numero_de_observacion.sql`** (2026-10-16, HU-07): rol `TUTOR` (id 6),
`asignatura_practica.lleva_tutor` (solo PEM-P6, con CHECK: solo donde se va a un colegio), `inscripcion.tutor_id` (FK,
CHECK distinto del guía y del profesor del curso, índice) y `pauta.numero_observacion` (CHECK: nulo en observación de
clase, 1 o 2 en Especialidad y Orientación). Las pautas de Especialidad u Orientación creadas antes con HU-06 quedan como
1.ª observación. **Probada sobre una base sin la 006** (la de desarrollo con la 006 deshecha a mano y una pauta de
Especialidad sin número agregada): termina sin errores, la pauta queda con 1 y las restricciones son las mismas que en
una instalación nueva (CHECK 40, FK 27). `db/schema.sql`, `db/seed.sql`, `db/datos-iniciales.sql` y
`db/verificacion.sql` ya la incluyen.

**`db/migraciones/005_versiones_de_pauta.sql`** (2026-10-11, HU-06): columnas `pauta.version`, `pauta.version_anterior_id` y `pauta.fecha_creacion`, unicidad (asignatura, nombre, versión), FK y UNIQUE de la versión anterior y CHECK de la versión 1. **Probada sobre la base de desarrollo con datos** (la pauta existente quedó como versión 1) y luego con una base nueva. `db/schema.sql` ya la incluye.

**`db/migraciones/004_rol_secundario.sql`** (2026-10-11, confirmado por el cliente): columna `usuario.rol_secundario_id` con su FK y CHECK, nueva descripción del rol `JEFE_CARRERA` y, solo en la base de demo (condicionado al correo de demo), el segundo rol del jefe y la inscripción 40. `db/schema.sql` y `db/seed.sql` ya la incluyen. **No se ejecutó sobre una base antigua**: se probó con una base nueva (`.\db\bd.ps1`).

**`db/migraciones/003_jefe_carrera_y_fecha_cierre.sql`** (2026-10-10, HU-31): agrega el rol `JEFE_CARRERA` (id 5), la columna `inscripcion.fecha_cierre` con su CHECK e índice, y les pone fecha de cierre a las inscripciones ya cerradas (S-48). Probada sobre la base del último commit (5 roles, 35 cerradas con fecha y 20 en curso sin fecha, 35 CHECK); `db/schema.sql` y `db/seed.sql` ya la incluyen. `verificacion.sql` suma el bloque 20 y `bd.ps1` una prueba negativa más (24 en total).

**Fase F (2026-09-22): sin migración nueva.** Solo se amplió `db/seed.sql` (más planificaciones y evaluaciones) y `db/verificacion.sql` (bloque 18 y 19); el esquema no cambió.

## Cómo se probó (para repetirlo)

1. `.\db\bd.ps1` (base limpia) → `cd backend` → `mvn clean package` (159 pruebas).
2. Arrancar `java -jar target\practicas-0.1.0-SNAPSHOT.jar` y probar con Swagger según `docs/GUIA_DEFENSA.md`.
3. **Fase E (22-sep):** además, con la app arrancada y sin navegador disponible, se probó por HTTP con `curl -F` (multipart): login, `POST` y `GET` de documentos en una planificación y en una evaluación, descarga con las cabeceras `Content-Disposition` correctas, 403 de quien no corresponde; `POST`, `GET` y `PUT` de nota de voz, `GET` del audio, 409 al grabar una segunda vez y 403 de otro guía. Después se reinstaló la base (`.\db\bd.ps1`) para dejarla limpia.
4. **Fase F (22-sep):** `.\db\bd.ps1` → revisar el bloque 18 y 19 de `db/verificacion.sql` (0 violaciones, actividad por guía y por profesor del curso) → `mvn clean package` (159 pruebas) → arrancar la app y comprobar por HTTP (`GET /api/evaluaciones` y `GET /api/planificaciones` como coordinador) que aparecen 10 y 14 estudiantes distintos. Se reinstaló la base al final para dejarla limpia.
5. **HU-31 (10-oct):** `.\db\bd.ps1` (bloque 20 con 0 violaciones, 24 de 24 pruebas negativas) → `mvn clean package` (175 pruebas) → arrancar la app y probar por HTTP con `curl`: login como `jefe.carrera@demo.example` y `GET /api/reportes/cartola-practicas?desde=2025-01-01&hasta=2025-12-31` da 200 con `application/pdf` y el archivo adjunto; los otros 4 roles, 403; fechas invertidas, 400 con mensaje; el jefe en `/api/estudiantes`, 403; Swagger lista 43 operaciones. La migración 003 se probó sobre la base del último commit. Se reinstaló la base al final.

## Sprint 2: ronda de pruebas de los 5 roles en el navegador (2026-10-11, primera pasada)

**Cómo se hizo:** la extensión de Chrome no estaba conectada, así que se usó **Chrome headless manejado por el
protocolo DevTools** (un script de Python en la carpeta temporal de la sesión, fuera del repositorio): entra con cada
usuario de demo por el formulario de login, recorre cada ítem del menú y algunas pantallas de detalle, guarda capturas
y registra los errores de JavaScript y las alertas de error visibles. Después se reinstaló la base con `.\db\bd.ps1`.

**Resultado general:** los 5 usuarios entran y ven exactamente su menú; **ningún error de JavaScript** salvo el
esperado al abrir algo sin permiso (ver abajo). Lo nuevo del jefe de carrera quedó probado en la página: cabecera
"Jefe de carrera · Profesor guía", menú con Cartola + Inscripciones + Evaluaciones + Planificación + Historial, la
cartola se descarga (y con fechas invertidas marca el campo «Hasta»), ve la planificación de Diego (de otro guía) sin
botones de decisión, ve el historial de cualquier estudiante, y **solo** en la inscripción de Isidora aparece «Evaluar»;
se creó una evaluación de Isidora desde la página sin problemas. "Pautas de evaluación" le da "Sin permiso" (correcto:
es del coordinador). En ancho de celular (390 px) la página no se desborda.

**Para revisar juntos (no se tocó nada de esto; ninguno bloquea):**
1. **Estudiante · Mis evaluaciones:** el subtítulo dice "Solo ves las evaluaciones finalizadas **de los estudiantes que
   tienes asociados**", que es el texto para profesores; para el estudiante debería decir algo como "Ves tus evaluaciones
   finalizadas". Además se le ofrece el filtro "Estado: Borrador", que para él nunca trae nada.
2. **Abrir una evaluación ajena** (estudiante que entra por URL a `#/evaluaciones/1`): muestra "No tienes permiso para
   esta acción. No tienes acceso a esta evaluación." Se entiende, pero repite la idea dos veces; y en la consola queda un
   `ApiError` (lo registra `app.js` a propósito con `console.error`). Normalmente no se llega ahí desde el menú.
3. **Barra lateral achicada** (botón ☰ en escritorio): el título "ACCESOS RÁPIDOS" se corta ("ACCESC / RÁPIDC") porque
   no cabe en el ancho de los íconos. Convendría ocultarlo o cambiarlo por una línea divisoria en ese modo.
4. **Evaluación en borrador (guía):** la barra fija de abajo ("6 de 20 indicadores · Guardar avance · Finalizar
   evaluación") queda encima del bloque de la nota de voz y se alcanza a ver el botón "Guardar nota de voz" detrás de
   ella, como fantasma. Al desplazarse se ve bien, pero en la primera vista confunde.
5. **Celular:** las tablas largas (inscripciones, evaluaciones) se desplazan de lado dentro de su recuadro; la página no
   se rompe, pero en un teléfono cuesta leer las columnas de la derecha (acciones). Para la demo del 28 conviene mostrar
   en un computador.
6. **Jefe de carrera · Evaluaciones:** como también es guía, el título dice "Evaluar una clase con la pauta digital..."
   con una línea extra "Como jefe de carrera ves además todas las demás, en modo lectura". Funciona, pero hay que decidir
   si preferimos un título neutro ("Evaluaciones de clases") cuando la cuenta tiene los dos roles.
7. **Mi cuenta (todos):** "Con tu rol puedes" está en singular aunque el jefe tenga dos roles (se listan los permisos de
   ambos). Detalle de redacción.

Quedó pendiente de esta ronda (no alcanzó el tiempo): recorrer los flujos completos de punta a punta en la página
(estudiante sube documento y envía → guía observa → estudiante corrige → guía aprueba → curso autoriza; guía completa y
finaliza una evaluación con nota de voz). Esos flujos sí están cubiertos por las pruebas de integración.

## Resumen del tramo autónomo del 2026-10-11 (tareas 0 a 3)

| Tarea | Estado | Cómo se comprobó |
|---|---|---|
| 0. Cerrar lo del jefe de carrera (segundo rol, lectura ampliada, menú) | **Hecha.** Faltaba solo verla en el navegador | Ronda de la Tarea 3 con `jefe.carrera@demo.example` (arriba) |
| 1. Carpeta `deploy/` | **Hecha y probada** | Instalación real en Ubuntu 26.04 (WSL): reinicio de systemd tras `kill -9`, 30 MB en nginx, 80 → 443, respaldo con fecha y restaurado, reinstalación idempotente. Ver "Sprint 2: carpeta deploy/" |
| 2. HU-06 Configurar pautas | **Hecha y probada** | 9 pruebas de integración nuevas + pantalla del coordinador recorrida en el navegador |
| 3. Ronda de los 5 roles | **Primera pasada hecha** | 7 observaciones anotadas arriba para revisar juntos |

- **Pruebas:** **210**, todas pasan (`.\db\bd.ps1` y luego `mvn clean package`, corrido antes de pasar de una tarea a la
  siguiente y al final). Eran 201 al empezar el tramo: +9 de HU-06.
- **Endpoints:** **49** (44 + 5 de HU-06). **Migraciones:** 5 (la nueva es `005_versiones_de_pauta.sql`).
- **Nada quedó roto ni bloqueado.** No se hizo ningún `git add`, `commit` ni `push`.
- **Supuestos nuevos:** S-51 (una pauta cuenta como usada aunque solo tenga un borrador), S-52 (la versión anterior queda
  inactiva; borrar una versión nueva sin uso deshace la edición), S-53 (la pauta se edita completa; una errata en un
  indicador de una pauta usada también crea versión). Están en "SUPUESTOS del Sprint 2".
- **Decisiones que dependen de ustedes o del cliente (se tomó la opción conservadora):**
  - HU-06: ¿corregir una errata en una pauta ya usada debería poder hacerse sin crear una versión? Hoy no (S-53).
  - HU-06: las pautas de tipo `TUTOR` se pueden crear, pero nadie puede evaluar con ellas hasta que exista el rol de tutor
    (HU-07, fuera de este tramo).
  - Despliegue: el certificado definitivo (TI o Let's Encrypt), la opción A o B de la base y dónde se guardan los respaldos
    fuera del servidor siguen siendo preguntas para TI (`DESPLIEGUE.md`, sección 6).
- **Quedó instalado en el Ubuntu de WSL** (para las pruebas de `deploy/`): Java 17, MySQL 8.4, nginx y el servicio
  `isw-practicas`, todos **detenidos y deshabilitados** para no ocupar puertos de Windows.

## Sprint 2: flujos de punta a punta en la página (2026-10-16, Tarea 0)

**Cómo se hizo:** igual que el 11-oct, Chrome headless manejado por el protocolo DevTools (script de Python en la
carpeta temporal de la sesión, fuera del repositorio), con el micrófono simulado de Chrome
(`--use-fake-device-for-media-stream`). Base limpia con `.\db\bd.ps1` antes y después. Se usó la inscripción 3
(Camila Rojas · Trabajo Colaborativo 2026-2, guía Marcos Bravo, profesor del curso Ricardo Núñez).

**Planificación enviada → observada → corregida → aprobada → autorizada: funciona completo.**
1. Camila llena el formulario y pulsa "Guardar y enviar" **sin documento**: el borrador se guarda, no se envía y aparece
   el error (S-47). Adjunta el PDF en el mismo formulario y vuelve a enviar: pasa a "En revisión del guía".
2. Marcos la ve primera en su lista con "Requiere tu acción". "Devolver" sin comentario marca el campo ("el comentario es
   obligatorio"); con comentario y confirmación pasa a "Con observaciones" y el historial muestra la observación.
3. Camila ve el aviso amarillo con el comentario del guía, en el detalle y en "Editar"; corrige "Tiempos" y reenvía (no
   tuvo que volver a subir el documento).
4. Marcos aprueba con comentario → "En autorización". 5. Ricardo la ve primera en su lista, autoriza → "Autorizada", con
   los 4 pasos marcados y las 3 decisiones en el historial.

**Evaluación completada con nota de voz: funciona completo.** Desde la planificación autorizada, Marcos pulsa "Evaluar
esta clase" (llega con la inscripción elegida), vincula la planificación (se adelantan la fecha y el objetivo), crea la
evaluación, marca 6 indicadores y guarda avance; graba 3 s con el micrófono, detiene, escucha la vista previa; intentar
guardar sin texto avisa "Escribe o corrige la transcripción"; con texto se guarda. "Finalizar" con 6 de 20 resalta los 14
que faltan; con los 20 confirma y queda 6,10 (122 de 140), inmutable. Camila y Ricardo ven la evaluación finalizada, el
audio y la transcripción. **Ningún error de JavaScript** en todo el recorrido.

**Observaciones de este recorrido:**
- **A (ajuste chico, corregido en la Tarea 1):** al enviar sin documento el mensaje dice "faltan estos elementos:
  **[documentoFormal]**" (nombres internos de la API) y queda arriba del formulario, fuera de la vista: el usuario solo ve
  el aviso rojo de la esquina "El borrador se guardó, pero no se pudo enviar".
- **B (ajuste chico, corregido):** el aviso dice "**Revisión del profesor guía dejó observaciones**", que se lee raro.
- **C (ajuste chico, corregido):** "Evaluar esta clase" llega con la inscripción elegida, pero no con *esa*
  planificación vinculada: hay que elegirla de nuevo.
- **D (ajuste chico, corregido):** si el guía escribe la transcripción a mano antes de guardar (o corrige lo que
  reconoció el navegador), la nota queda rotulada "**Transcripción automática, sin corregir**", aunque no lo sea.
- **E (más grande, no se tocó):** el inicio no avisa nada pendiente: Camila no ve en "Inicio" que tiene una planificación
  con observaciones, ni Marcos que tiene una por revisar; solo aparece la marca "Requiere tu acción" dentro de la lista.
  Encaja con el panel del coordinador / notificaciones de sprints siguientes.
- **F (regla de negocio, no se tocó; para preguntar):** el estudiante puede **subir documentos a su planificación en
  cualquier estado**, también en revisión o ya autorizada (el backend no lo limita y la pantalla muestra "Subir
  documento"). ¿Debe poder agregar archivos después de enviarla? Queda como **S-54** (supuesto: sí, se permite).
- **G (no se pudo probar aquí):** la **transcripción automática** necesita una voz real: con el micrófono simulado Chrome
  graba un tono y el reconocimiento no devuelve texto (sin error). Falta probarla a mano con Chrome y micrófono antes
  de la defensa.

## Sprint 2: corrección de las 7 observaciones de la ronda del 11-oct (2026-10-16, Tarea 1)

Solo front-end (JS y CSS); **sin cambios en reglas de negocio, backend ni esquema**. Antes de corregir se pasó una ronda
de "línea base" con Chrome headless para confirmar que cada observación se reproducía.

| # | Observación | Qué se hizo | Archivo |
|---|---|---|---|
| 1 | Estudiante · Mis evaluaciones: subtítulo de profesor y filtro "Borrador" inútil | Subtítulo propio ("Ves tus evaluaciones finalizadas: mientras el profesor guía la completa, la evaluación es un borrador privado y no aparece aquí.") y el filtro Estado se oculta al estudiante | `js/vistas/evaluaciones.js` |
| 2 | Evaluación ajena: "No tienes permiso… No tienes acceso…" repetido y `ApiError` en la consola | Un 403 con motivo concreto del servidor muestra solo ese motivo ("No tienes acceso a esta evaluación."); el genérico sigue siendo "No tienes permiso para esta acción.". A la consola solo van los errores inesperados (sin código HTTP o 5xx) | `js/ui.js` (`describirError`), `js/app.js` |
| 3 | Barra lateral achicada: "ACCESOS RÁPIDOS" cortado | **Ya estaba resuelto en el código** (`app.css:93` convierte el título en una línea divisoria de 1 px en ese modo); se comprobó en la línea base y después | — |
| 4 | Barra fija de la evaluación encima de la nota de voz, con el botón "como fantasma" detrás | La barra era 97 % opaca y dejaba ver lo de abajo: ahora es opaca y con `z-index: 20` (bajo la cabecera, 30, y el menú del celular, 40). Bajo el botón deshabilitado "Guardar nota de voz" se lee "El botón se activa cuando detienes la grabación." (el verde pálido parecía un botón roto) | `css/vistas.css`, `js/notaVoz.js` |
| 5 | Celular: las acciones de las tablas quedaban fuera de la pantalla | En pantallas de 640 px o menos, la columna de acciones (Abrir, Ver, Historial…) queda fija a la derecha con una sombra; el resto se sigue desplazando de lado. A 390 px el botón queda visible en inscripciones, evaluaciones y planificaciones. Una vista "en tarjetas" para el celular sería mejor, pero es un cambio más grande | `css/base.css` |
| 6 | Jefe de carrera · Evaluaciones: título de guía + línea extra | **Se eligió el título neutro** "Evaluaciones de clases" cuando la cuenta es guía y además ve todo, con un subtítulo que explica ambos roles. Es una decisión de redacción: se puede revertir si prefieren otra | `js/vistas/evaluaciones.js` |
| 7 | Mi cuenta: "Con tu rol puedes" en singular con dos roles | "Con tus roles puedes" y "lo que tus roles permiten" cuando hay dos | `js/vistas/cuenta.js` |

**Ajustes chicos de la Tarea 0 (también corregidos):**
- A: el mensaje de elementos faltantes usa nombres para el usuario ("…faltan estos elementos: la planificación formal
  (Word o PDF) adjunta.") en el formulario y en el botón "Enviar a revisión" del detalle, y el formulario se desplaza
  hasta la alerta. El backend no cambió (sigue respondiendo los nombres de la API, que las pruebas comprueban).
- B: "**El profesor guía** dejó observaciones" / "El profesor del curso dejó observaciones".
- C: "Evaluar esta clase" pasa también `planificacionId` y la nueva evaluación llega con esa planificación vinculada
  (y la fecha y el objetivo adelantados).
- D: la nota de voz dice "Transcripción guardada junto con la grabación." o "Transcripción corregida por el evaluador
  después de grabar.", en vez de "automática, sin corregir".

**Comprobación:** después de recompilar, nueva ronda con Chrome headless y los 5 usuarios de demo (coordinador, guía,
profesor del curso, estudiante y jefe de carrera): los 5 entran, recorren **todos** los enlaces de su menú (39 pantallas
distintas en total) **sin errores de JavaScript ni alertas de error**, y cada una de las 7 observaciones da el resultado esperado
(subtítulo y filtro del estudiante, mensaje único y consola limpia, divisoria de 1 px, barra opaca, botón visible a
390 px, título neutro, "Con tus roles puedes" solo con dos roles). Además se repitió completo el recorrido de la Tarea 0
para comprobar A–D. Después: `.\db\bd.ps1` y `mvn clean package`.

## Sprint 2: HU-07 pautas de Práctica Profesional con tutor, primera parte (2026-10-16, Tarea 2)

**Alcance de hoy:** el modelo, el backend y la página mínima para que **un tutor evalúe con su pauta**, y las dos pautas
reales de la **1.ª observación de Especialidad** (colaborador y tutor). Queda **incompleta a propósito** (ver "Falta").

**Base de datos (migración 006, ver "Migraciones"):** rol `TUTOR`, `lleva_tutor` en la asignatura, `tutor_id` en la
inscripción y `numero_observacion` en la pauta. **Las pautas son reales:** se generaron con un script desde los `.docx`
de `Documents/6_Práctica Profesional.rar` → "Pautas de evaluación Práctica Profesional 2025-2" → PRIMERA PAUTA DE
EVALUACIÓN ESPECIALIDAD (COLABORADOR-A y TUTOR-A): escala 2 a 7 sin decimales, dimensiones A-D (A. Conocimiento y
preparación de la enseñanza, B. Convivencia al interior del aula, C. Transposición didáctica, D. Responsabilidad y ética
profesional), 19 indicadores, **puntaje máximo 133, igual que la pauta impresa**. Están en la parte de catálogo de
`seed.sql` y también en `datos-iniciales.sql` (servidor). Demo: `tutor1@demo.example` (Rodrigo Saavedra, id 33) es tutor
de la inscripción 17 (Sebastián Vargas, Práctica Profesional 2026-2). `verificacion.sql`: bloques 6b, 22 y 23 nuevos;
`bd.ps1`: 3 pruebas negativas nuevas (**27 de 27**). Una de ellas encontró un error real: el primer CHECK de
`numero_observacion` dejaba pasar `NULL` (en SQL, `NULL IN (1, 2)` es "desconocido" y MySQL lo acepta); se corrigió con
`IS NOT NULL`.

**Criterios de aceptación:**

| # | Criterio | Estado |
|---|---|---|
| 1 | Escala de 2 a 7 y dimensiones de la A a la E | **Cumplido.** La escala ya era configurable (HU-06); los puntajes fuera de 2-7 dan 400. Las pautas cargadas tienen A-D (la impresa de 1.ª observación tiene 4; las de 2.ª pueden llegar a E) |
| 2 | Cada evaluación indica si es la 1.ª o la 2.ª observación | **Cumplido** vía la pauta (S-55): `EvaluacionDto.pauta` trae `tipoEvaluador`, `ambito` y `numeroObservacion`; la página lo muestra |
| 3 | El tutor solo evalúa con pautas de tutor | **Cumplido.** Tutor + pauta de colaborador = 400; guía + pauta de tutor = 400; tutor en una inscripción donde no es tutor = 403. En "Nueva evaluación" cada uno ve solo sus pautas |
| 4 | El puntaje máximo de cada pauta se calcula con sus indicadores | **Cumplido.** `PautaDto.puntajeMaximo` (nuevo) y `PautaGestionDto.puntajeMaximo` (de HU-06) = indicadores × escala máxima |

**Backend:** `Roles.TUTOR`; `AccesoService` (el tutor ve lo de las inscripciones donde es tutor, S-58);
`EvaluacionService` (evalúa el guía o el tutor asignado, cada uno con su tipo de pauta; las evaluaciones, la nota de voz
y los documentos de evaluación aceptan `TUTOR` en `@PreAuthorize`, y la escritura se sigue comprobando por id del
evaluador); `InscripcionService` (tutor opcional al inscribir y endpoint nuevo **`PUT /api/inscripciones/{id}/tutor`**,
solo coordinador: asigna, cambia o quita; 400 si la asignatura no lleva tutor, si no tiene rol TUTOR o si es el guía o el
profesor del curso; 409 si la inscripción está cerrada); `RolProfesor.TUTOR` (el coordinador registra tutores);
`PautaGuardarRequest.numeroObservacion` (obligatorio en Especialidad y Orientación, prohibido en observación de clase).
**50 endpoints** (49 + 1).

**Front-end:** el tutor entra con su menú (Mis inscripciones, Evaluaciones, Planificación, Historial, Mi cuenta) y sus
accesos rápidos; en sus inscripciones aparece «Evaluar»; "Nueva evaluación" le ofrece solo las pautas de tutor de esa
asignatura. El coordinador ve el tutor en la tabla de inscripciones ("Tutor: ..." o "Sin tutor asignado"), tiene el
botón «Tutor» en las de Práctica Profesional, registra profesores con rol Tutor y el editor de pautas tiene el campo
"Observación" (se muestra y es obligatorio solo en Especialidad u Orientación).

**Pruebas:** `HU07TutorIntegracionTest` (7: pauta 2-7 con A-D y máximo 133; lo que ve el tutor; el tutor evalúa,
finaliza y graba nota de voz; criterio 3 en ambos sentidos; asignar y quitar tutor con sus 400/403/404/409; tutor al
inscribir; número de observación obligatorio) y `AccesoServiceTest` (+1). Se ajustaron 5 pruebas que daban por hecho 5
roles, 11 profesores, 1 pauta en la asignatura 6 o "TUTOR" como ejemplo de rol desconocido (ahora "INVITADO"). **218
pruebas, 0 fallos.** En la página (Chrome headless): el tutor evaluó a Sebastián con la pauta de 19 indicadores (6,47;
123 de 133); el coordinador quitó y volvió a asignar el tutor y creó una pauta de Orientación de 1.ª observación; nueva
ronda con los 5 usuarios de demo más el tutor: 0 errores.

**Falta (para seguir el 25-oct):**
1. **Cargar las otras 6 pautas de Práctica Profesional** (Especialidad 2.ª; Orientación 1.ª y 2.ª; colaborador y tutor)
   y las de **Práctica Pedagógica** (4 del colaborador, `Documents/5_Práctica Pedagógica.rar`). El script que las genera
   desde los `.docx` quedó en la carpeta temporal de la sesión: hay que rehacerlo o guardarlo en `db/` si se aprueba.
2. Elegir el tutor **en el mismo formulario** de "Nueva inscripción" (hoy se hace después, con el botón «Tutor»; la API ya
   lo acepta al crear).
3. Preguntar al cliente: ¿una sola evaluación por pauta (1.ª y 2.ª) por inscripción y evaluador? (S-55); ¿el tutor
   participa en las planificaciones? (S-58); ¿el tutor existe también en Práctica Pedagógica? (hoy no: `lleva_tutor`
   solo en PEM-P6).
4. La "NOTA FINAL" y la escala de notas que trae la pauta impresa (puntaje → nota) no se calculan: es parte de HU-09.
5. Actualizar `RESUMEN.md`, `objetivosAFUTURO.md` y el estado de HU-07 en el Product Backlog cuando se cierre.

## Resumen del tramo del 2026-10-16 (tareas 0 a 3)

| Tarea | Estado | Cómo se comprobó |
|---|---|---|
| 0. Flujos de punta a punta en la página | **Hecha.** Los dos flujos funcionan completos; 7 observaciones (A a G) | Chrome headless con estudiante, guía y profesor del curso; ver "flujos de punta a punta" |
| 1. Las 7 observaciones del 11-oct | **Hecha.** 6 corregidas y 1 que ya estaba resuelta (la 3); más 4 ajustes chicos de la Tarea 0 (A-D) | Ronda antes/después con Chrome headless y los 5 usuarios de demo: 0 errores en 39 pantallas |
| 2. HU-07 | **Primera parte hecha** (los 4 criterios cumplidos con 2 de las 12 pautas reales); falta cargar las demás pautas y detalles de la página | 8 pruebas nuevas + recorrido en la página con el tutor y el coordinador |
| 3. Resumen y números | **Hecha** (esta sección y `CLAUDE.md`) | — |

- **Pruebas:** **218**, todas pasan (`.\db\bd.ps1` y luego `mvn clean package`, antes de pasar de una tarea a la siguiente
  y al final). Eran 210 al empezar: +7 de `HU07TutorIntegracionTest` y +1 en `AccesoServiceTest`.
- **Endpoints:** **50** (49 + `PUT /api/inscripciones/{id}/tutor`). **Migraciones:** 6 (nueva:
  `006_tutor_y_numero_de_observacion.sql`). **Roles:** 6 (nuevo: `TUTOR`). **Pruebas negativas de la base:** 27 (+3).
- **Nada quedó roto.** Sin cambios en git (ni `add`, ni `commit`, ni `push`). Base restaurada con `.\db\bd.ps1` y
  aplicación detenida al terminar.
- **Supuestos nuevos:** S-54 (documentos de la planificación en cualquier estado) y S-55 a S-58 (HU-07), en "SUPUESTOS del
  Sprint 2".
- **Decisiones que tomé y conviene revisar juntos:** el título neutro "Evaluaciones de clases" para el jefe de carrera
  (observación 6); el número de observación en la pauta y no en la evaluación (S-55); la numeración corrida de los
  indicadores (S-56).
- **Para revisar a mano antes de la defensa:** la transcripción automática de la nota de voz con micrófono y voz reales
  (G de la Tarea 0) y los temas E y F de la Tarea 0 (avisos de pendientes en el inicio; documentos después de enviar la
  planificación).

## Verificación de cierre antes de subir a GitHub (2026-10-17)

Revisión completa sin cambiar código (solo esta sección y los números de `CLAUDE.md`). Las comprobaciones por HTTP y en
el navegador se hicieron con guiones de Python en la carpeta temporal de la sesión (fuera del repositorio); la clave de
demo la leen del encabezado de `seed.sql`. Sin `git add`, `commit` ni `push`.

| Paso | Resultado |
|---|---|
| 1. `.\db\bd.ps1` | Sin errores: MySQL 8.4.9 aceptado (≥ 8.0.16), 16 tablas, `verificacion.sql` sin violaciones, **27 de 27** pruebas negativas |
| 2. `mvn clean package` | **218 pruebas, 0 fallos, 0 errores, 0 omitidas. BUILD SUCCESS** |
| 3. Lo anterior al Sprint 2 (perfil normal, HTTP) | **64 de 64 comprobaciones** (ver abajo) |
| 4. Lo nuevo del Sprint 2 (perfil `prod`, HTTP) | **49 de 49** + control de versión de MySQL **26 de 26** + liberación del bloqueo a los 15 min: **2 de 2** |
| 5. Navegador (Chrome headless, micrófono simulado, perfil `prod`) | Los dos flujos de punta a punta completos; **las 7 observaciones del 11-oct resueltas**; 46 pantallas de 6 usuarios sin errores de JS |
| 6. Secretos y `.gitignore` | Ninguna clave real escrita. `.gitignore` cubre `.env`, `target/`, `adjuntos/`, `*.log`, `*.pem`, `*.key`; **no** cubre respaldos `.sql`/`.tar.gz` ni `.crt`/`.csr` (hoy no hay ninguno en la carpeta) |
| 7. `.\db\bd.ps1` al final | Base restaurada con los datos de demo |

**Paso 3, lo de antes (perfil normal):** Swagger abierto y con **50 operaciones**. HU-01: login 200, clave mala 401, sin
token o token falso 401, `/me` con su rol, `/api/roles` 200 coordinador y 403 estudiante. HU-02: el coordinador lista y
crea estudiantes (RUT con dígito malo 400; el guía 403), inscribe sin guía en el ciclo inicial y con guía da 400; el
estudiante ve solo sus 3 inscripciones y una ajena da 403. HU-04 + HU-21: crear, enviar sin documento 400 (S-47), subir
PDF (otro estudiante 403, `.exe` 400), enviar, observar sin comentario 400, otro guía 403, observar →
`CON_OBSERVACIONES`, corregir, reenviar, aprobar, el guía como profesor del curso 403, autorizar → `AUTORIZADA`;
descarga del documento por el profesor del curso y 403 a un estudiante ajeno. HU-03: pauta de 20 indicadores, guía ajeno
y estudiante 403, borrador privado (403 al estudiante), finalizar incompleta 400, puntaje fuera de escala 400, finalizada
con 120 puntos e inmutable. HU-12/HU-22: nota de voz (otro guía 403, segunda 409, corrección con `PUT`), documento de
la evaluación, el estudiante escucha el audio y otro estudiante 403. HU-05: historial propio, del guía asignado, y 403 al
guía no asignado y al estudiante ajeno. HU-31: el jefe descarga el PDF, fechas invertidas 400, los otros 4 roles 403,
sin token 401.

**Paso 4, lo nuevo (perfil `prod`, con `ISW_JWT_SECRET` aleatorio solo en el proceso):** `/swagger-ui.html`,
`/swagger-ui/index.html` y `/v3/api-docs` no responden; la página y el login funcionan igual; la consola queda vacía y
todo va al archivo de log (el único WARN es el del bloqueo provocado). HU-26: sin token 401, actual incorrecta 400,
nueva corta 400, nueva igual 400, cambio 204, la vieja da 401 y la nueva entra. Bloqueo: 5 fallos → 401, el 6.º intento
**con la clave correcta** da 429 con `Retry-After: 900` y el mensaje de 15 minutos; a los 14,0 minutos sigue en 429 ("Intenta nuevamente en 1 minuto", `Retry-After: 59`) y a
los 15,5 minutos la clave correcta vuelve a entrar (200). Se esperó el tiempo real, sin acortar la configuración. MySQL: el
control de `bd.ps1` y de `datos-iniciales.sql` detiene 8.0.15, 8.0.4-rc, 5.7 y MariaDB y deja pasar 8.0.16 en adelante
(`.\db\pruebas-version-mysql.ps1`, 26 de 26). Jefe de carrera: `/me` con `JEFE_CARRERA` + `PROFESOR_GUIA`; lee
inscripciones, planificaciones, evaluaciones (incluidos los mismos borradores) y estudiantes igual que el coordinador;
403 en roles, profesores, inscribir, cerrar y gestión de pautas; evalúa la inscripción 40 (donde es guía) y 403 en la 3;
no graba nota de voz ni revisa planificaciones ajenas. HU-06: editar la pauta 1 (usada) crea la versión 2 (otro id,
`versionAnteriorId` 1, 21 indicadores); la 1 queda con sus 20 indicadores intactos, inactiva y no vigente; una
evaluación antigua devuelve exactamente el mismo detalle; editar la versión vieja o borrar la usada da 409; una pauta
nueva sin uso se crea (201) y se borra (204, luego 404); borrar la versión 2 sin uso la deshace (la 1 vuelve a vigente).

**Paso 5, navegador:** planificación enviada sin documento (mensaje "falta la planificación formal (Word o PDF)", sin
nombres internos) → con PDF → el guía la ve con "Requiere tu acción" → devolver sin comentario marca el campo → con
comentario queda "Con observaciones" → Camila ve "El profesor guía dejó observaciones" → corrige y reenvía sin volver a
subir el documento → aprobada → autorizada. Evaluación: "Evaluar esta clase" llega con la planificación vinculada, 6 de
20 y guardar avance, grabación de 3 s con el micrófono simulado, guardar sin texto pide la transcripción, con texto se
guarda y no dice "sin corregir", finalizar con 6 de 20 se niega, con 20 queda finalizada (6,00; 120 de 140); Camila y el
profesor del curso ven el audio y la transcripción. **Las 7 observaciones del 11-oct: las 7 resueltas** (1 subtítulo y
filtro del estudiante; 2 un solo mensaje y consola limpia; 3 divisoria de 1 px; 4 barra opaca con `z-index` 20 y aviso
bajo el botón; 5 acciones fijas y visibles a 390 px sin desborde; 6 título neutro del jefe; 7 "Con tus roles" / "Con tu
rol"). Ronda por todos los enlaces del menú de coordinador, guía, profesor del curso, estudiante, jefe y tutor: 46
pantallas, 0 errores de JS, 0 alertas de error, 0 diálogos nativos.

**Encontrado (el 1 y el 2 se corrigieron el mismo día, ver "Correcciones" más abajo):**
1. **`deploy/install.sh` no rechaza un MySQL viejo antes de crear las tablas.** Solo muestra la versión; quien se detiene
   es `datos-iniciales.sql`, que corre **después** de `schema.sql`. En un MySQL < 8.0.16 quedarían las tablas creadas y
   vacías, y al volver a correr el instalador las vería ("la base ya tiene N tablas: NO se toca") y seguiría sin aviso.
   En Windows, `bd.ps1` sí se detiene antes de tocar la base. Arreglo sugerido: comparar `VERSION_MYSQL` en `install.sh`
   antes de `schema.sql` (y en modo de base externa, avisar a TI). Como la petición a TI es MySQL 8.4 LTS, no bloquea.
2. **`.gitignore`:** agregar `*.sql.gz`, `*.tar.gz`, `*.tgz`, `*.dump`, `*.crt`, `*.csr` y un patrón para volcados de
   respaldo (por ejemplo `isw_practicas_*.sql` y `adjuntos_*.tar.gz`, los nombres que genera `backup.sh`). No se puede
   ignorar `*.sql` en general porque `db/` lo usa.
3. Sin seguimiento en git y a decidir si se suben: `CONTEXTO_CHAT.md`, `DESPLIEGUE.md`, `GUIA_ESTUDIO_SPRINT1.md` y
   `objetivosAFUTURO.md` (no tienen claves; según `CLAUDE.md` los tres últimos estaban "sin commit" a propósito).
4. La clave de demo sigue escrita en `db/seed.sql` (encabezado) y `RESUMEN.md` §12: es la excepción aprobada el 20-sep,
   solo para desarrollo. Su hash está en `seed.sql` y en `bd.ps1` (pruebas negativas). `JwtServiceTest` usa dos secretos
   de prueba inventados (no son reales).

**Pendiente para el Sprint 3** (además de lo de arriba): el resto de HU-07 (las otras 6 pautas de Práctica Profesional y
las 4 de Práctica Pedagógica, tutor en el formulario de inscripción); HU-08 informes, HU-09 nota final, HU-10 duplas,
HU-25, HU-27, HU-28; avisos de pendientes en el inicio (E); preguntar S-37, S-38, S-54, S-55, S-58 y el peso de las
pautas; probar la transcripción con voz real (G); pedir el servidor a TI (MySQL 8.4 LTS) y desplegar; S-49/S-50
(bloqueo en memoria, tokens vigentes tras cambiar la clave); retrospectiva del Sprint 1 y planificación formal del
Sprint 2.

**Números al 17-oct:** 218 pruebas automáticas (0 fallos), 50 endpoints, 6 roles, 16 tablas, 6 migraciones, 27 pruebas
negativas de la base, 40 pruebas del control de versión de MySQL (26 + 14 de `install.sh`, ver abajo).

**Sobre los "exit code 255" de la verificación:** no fueron errores de la aplicación. Al cambiar de perfil y al terminar
se detuvo el `java -jar` con `Stop-Process -Force`, y un proceso cortado así termina con ese código. El log del perfil
`prod` tiene 0 líneas `ERROR` y la aplicación respondió hasta el final.

### Correcciones después de la verificación (2026-10-17, pedidas por el usuario)

Se corrigieron los hallazgos 1 y 2 de arriba. El 3 y el 4 quedan como estaban (decisión del usuario al subir).
- **`deploy/install.sh`, versión mínima de MySQL:** función `version_mysql_ok` con la misma regla que `db/VersionMysql.ps1`
  (≥ 8.0.16, rechaza MariaDB y textos que no son versión). El control se hace en el paso 1, **justo después de instalar
  los paquetes y antes de crear el usuario, las carpetas, el archivo de claves, el jar o las tablas**. Se repite **en
  cada ejecución, existan o no las tablas**. Si no alcanza se detiene con `ERROR: MySQL X no sirve: se necesita MySQL
  8.0.16 o superior (se recomienda 8.4 LTS; MariaDB y MySQL 5.7 no sirven)...`. El paso 5 ya no vuelve a consultar la
  versión. Con `--base-externa` (sin acceso de root a la base) solo avisa que TI debe entregar ≥ 8.0.16, y
  `datos-iniciales.sql` lo sigue controlando. La guía `deploy/GUIA_DESPLIEGUE_EQUIPO.md` (paso 3) lo explica.
- **Prueba:** `db/pruebas-version-mysql.ps1` tiene una parte 3 que toma la función de `install.sh` y la ejecuta en bash
  (Git para Windows o WSL) con los mismos 14 casos: **40 de 40** en total. Además se simuló el bloque real del
  instalador con un `mysql` falso: 5.7.44 y 10.11.6-MariaDB terminan con código 1 y el mensaje, y 8.4.9 sigue al paso
  2. `bash -n` sin errores; el archivo sigue con fines de línea LF. **No se probó en un Linux real con un MySQL viejo.**
- **`.gitignore`:** se agregaron `isw_practicas_*.sql`, `*.sql.gz` y `*.tar.gz` (los respaldos de `backup.sh`, que tienen
  datos de personas), y `*.crt` y `*.csr`. Comprobado con `git check-ignore`: esos nombres quedan ignorados, y
  `db/schema.sql`, `seed.sql`, `datos-iniciales.sql` y las migraciones **no**.
- Después: `mvn clean package` **218 pruebas, 0 fallos** (no cambió código Java) y `.\db\bd.ps1` para dejar la base
  limpia.

## Sprint 2: chatbot, tanda 1 de 3 — HU-15 y HU-16 (2026-10-26, rama `feature/chatbot`)

**Plan acordado con el usuario:** las 6 HU del chatbot (HU-15, 16, 17, 20, 29, 30) en 3 tandas de 2, con un commit del
usuario después de cada una. Esta es la primera. Sin cambios en git (ni `add`, ni `commit`, ni `push`).

**Qué se construyó.** El estudiante de Práctica Pedagógica o Profesional (las asignaturas con `chatbot_habilitado`)
conversa con un asistente que retroalimenta una planificación que **él ya escribió**. Elige la planificación en el chat;
el asistente lee su formulario y sus documentos (Word convertido a texto, PDF tal cual) y tiene como contexto el
historial del estudiante (HU-05: semestre, prácticas aprobadas con nota, evaluaciones y planificaciones de la práctica
en curso). Su profesor guía lee la conversación (HU-16).

**Backend:**
- `ChatbotController`: `GET /api/inscripciones/{id}/chatbot` (conversación; estudiante dueño o su guía; desde HU-20 también el jefe de carrera) y
  `POST /api/inscripciones/{id}/chatbot/mensajes` (solo el estudiante dueño; 201 con los dos mensajes). **52 endpoints**
  (50 + 2).
- `ChatbotService`: reglas (403 si no es el dueño, 409 si la asignatura no tiene chatbot o la inscripción está cerrada,
  400 si la planificación es de otra inscripción, 404 si no existe), armado del contexto con
  `HistorialService.historialDe` (las mismas reglas de acceso del endpoint de historial) y de la planificación. **La
  llamada al modelo va fuera de toda transacción** (no deja una conexión a la base tomada mientras espera) y los dos
  mensajes se guardan después, juntos: si el proveedor falla no queda un mensaje huérfano.
- `ModeloLenguaje` (interfaz) y `ModeloLenguajeAnthropic` (SDK oficial `com.anthropic:anthropic-java` 2.34.0, modelo
  `claude-opus-5-5`, esfuerzo medio, tope 16 000 tokens, 90 s, 2 reintentos). **Reemplazado el mismo día por
  `ModeloLenguajeGroq`: ver "Cambio de proveedor a Groq" más abajo; lo que sigue de este punto ya no aplica.** Toda falla del proveedor (sin red, clave
  mala, cuota, 5xx, respuesta vacía) se convierte en `ServicioExternoException` → **503** con un mensaje en español; la
  aplicación sigue funcionando. Sin clave, la aplicación arranca igual y el chatbot responde 503 "no está configurado".
  Si el modelo rechaza la petición por sus filtros de seguridad, se pide al proveedor que reintente con otro modelo
  (`app.chatbot.respaldo-por-rechazo`, función beta "fallbacks"; si diera problemas se apaga en `application.properties`).
- `InstruccionesChatbot`: las reglas de RetroPlani Mate de los criterios 2 a 8 y el contexto del estudiante.
- `TextoDocx`: texto de un `.docx` con `java.util.zip` (sin librerías nuevas; tope de 5 MB descomprimidos).
- `AccesoService.puedeVerChat`: misma regla de pertenencia que `puedeVerInscripcion` (se extrajo a `participaComo`),
  pero solo para los roles `ESTUDIANTE` y `PROFESOR_GUIA` (S-60).
- Configuración `app.chatbot.*` (`ChatbotProperties`); la clave sale **solo** de `ISW_ANTHROPIC_API_KEY`.

**Front-end:** ítem "Asistente de planificación" (estudiante y guía), vista `vistas/chatbot.js` (lista de prácticas o
estudiantes con asistente; chat con burbujas, selector "Planificación a revisar", aviso "Tu profesor guía, X, puede leer
esta conversación"; el guía lo ve en solo lectura), botón «Asistente» en "Mis inscripciones" y estilos en `vistas.css`.

**Criterios de aceptación:**

| HU | Criterio | Estado |
|---|---|---|
| HU-15 | 1. Solo en las dos prácticas finales; contexto = semestre y asignaturas aprobadas (HU-05) | **Cumplido** (409 en las demás; el contexto viaja en las instrucciones). Las "aprobadas" son las prácticas del sistema (S-64) |
| HU-15 | 2. Adjunta la planificación como archivo; si la pega como texto, pide el archivo | **Parcial:** se adjunta eligiendo una planificación del sistema con sus documentos Word/PDF (S-61); no acepta imágenes. Pedir el archivo si la pega como texto es una instrucción al modelo (S-62) |
| HU-15 | 3. Nunca diseña; mensaje fijo de límite | **Por instrucciones** (S-62): texto fijo redactado por el equipo; sin garantía determinista |
| HU-15 | 4. Verificación documental antes de retroalimentar | **Por instrucciones** (S-62); el sistema le entrega la lista real de documentos adjuntos |
| HU-15 | 5. Hasta 5 preguntas diagnósticas e integra las respuestas | **Por instrucciones** (S-62); la conversación completa se le envía en cada mensaje |
| HU-15 | 6. Revisa coherencia, 3 momentos, DUA, evaluación, recursos, errores; nunca pone nota | **Por instrucciones** (S-62) |
| HU-15 | 7. Preguntas de reflexión; máximo 2 retroalimentaciones por clase | **Por instrucciones** (S-62); el código no cuenta las retroalimentaciones |
| HU-15 | 8. Español, tono formativo, avisa si falta información | **Por instrucciones** |
| HU-15 | 9. Guarda cada mensaje por inscripción | **Cumplido** (`mensaje_chatbot`, con la planificación adjunta) |
| HU-16 | 1. El guía ve el chat de sus estudiantes asignados y de nadie más | **Cumplido** (403 a otro guía, al profesor del curso, al coordinador, al jefe de carrera y al tutor; S-60) |
| HU-16 | 2. El estudiante sabe que su guía puede verlo | **Cumplido** (aviso con el nombre del guía en el chat) |

**Estado honesto: HU-16 TERMINADA; HU-15 PARCIAL.** HU-15 funciona de punta a punta, pero (a) los criterios 3 a 8 dependen
del modelo y **no se han probado con el modelo real**: no hay clave del proveedor en este equipo, así que todas las
pruebas usan un doble y la página se probó con el chatbot sin configurar (503) y con mensajes insertados a mano; (b) el
criterio 2 se cumple con una variante (elegir la planificación del sistema, sin imágenes); (c) el proveedor y la
privacidad de enviar datos del estudiante a un tercero no están confirmados con el cliente (S-59).

**Pruebas: 235, 0 fallos** (eran 218; **+17**): `HU15ChatbotIntegracionTest` (8: caso feliz con el historial como
contexto, la conversación sigue con la misma planificación, estudiante sin historial, sin acceso, solo prácticas finales
y EN_CURSO, falla de la API externa → 503 sin guardar nada y la aplicación sigue respondiendo, planificación ajena 400 y
lectura de Word y PDF, mensaje vacío o largo 400), `HU16ChatGuiaIntegracionTest` (3), `AccesoServiceTest` (+1),
`ModeloLenguajeAnthropicTest` (3: sin clave, proveedor caído con un puerto local cerrado y armado de los turnos; nunca
llama a la API real) y `TextoDocxTest` (2). Las pruebas de integración reemplazan el modelo con `@MockitoBean`.

**Prueba real (HTTP, `java -jar` sin clave del proveedor): 12 de 12** — conversación vacía 200, aviso con el guía, 503
"no está configurado" al escribir, nada guardado después, `/api/auth/me` sigue en 200, el guía lee sin poder escribir,
el coordinador 403, Trabajo Colaborativo 409, Swagger con 52 operaciones y los archivos nuevos se sirven. **Navegador
(Chrome headless):** chat del estudiante con mensajes de ejemplo insertados a mano (borrados después con `bd.ps1`), el
503 se muestra dentro del formulario sin romper la vista, vista móvil de 390 px, lista y lectura del guía, Camila sin
práctica final ve "Aún no tienes acceso al asistente", el coordinador no tiene la función; **0 errores de JavaScript**.

**Falta / riesgos para las tandas siguientes:**
1. Probar con una clave real (`ISW_ANTHROPIC_API_KEY`) los criterios 3 a 8 con conversaciones de verdad, idealmente con
   el coordinador (es la base de HU-17 y HU-30). Tiene costo por uso.
2. Confirmar con el cliente: proveedor, costo y privacidad (S-59); quién lee el chat (S-60, sigue S-38).
3. El mensaje del estudiante no se muestra mientras el asistente responde (aparece junto con la respuesta, de 5 a 30 s):
   el botón muestra "El asistente está respondiendo…". Sin respuesta en streaming.
4. HU-17 y HU-29 (calibrar el estilo y cargar la base de conocimiento) necesitarán guardar las instrucciones y los
   documentos de referencia en la base: hoy las reglas están en el código (`InstruccionesChatbot`).

### Cambio de proveedor a Groq (2026-10-26, pedido por el usuario)

**Por qué:** Groq tiene capa gratuita; Anthropic cobra por uso. Groq expone la API "chat completions" de OpenAI en
`https://api.groq.com/openai/v1`.

**Qué cambió:**
- `ModeloLenguajeAnthropic` y la dependencia `com.anthropic:anthropic-java` **salieron**. Entró `ModeloLenguajeGroq`, que
  llama a `POST /chat/completions` con el `RestClient` de Spring (ya estaba en el proyecto: sin SDK nuevo). Instrucciones
  como mensaje `system`, turnos alternados `user`/`assistant`, `temperature` 0,3 y `max_completion_tokens` 1 500.
- Errores: **429** (límite de la capa gratuita) → 503 "alcanzó su límite de uso, inténtalo de nuevo en N segundos/minutos"
  (según `retry-after`) **sin reintentar**, porque reintentar solo agota más la cuota; **5xx o sin red** → hasta 2
  reintentos y después 503; **401/400** → 503 sin reintentar. Cada respuesta deja en el log los tokens usados y lo que
  queda de cuota (cabeceras `x-ratelimit-remaining-*`).
- **Groq solo recibe texto:** los PDF ahora también se convierten a texto (`TextoPdf`, con `PdfTextExtractor` de OpenPDF,
  la librería de la cartola; máximo 30 páginas). Todos los documentos juntos se recortan a 14 000 caracteres.
- `app.chatbot.*`: clave en `ISW_GROQ_API_KEY`, `url-base`, `modelo`, `temperatura`; `.env.example` con el marcador.
- **`app.chatbot.respaldo-por-rechazo` se eliminó.** Era la función beta "fallbacks" de la API de Anthropic: cuando el
  modelo se niega a responder por sus filtros de seguridad, el propio servidor reintenta con otro modelo. Groq no tiene
  nada equivalente ni ese tipo de rechazo. Lo parecido en Groq sería cambiar a un modelo más chico cuando se agota la
  cuota (429), pero se descartó: la calidad de la retroalimentación cambiaría sin aviso y cada modelo tiene su propia
  cuota, así que solo aplaza el problema.
- Pruebas: `ModeloLenguajeAnthropicTest` (3) se reemplazó por `ModeloLenguajeGroqTest` (5, contra un servidor HTTP local
  que imita a Groq: cuerpo y cabecera enviados, 429 sin reintento, 5xx con reintentos, 401, sin conexión) y se agregó
  `TextoPdfTest` (2). La prueba de HU-15 que adjunta un PDF ahora genera un PDF real y comprueba que su texto llega.

**Prueba con el modelo real:** ver la sección siguiente.

### Prueba con el modelo real de Groq (2026-10-26)

Con `ISW_GROQ_API_KEY` definida, `java -jar` y un guion HTTP como Valentina (inscripción 11). Se subió a su
planificación 8 un Word con errores puestos a propósito: "si a es negativo la parábola se abre hacia arriba", "toda
parábola corta siempre al eje X en dos puntos", sin DUA, cierre sin evidencia y evaluación (factorización) desalineada
del objetivo.

**1. El modelo configurado ya no existía.** Groq respondió 404 a `llama-3.3-70b-versatile` (la consulta `GET /models`
de esta cuenta ya no lo lista). El sistema se comportó como debía: 503, nada guardado, la app siguió respondiendo.
**Cambio:** `app.chatbot.modelo=openai/gpt-oss-120b` (el modelo de texto más grande disponible, contexto de 131 000
tokens) y `app.chatbot.max-tokens` de 1 500 a **3 000**, porque gpt-oss razona antes de contestar y esos tokens cuentan
en el tope (la retroalimentación completa usó 1 640). Mismo cambio en `ChatbotProperties` y `ModeloLenguajeGroqTest`.
Si Groq retira también este modelo, basta cambiar la propiedad (no el código).

**2. Criterios de HU-15 con el modelo real:**

| Criterio | Resultado |
|---|---|
| 2. Pega la planificación como texto → pedir el archivo | **A medias:** respondió con el mensaje fijo de límite, que termina pidiendo elegir la planificación en "Planificación a revisar"; no la retroalimentó como texto, pero tampoco usó una respuesta propia para este caso |
| 3. Pide escribir una actividad de inicio | **Cumple:** mensaje fijo, palabra por palabra |
| 4. Verificación documental | **Cumple:** notó que la "guía de exploración" está en los recursos pero no adjunta, y la pidió |
| 5. Hasta 5 preguntas diagnósticas e integra las respuestas | **Cumple:** exactamente 5; la retroalimentación usó lo respondido (36 estudiantes, 3 con NEE, TDAH y lectura, GeoGebra pedido por la guía) |
| 6. Coherencia, 3 momentos, DUA, evaluación, recursos, errores; sin nota | **Cumple:** detectó **los dos errores matemáticos** y la evaluación desalineada; tablas por momento; DUA por principios. Pedirle nota del 1 al 7: se negó y explicó por qué |
| 7. Preguntas de reflexión; máximo 2 retroalimentaciones | **Cumple:** 3 preguntas de reflexión en cada una; a la tercera petición se negó y derivó a la profesora guía por su nombre |
| 8. Español, tono formativo | **Cumple** |

**Para calibrar con el coordinador (HU-17):** algunas sugerencias rozan el "no diseña" (proponer una tarjeta de apoyo,
"pedir que modifiquen el término constante", una sección "Sugerencias concretas paso a paso"). No entrega material listo
para copiar, pero el límite entre sugerir y diseñar lo debe fijar el cliente.

**3. Límite de la capa gratuita (riesgo nuevo para el despliegue).** `openai/gpt-oss-120b` permite **8 000 tokens por
minuto para toda la cuenta** (1 000 peticiones al día). Groq descuenta la entrada más el tope reservado de salida, así
que cada mensaje gasta de 4 000 a 6 000: en la práctica **uno o dos mensajes por minuto para todo el sistema**. En la
prueba, el 5.º y 6.º mensaje enviados seguidos dieron 429 → 503 "alcanzó su límite de uso, inténtalo de nuevo en 28
segundos", sin guardar nada; repetidos con un minuto de pausa respondieron bien. Sirve para la demostración y para
pocos estudiantes; para un curso completo escribiendo a la vez hace falta el plan de pago de Groq u otro proveedor
(S-59, a decidir con el cliente).

**Otros datos:** respuestas de 0,7 a 4 s. Sin cortes por largo. El guía (`guia2@demo.example`) leyó los 8 mensajes.
**239 pruebas, 0 fallos** con el modelo nuevo; la base se recreó con `bd.ps1` después de la prueba.

## Sprint 2: chatbot, tanda 2 de 3 - HU-17 y HU-20 (2026-10-26)

Rama `feature/chatbot`, sin commit (lo hace el usuario). **HU-17 TERMINADA; HU-20 PARCIAL.**

### HU-17 · El coordinador calibra el estilo del chatbot

**Por qué así:** las instrucciones estaban fijas en `InstruccionesChatbot.java`; el coordinador no podía ajustarlas sin
un programador. Ahora son un registro de la base que solo él edita, y el código solo agrega lo que el coordinador no
debe tocar: el contexto del estudiante (semestre, prácticas aprobadas, evaluaciones), que sale del historial (HU-05).

**Base de datos (migración `008_instrucciones_chatbot.sql`, tabla 17):** `instrucciones_chatbot` con una sola fila
(`CHECK id = 1`): `texto`, `editado_por_id` (FK a usuario) y `fecha_edicion` (`CHECK`: los dos nulos o los dos con
valor). La migración deja la tabla **vacía** a propósito: así sigue siendo solo ASCII y el texto no queda copiado en ella.
Sin fila, el chatbot usa el texto base que trae la aplicación; la primera vez que el coordinador guarda, se crea.
`db/seed.sql` (desarrollo) sí carga la fila con el texto base (PARTE 3). `datos-iniciales.sql` (producción) no la carga.
También se actualizaron `schema.sql`, `verificacion.sql` (17 tablas, bloque 25 nuevo; el conteo de FK ya estaba
desactualizado desde la migración 007: ahora es 29), `bd.ps1` y `Sprint1/mer.md`.

**Texto base (S-65):** una sola fuente, `backend/src/main/resources/chatbot/instrucciones-base.txt` (~7 000 caracteres):
la propuesta RetroPlani Mate V3 resumida y adaptada. Trae regla prioritaria (no diseñar, exigir la planificación
adjunta), flujo (umbral mínimo, verificación documental, hasta 5 preguntas diagnósticas, retroalimentación por componente,
reflexión, máximo 2 retroalimentaciones), tono y expresiones a usar y a evitar, y 9 respuestas modelo. El `INSERT` de
`seed.sql` se **generó** desde ese archivo; la prueba `elSeedCargaElTextoBaseSinEditar` falla si se desincronizan.
Cambio de comportamiento para HU-15: la respuesta a "pegó la planificación como texto" ya no es la de "piden diseñar"
(en la prueba real del mismo día respondía con la de diseño), y el mensaje de límite ahora es el de la propuesta.

**Backend:** entidad `CalibracionChatbot`, `CalibracionChatbotRepository`, `CalibracionChatbotService` y
`CalibracionChatbotController`:

| Endpoint | Quién | Qué |
|---|---|---|
| `GET /api/chatbot/instrucciones` | Solo `COORDINADOR` (403 los demás) | Texto vigente, si es el base, quién y cuándo lo editó, el texto base y los límites |
| `PUT /api/chatbot/instrucciones` | Solo `COORDINADOR` | Reemplaza el texto (200 a 12 000 caracteres, 400 si no, S-67); guarda quién y cuándo |

`ChatbotService` arma las instrucciones con `calibracion.textoVigente()` (texto del coordinador o, si no hay fila o está
en blanco, el base) más el contexto del estudiante. **54 endpoints** (eran 52).

**Front-end:** pantalla "Estilo del asistente" (`vistas/estilo-asistente.js`, ítem del menú y acceso rápido del
coordinador): área de texto con contador de caracteres, quién y cuándo editó, "Guardar", "Descartar cambios" y "Volver
al texto base" (pide confirmación y no guarda hasta presionar Guardar).

| Criterio | Estado |
|---|---|
| 1. Editar tono y expresiones permitidas/evitadas sin tocar código | **Cumplido** |
| 2. Revisar y ajustar los casos de respuesta modelo | **Cumplido** (van en el mismo texto, sección "Respuestas modelo") |
| 3. Los cambios aplican a lo nuevo, no reescriben lo guardado | **Cumplido** (S-68: rige desde el siguiente mensaje, también en conversaciones en curso) |

### HU-20 · El jefe de carrera lee los chats

`AccesoService.puedeVerChat` acepta además `leeChatComoJefe`: rol `JEFE_CARRERA` y asignatura con `chatbot_habilitado`,
sin necesidad de ser el guía. `GET /api/inscripciones/{id}/chatbot` acepta `JEFE_CARRERA`; escribir sigue siendo solo del
estudiante dueño (403). Cada lectura del jefe queda en el log (`Jefe de carrera 32 leyó el chat de la inscripción 11`).
Front-end: ítem "Conversaciones del asistente" y acceso rápido del jefe; lista de todas las prácticas con asistente
(período más reciente primero) con filtro por estudiante, asignatura o período (sin distinguir tildes); el chat se abre
en solo lectura con el aviso "como jefe de carrera puedes leer esta conversación".

| Criterio (borrador) | Estado |
|---|---|
| 1. Listar conversaciones por estudiante, asignatura y fecha | **Cumplido en parte:** filtro por estudiante, asignatura y período; no por fecha de mensaje |
| 2. Solo lectura | **Cumplido** |
| 3. El estudiante ve un aviso de que el jefe puede leer | **No hecho, por decisión del usuario** (S-66): pendiente de confirmar con el cliente |
| 4. Cada acceso queda registrado | **Parcial:** solo en el log de la aplicación, no en una tabla (S-66) |

### Pruebas: 250, 0 fallos (eran 239; +11)

- `HU17CalibrarChatbotIntegracionTest` (6): el seed trae el texto base idéntico al archivo; el coordinador edita y el
  **siguiente** mensaje lleva el texto nuevo más el contexto del estudiante, mientras el mensaje anterior se envió con el
  base y los guardados no cambian; volver al texto base; los otros 5 roles reciben 403 al leer y al editar; vacío, corto
  o de más de 12 000 caracteres da 400; sin fila en la tabla se usa el base y el primer guardado la crea.
- `HU20ChatJefeCarreraIntegracionTest` (4): el jefe lee el chat de Valentina sin ser su guía; no escribe; solo en
  asignaturas con chatbot (Trabajo Colaborativo 403); coordinador, profesor del curso, otro guía, otro estudiante y
  tutor siguen en 403.
- `AccesoServiceTest` (+1, `elJefeDeCarreraLeeLosChatsDeLasPracticasConChatbot`). En `HU16ChatGuiaIntegracionTest` el
  jefe dejó de esperar 403 y `HU15ChatbotIntegracionTest` busca las frases del texto base nuevo.
- `bd.ps1`: 17 tablas, 27 de 27 pruebas negativas, bloque 25 con 0 violaciones.

### Prueba con el modelo real de Groq (`openai/gpt-oss-120b`)

Mismo mensaje de Valentina ("Te adjunto mi planificación de la clase de funciones cuadráticas para que la revises.") con
el mismo Word con errores de la prueba de HU-15, partiendo de una conversación vacía (`bd.ps1` entre una fase y otra):

- **Fase A, texto base:** verificación documental en una tabla, observaciones preliminares (detectó los dos errores sobre
  la parábola y las dos evaluaciones distintas) y **5 preguntas diagnósticas**. 2 364 tokens de entrada y 1 017 de salida.
- **Fase B:** el coordinador (Andrea Fuentes Lagos) agregó por la API esta calibración al final del texto:

  ```
  ## Calibración de la coordinación (prueba HU-17)
  - Comienza SIEMPRE tu respuesta con la frase exacta "Hola, colega:".
  - Responde en un máximo de 120 palabras.
  - Haz como máximo 2 preguntas diagnósticas, numeradas.
  - No uses tablas ni títulos con #.
  ```

  Respuesta real (388 tokens de salida, 1,5 s):

  > Hola, colega: recibí tu planificación "Introducción a las funciones cuadráticas". Incluye objetivo, inicio,
  > desarrollo y cierre, y menciona recursos (GeoGebra, proyector) y una guía de exploración, aunque la guía no está
  > adjunta. La evaluación indicada en el texto ("prueba escrita de factorización al final de la unidad") no parece
  > alinearse con el objetivo de reconocer la función y su gráfica.
  >
  > 1. ¿Cómo relacionas esa prueba de factorización con el objetivo de reconocer la función cuadrática y su parábola?
  > 2. ¿Qué acciones concretas de DUA incluirás en cada momento para apoyar a estudiantes que tengan dificultades con la representación gráfica?

  Cumplió las 4 reglas nuevas y mantuvo las del texto base (verificación documental, recurso faltante). En la misma fase:
  `PUT` del jefe de carrera 403, `GET` de la estudiante 403, el jefe leyó el chat (200, 2 mensajes, sin poder escribir) y
  el coordinador recibió 403 al leerlo.

**Navegador (Chrome, extensión):** pantalla "Estilo del asistente" del coordinador con el texto y "Última edición";
lista del jefe con las 6 prácticas con asistente, filtro "valentína" → 1 resultado, chat en solo lectura sin formulario;
**0 errores de JavaScript**. Después: `bd.ps1`, aplicación detenida.

### Pendiente / para revisar

1. Confirmar con el cliente S-65 (texto base resumido), S-66 (aviso al estudiante y registro de accesos del jefe) y S-68.
2. HU-20 criterios 3 y 4 si el cliente los pide: el aviso es una línea en el chat del estudiante; el registro en la base
   necesita una tabla nueva (otra migración).
3. El texto base es más largo que las reglas anteriores: el primer mensaje con la planificación adjunta pasó de 1 808 a
   2 364 tokens de entrada. Con la capa gratuita de Groq (8 000 tokens/minuto, contando el tope de salida reservado) la
   cuota sigue alcanzando para uno o dos mensajes por minuto en todo el sistema (S-59).
4. Producción: aplicar `008_instrucciones_chatbot.sql` en el servidor (paso de migraciones de `deploy/GUIA_DESPLIEGUE_EQUIPO.md`).

## Sprint 2: chatbot, tanda 3 de 3 - HU-29 y HU-30, fin del chatbot por ahora (2026-10-26)

Rama `feature/chatbot`, sin commit. **HU-29 TERMINADA con un límite (S-70); HU-30 PARCIAL a propósito.**
**263 pruebas, 0 fallos** (eran 250; +13). Migración `009_base_conocimiento_y_validacion.sql` (tabla 18). **60 endpoints.**

**HU-29 · Base de conocimiento.** Tabla `documento_base_conocimiento` (uno vigente por tipo, 10 tipos = la lista de la
propuesta RetroPlani Mate, S-69). `GET /api/chatbot/base-conocimiento`, `POST /{tipo}` (subir o reemplazar, multipart) y
`DELETE /{tipo}`, solo el coordinador (403 los demás). Usa `AlmacenamientoArchivosService` (disco, 10 MB, Word/PDF). El
texto se extrae una sola vez al subir (`TextoDocx`, `TextoPdf` hasta 400 páginas) y se guarda en la base; sin texto
legible, 400. En cada mensaje `FragmentosBaseConocimiento` elige los fragmentos (≈700 caracteres) con más palabras en
común con el mensaje y la planificación, pesadas por rareza, hasta 5 000 caracteres (S-70: búsqueda por palabras, no
embeddings). Sin documentos, el chatbot funciona igual. Pantalla: tarjeta "Base de conocimiento" en "Configuración del
asistente" (antes "Estilo del asistente").

**Prueba real (Groq):** el coordinador subió `02_Rubrica_planificacion_clases.docx` (2 520 caracteres) y
`06_Bases_Curriculares_Matematica_7basico_2medio.pdf` (58 846 caracteres); la estudiante no pudo subir (403). La
respuesta citó las Bases ("mostrar que comprenden la función cuadrática y representarla gráficamente") y recorrió los
criterios reales de la rúbrica (presentación, diagnóstico, léxico y ortografía, dificultades matemáticas). 3 741 tokens de
entrada (antes 2 364). **Observaciones:** no advirtió que la función cuadrática es de 2.º medio y la clase dice 1.º medio;
ante una pregunta directa saltó las preguntas diagnósticas y entregó la retroalimentación completa con sugerencias muy
concretas (calibrar con HU-17).

**HU-30 · Validación (lo mínimo).** `estudiante.validador_chatbot` e `instrucciones_chatbot.validacion_abierta`.
`GET/PUT /api/chatbot/validacion` y `PUT /api/chatbot/validacion/validadores/{estudianteId}`, solo el coordinador. Con la
validación abierta, un no validador ve "aún no disponible" y recibe 409 al escribir. Seed: cerrada, Valentina validadora.
Pantalla: tarjeta "Validación con estudiantes" (interruptor y casillas). Probado en Chrome: abrir la validación y la
vista de Sebastián (aviso, sin formulario), 0 errores de JavaScript.

Supuestos nuevos: **S-69** (un documento por tipo, los 10 de la propuesta; el anterior queda en disco), **S-70**
(búsqueda por palabras con tope de 5 000 caracteres), **S-71** (la SUS y la rúbrica de jueces van fuera del sistema).

### Lo que le tocaba al compañero (resuelto el 26-oct en el cierre final, sección siguiente)
1. **HU-30:** registrar la Escala SUS y la rúbrica de 9 criterios: se propone un formulario externo (Google Forms) y
   confirmarlo con el cliente. No hay nada de esto en el sistema.
2. **HU-20:** aviso al estudiante de que el jefe lee su chat y registro de accesos en una tabla (S-66), si el cliente lo pide.
3. **Confirmar con el cliente** S-59 (Groq, costo y privacidad: capa gratuita = 1 mensaje/minuto con la base de
   conocimiento), S-60, S-65 a S-71 y S-38.
4. **HU-29:** si los jueces ven respuestas pobres, cambiar la búsqueda por embeddings (solo `FragmentosBaseConocimiento`).
5. Aplicar las migraciones 007, 008 y 009 en el servidor (`CLAUDE.md` y `DESPLIEGUE.md` ya quedaron al día el 26-oct).


## Sprint 2: chatbot, cierre final - HU-15, HU-20, HU-29 y HU-30 completas (2026-10-26)

Rama `feature/chatbot`, sin commit (lo hace el usuario). Se cierran los 4 criterios que quedaron a medias en las tandas
1 a 3. **Las 6 HU del chatbot (HU-15, 16, 17, 20, 29 y 30) quedan completas; no queda nada pendiente de código.**
**280 pruebas, 0 fallos** (eran 263; +17). Migraciones `010` y `011` (tablas 19 a 22). **64 endpoints** (eran 60).
Base: 22 tablas, PK 22, FK 35, UNIQUE 18, CHECK 47, **31 de 31 pruebas negativas** (eran 27; +4).

### 1. HU-15, criterio 2 · La planificación pegada como texto (regla en código)
Antes lo hacía el modelo "por instrucciones", sin garantía. Ahora `ChatbotService` decide **antes** de llamar al modelo:
si en la conversación no hay planificación adjunta y el mensaje parece una planificación pegada
(`PlanificacionPegada.pareceUna`), responde un texto fijo (`PEGAR_PLANIFICACION`: elegirla en «Planificación a revisar»)
**sin llamar a Groq**, igual que las otras reglas del sistema. Parece una planificación = más de 400 caracteres **y** al
menos 2 partes distintas de una planificación (objetivo, recursos, actividad, evaluación, DUA, inicio, desarrollo,
cierre, indicador, habilidad, OA, minutos), sin tildes ni mayúsculas (SUPUESTO **S-72**). Una pregunta corta, aunque
nombre esas partes, sigue llegando al modelo. Los dos mensajes se guardan (el guía ve el intento), pero
`historiaParaElModelo` los saca de la historia que viaja en los mensajes siguientes: el modelo nunca leyó ese texto y
reenviarlo gastaría la cuota de tokens. Pruebas: `HU15PlanificacionPegadaIntegracionTest` (4; el mock comprueba que el
modelo no se llamó) y `PlanificacionPegadaTest` (4).

### 2. HU-20, criterios 3 y 4 · Aviso al estudiante y registro de las lecturas del jefe
- **Criterio 4:** tabla `lectura_chat_jefe_carrera` (inscripción, usuario, fecha; migración `010`). Cada
  `GET /api/inscripciones/{id}/chatbot` del jefe de carrera inserta una fila (el método pasó de `readOnly` a
  transacción normal). No se registran las lecturas del guía, del estudiante ni las del jefe que lee como guía de esa
  inscripción por su segundo rol (eso es HU-16). Un 403 no deja fila.
- **Criterio 3:** `ConversacionChatbotDto` trae `avisoJefeCarrera` (en las asignaturas con chatbot) y
  `ultimaLecturaJefeCarrera`. La pantalla del estudiante muestra, bajo el aviso del guía: «El jefe de carrera también
  puede leer esta conversación, en solo lectura; cada vez que la abre queda registrado. Última lectura: …» (o «Aún no la
  ha leído»). Solo la última, no la lista (S-73).
- **S-66 ajustado:** antes "sin aviso y solo en el log"; ahora con aviso y registro en la base. **Sigue siendo SUPUESTO**
  (no lo confirmó el cliente), igual que la privacidad de los chats (S-38).
- Pruebas: 2 nuevas en `HU20ChatJefeCarreraIntegracionTest` (el jefe lee dos veces → 2 filas y el estudiante ve la fecha;
  el guía y el estudiante leen → 0 filas; el coordinador recibe 403 → 0 filas).

### 3. HU-29 · Ranking de fragmentos por TF-IDF (S-70 se mantiene; S-74 nuevo)
Sin presupuesto para embeddings (Groq no los ofrece en la capa gratuita), se mejoró `FragmentosBaseConocimiento` sin
servicio externo: cada fragmento y la consulta se pesan con **TF** (1 + ln de las veces que aparece cada palabra) por
**IDF** (fórmula de BM25: casi cero para una palabra que está en todos los fragmentos) y se ordenan por **coseno**. Antes
solo se sumaban las palabras en común (cada una pesada por rareza, pero sin contar repeticiones ni el largo del
fragmento). Además las palabras cuentan desde **3 letras** (antes 4), para que entren siglas como "DUA", con una lista de
palabras vacías de 3 letras ("los", "con", "que"...). Pruebas nuevas en `FragmentosBaseConocimientoTest` (2): un
fragmento del DUA que comparte 2 palabras con la consulta le gana a uno genérico que comparte 6 palabras comunes (con el
algoritmo anterior ganaba el genérico: lo verifiqué con el cálculo, no corriendo el código anterior), y entre dos
fragmentos con la misma palabra gana el que más la trata.

**Limitaciones conocidas (no son pendientes de código):**
- **Sigue sin ser búsqueda por significado:** un sinónimo o una paráfrasis no coincide (S-70 se mantiene). Mejoró la
  precisión, no la "comprensión".
- **Criterio 2 ("responde solo con esos documentos")** sigue dependiendo de las instrucciones al modelo: los fragmentos
  viajan como material de consulta, pero nada impide que el modelo agregue conocimiento propio. Garantizarlo al 100 %
  exigiría otra arquitectura (por ejemplo, verificar cada afirmación contra las fuentes); no se fuerza.

### 4. HU-30 completa · Escala SUS y rúbrica de 9 criterios dentro del sistema
- **Los instrumentos NO son inventados:** están en la propuesta RetroPlani Mate V3 (`Documents/ChatBot (1).rar`,
  apartado I, carta a los jueces expertos): la **Escala SUS adaptada al chatbot** (10 ítems de 1 a 5, versión en
  español de Sevilla-González et al., 2020, adaptada de Brooke, 1986), la **rúbrica de juicio experto** (9 criterios de 1
  "logro insuficiente" a 4 "logro alto") y sus **4 preguntas abiertas**. Los textos están en `InstrumentosValidacion`;
  de la rúbrica se muestra el descriptor del nivel 4 de cada criterio. No hizo falta el supuesto de "versión más citada".
- Tablas (migración `011`): `respuesta_sus` (10 ítems con CHECK 1 a 5; el **puntaje SUS 0 a 100 lo calcula la base**,
  columna generada con la fórmula de Brooke: impares respuesta − 1, pares 5 − respuesta, suma × 2,5),
  `respuesta_rubrica_experto` (las 4 preguntas abiertas) y `respuesta_rubrica_criterio` (9 filas por rúbrica: número
  1 a 9, puntaje 1 a 4, comentario opcional). Las dos primeras con UNIQUE por validador: **una vez cada una**.
- Endpoints (`RespuestasValidacionController`): `GET /api/chatbot/validacion/mi-validacion` (estudiante: si es validador,
  qué respondió y los textos), `POST .../sus` y `POST .../rubrica` (estudiante validador; 403 si no lo es, 409 si ya
  respondió, 400 si faltan ítems o se salen de la escala), `GET .../resultados` (solo el coordinador: promedio SUS,
  promedio por ítem y por criterio, y cada respuesta con nombre y fecha). Los demás roles, 403.
- Pantallas: el validador ve en su asistente un aviso «Participas en la validación…» con el enlace a
  `#/chatbot/validacion` (SUS primero y rúbrica después, con la misma escala de botones que la pauta de evaluación); el
  coordinador ve la tarjeta «Resultados de la validación» en «Configuración del asistente».
- **SUPUESTO S-75:** en la propuesta, la SUS y la rúbrica son para los **jueces expertos** y los estudiantes de 4.º y 5.º
  año usan otro instrumento (el formato de Andrés Seguel, que no está en el material). Aquí responde cualquier
  **estudiante marcado como validador**: un juez experto usaría una cuenta de estudiante validador (el chatbot solo lo
  usa un estudiante). Se puede responder con la validación abierta o cerrada; desmarcar al validador no borra lo que
  respondió. No se guardan la identificación del juez (filiación, firma) ni el instrumento de usuarios. Reemplaza a
  **S-71** (que dejaba la SUS y la rúbrica fuera del sistema).
- Pruebas: 5 nuevas en `HU30ValidacionChatbotIntegracionTest` (responde las dos una vez y la segunda da 409; el
  coordinador ve SUS 80,00 y los promedios; sin respuestas los promedios van vacíos; un no validador recibe 403 hasta que
  lo marcan; los demás roles 403; incompletas o fuera de escala 400 sin guardar nada).

### Base de datos
- `verificacion.sql`: tablas 22, filas de las 4 tablas nuevas (0 en la demo), restricciones esperadas, bloques **28**
  (lecturas: solo de jefes de carrera y en asignaturas con chatbot) y **29** (rúbricas con sus 9 criterios, SUS entre 0 y
  100). No se controla que quien respondió siga marcado: el coordinador puede desmarcarlo después y su respuesta vale.
- `bd.ps1`: 4 pruebas negativas nuevas (ítem SUS fuera de 1 a 5, segunda SUS del mismo validador, criterio 10, lectura
  de una inscripción inexistente).
- **Migraciones probadas sobre una base sin ellas:** se borraron las 4 tablas como root y se aplicaron `010` y `011`;
  terminan sin errores y las restricciones quedan idénticas a una instalación nueva (CHECK 47, FK 35, PK 22, UNIQUE 18).
  En el servidor hay que aplicar, en orden, `007` a `011`.
- Las columnas son `INT` (no `TINYINT`): con `ddl-auto=validate`, Hibernate rechaza un `TINYINT` mapeado a `Integer`.

### Prueba con el modelo real de Groq (`openai/gpt-oss-120b`, 26-oct)
- **HU-15, criterio 2:** Valentina pegó como texto una planificación de función cuadrática (objetivo, recursos, inicio,
  desarrollo, cierre y evaluación) sin adjuntar ninguna. Respondió en **0,3 s** con el texto fijo («Parece que pegaste tu
  planificación como texto… elígela en la lista «Planificación a revisar»…»): **Groq no se llamó**. Luego escribió
  «Hola, ¿qué necesitas de mí para revisar una clase?» y ese sí llegó al modelo (1,4 s): pidió elegir la planificación
  en «Planificación a revisar».
- **HU-29 con el ranking nuevo:** el coordinador subió 3 documentos reales de la propuesta: la rúbrica (2 520
  caracteres), los ejemplos de acciones DUA (25 501) y las Bases Curriculares de 7.º a 2.º medio (58 846). Pregunta, con la
  planificación 8 adjunta: «¿Qué acciones DUA concretas podría agregar para atender la diversidad de mi curso en esta
  clase de función cuadrática?». Comparé los fragmentos que elige cada algoritmo con esos mismos textos (con un programa
  desechable que usa la clase compilada): **el nuevo trae el fragmento «Principios del DUA… Principio 1: proporcionar
  múltiples medios de representación»**, que el anterior dejaba fuera, y saca un párrafo genérico de las Bases («Los
  aportes de la matemática…»); los demás fragmentos de acciones DUA coinciden. El modelo hizo primero sus 5 preguntas
  diagnósticas (el flujo de HU-15); tras contestarlas, la retroalimentación organizó acciones DUA por momento de la clase
  («Representación múltiple», «Apoyo al procesamiento», «Opciones de respuesta»), en la línea de los principios del DUA
  del fragmento elegido; desde fuera no se puede saber qué parte salió del documento y cuál del conocimiento propio del
  modelo (ver la limitación del criterio 2). **Límite visto:** ninguno de los dos algoritmos trajo el OA de función cuadrática de las Bases para esa
  pregunta, porque la consulta la dominan las palabras del DUA (no es búsqueda por significado, S-70).
- **En Chrome:** la estudiante ve el aviso del jefe («Aún no la ha leído» → tras la lectura del jefe, «Última lectura:
  26-10-2026 22:50»); el formulario de validación (10 ítems SUS, 9 criterios, 4 preguntas abiertas) avisa si falta un
  ítem y, enviado, queda como «Respondiste… ¡Gracias!»; el coordinador ve SUS 80,00 y el criterio 6 en 2,00. Sin errores
  de JavaScript en la consola (en este equipo no hay `node`: la sintaxis de los módulos nuevos se validó cargándolos en
  Chrome).
- Nota: las primeras dos pruebas se enviaron con un script de PowerShell 5.1 que leyó su propio archivo como ANSI, por
  eso esos mensajes quedaron con tildes rotas en la base de prueba (no es un error de la aplicación); la pregunta de
  HU-29 se repitió desde el navegador con UTF-8 correcto. La base se recreó al final (`bd.ps1`).

### Qué queda (ya no es código)
- **Confirmar con el cliente** S-38, S-59 (Groq: costo, privacidad y cuota), S-60, S-65 a S-75.
- Aplicar las migraciones `007` a `011` en el servidor cuando exista.
