# CLAUDE.md

## Proyecto
Proyecto semestral de Ingeniería de Software (UBB, Pedagogía en Educación Matemática): sistema web para
evaluar y monitorear las 6 prácticas de cada estudiante. El chatbot y la nota de voz llegan en sprints
futuros. El detalle completo está en `RESUMEN.md`.

## Estado
- Sprint 1 con entrega el **viernes 25 de septiembre de 2026**.
- Fase A (documentos en `Sprint1/`): terminada.
- Fase B (base de datos MySQL, carpeta `db/`): terminada.
- Fase C (backend Spring Boot en `backend/`): **terminada y probada el 2026-09-20**. Las 5 HU, 32 endpoints, 146 pruebas automáticas.
- Ajustes del 2026-09-20 por las respuestas de un estudiante de PEM (orientativas, no del cliente): Aproximación es `GRUPO` sin planificaciones, Ambientes es `DUPLA`, el ciclo inicial no tiene guía, planificación con 3 días de anticipación. Cambió el esquema (migración `db/migraciones/001_...sql`).
- **Abierto:** S-37 (¿flujo de planificación en secuencia o en paralelo?), S-38 (el jefe de carrera pidió ver los chats; el estudiante no quiere que el guía los lea), S-09 (archivo del formato de planificación) y el peso de las pautas.
- **Pendiente del proyecto:** presentación (Persona 4), retrospectiva, subir el tablero, confirmar supuestos con el cliente o la profesora.
- 2026-09-21: se rehicieron `Sprint1/as-is.bpmn`, `to-be.bpmn`, `mer.md` (con vista simple en 3 partes) y los dos backlogs para que el equipo los dibuje a mano; imágenes guía en `Sprint1/diagramas/`. `tablero.csv` actualizado (41 tarjetas, código en "Hecho") para importarlo a Trello. Se borró `resumenDelSprint1.md` a pedido del usuario.
- Para retomar, lee: `backend/PROGRESO.md` (estado, endpoints, supuestos S-17 a S-38), `backend/README.md` (arranque), `docs/GUIA_DEFENSA.md` (arquitectura, guion de demo y preguntas de defensa) y `RESUMEN.md` sección 14.

## Tecnología
- Java 17, Spring Boot 3.5, Maven, Spring Data JPA, Spring Security con JWT y BCrypt, springdoc (Swagger), MySQL 8 (base `isw_practicas`, usuario de la app `isw_app`).
- La aplicación se conecta como `isw_app`, nunca como `root`. `ddl-auto=validate`: la app no crea ni altera tablas.
- Paquete base `cl.ubb.pem.practicas` con `controller, service, repository, entity, dto, security, exception, config`.

## Comandos
- Base de datos: `.\db\bd.ps1` (borra y recrea las tablas con los datos de demo) o `.\db\bd.ps1 -Accion Verificar`.
- Compilar y probar: `cd backend` y `mvn clean package` (146 pruebas). Sin pruebas: `-DskipTests`.
- Arrancar: `java -jar target\practicas-0.1.0-SNAPSHOT.jar`. Swagger en `http://localhost:8080/swagger-ui.html` (login → botón Authorize).

## Claves (regla estricta)
- Solo en variables de entorno de Windows: `ISW_DB_PASSWORD` (la app) e `ISW_DB_ROOT_PASSWORD` (solo administración). Opcional: `ISW_JWT_SECRET` (32 caracteres o más; si falta se genera uno temporal).
- Nunca escribirlas en archivos, comandos, mensajes ni en `application.properties`; usar `${ISW_DB_PASSWORD}`.
- `.env` está en `.gitignore`; `.env.example` solo lleva marcadores.
- La clave de los usuarios ficticios de demo (ver `db/seed.sql`) es solo para desarrollo: nunca como valor por defecto en el código ni en ningún sistema real. Las pruebas automáticas no la usan.

## Reglas del dominio ya implementadas (no cambiarlas sin avisar)
- 4 roles (ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR). El rol se lee de la base en cada petición. Cada endpoint restringe su rol con `@PreAuthorize`, y `AccesoService` decide "quién ve qué" (rol desconocido = no ve nada).
- Planificación: BORRADOR → EN_REVISION_GUIA → EN_AUTORIZACION → AUTORIZADA; el que devuelve debe comentar y el flujo repite desde el guía (S-10). **Sin guía** (ciclo inicial) va directo a EN_AUTORIZACION. Se envía con al menos 3 días de anticipación (configurable). Aproximación no entrega planificaciones.
- El guía (colaborador) solo existe en asignaturas que van a un colegio; en el ciclo inicial asignarlo da 400.
- Evaluación: BORRADOR → FINALIZADA (exige los 20 indicadores, calcula total y promedio, luego es inmutable). El borrador es privado del evaluador y del coordinador.
- Historial: el guía o el profesor del curso ve el recorrido completo de un estudiante asignado a él en alguna inscripción (S-21).

## Reglas de trabajo
- Responder siempre en español.
- Explicar lo que se hace, porque el usuario debe defender el proyecto.
- Ser breve en el chat.
- No hacer commits ni push si no se piden.
- Comprobar si algo ya está instalado antes de instalarlo.
- **Las pruebas de integración asumen los datos exactos de `db/seed.sql`.** Probar a mano o por HTTP deja filas nuevas: ejecutar `.\db\bd.ps1` antes de `mvn clean package` y antes de la defensa. Detener la aplicación al terminar de probar.
- Cambios de esquema: crear una migración nueva en `db/migraciones/` (se ejecuta una vez y como root) y actualizar también `db/schema.sql`, `db/seed.sql`, `db/verificacion.sql`, `Sprint1/mer.md`, las entidades y los DTO; anotarlo en `backend/PROGRESO.md`.
- En este entorno, los heredocs largos con Bash fallan: crear archivos con la herramienta Write. En PowerShell, el verificador de comandos a veces rechaza órdenes inofensivas (falsos positivos con Remove-Item o Set-Content inline): escribir un `.ps1` con Write y ejecutarlo aparte.
- Los supuestos S-01 a S-16 (`Sprint1/sprint-backlog.md`), S-34 a S-38 (allí mismo) y S-17 a S-33 (`backend/PROGRESO.md`) siguen sin confirmar con el cliente: marcar como SUPUESTO lo que se asuma.
- Lo que responda un estudiante es orientativo, no una confirmación del cliente: anotarlo como "reportado por estudiante, pendiente de confirmar". Preguntas pendientes y respuestas: `RESUMEN.md`, sección 13.
