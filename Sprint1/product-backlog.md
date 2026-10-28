# Product Backlog — Sistema de Formación Práctica PEM-UBB

| | |
|---|---|
| **Proyecto** | Sistema web para evaluar y monitorear las 6 prácticas de Pedagogía en Educación Matemática (UBB) |
| **Cliente** | La carrera PEM: Dr. Rodrigo Panes Chavarría y el coordinador de prácticas, Dr. Víctor Parra González |
| **Versión** | 1.3, actualizada el 10 de octubre de 2026 (Sprint 2: HU-31 y rol `JEFE_CARRERA`) |
| **De dónde sale** | La entrevista con el cliente, los protocolos y las pautas de la carpeta `Documents/`, las respuestas de un estudiante de PEM, el formato oficial de planificación (`FORMATO DE PLANIFICACIÓN DIDÁCTICA DIARIA.docx`) y la propuesta del chatbot "RetroPlani Mate" que ya usa el coordinador (`ChatBot (1).rar`) |
| **Por qué existe** | El criterio 3 de acreditación pide que la carrera *diseñe, implemente y monitoree un sistema para la formación práctica* que muestre un desarrollo creciente de la efectividad docente de sus estudiantes |

> Lo que el equipo decidió sin poder confirmarlo con el cliente aparece como **supuesto** (S-01, S-02...). Están todos en la sección 6 y con más detalle en `sprint-backlog.md`. Lo que contó un estudiante de PEM lo anotamos como "reportado por un estudiante": nos orienta, pero no es una confirmación del cliente.

---

## 1. Quiénes usan el sistema

| Rol | Quién es | Qué hace en el sistema |
|---|---|---|
| **Estudiante** | El practicante de PEM | Escribe sus planificaciones, es evaluado y mira su recorrido |
| **Profesor guía** | El profesor colaborador del colegio que lo recibe y lo evalúa | Revisa planificaciones y evalúa las clases con la pauta. En la entrevista lo llaman "guía" y en los documentos "colaborador": es la misma persona |
| **Profesor del curso** | El académico de la UBB a cargo de la asignatura | Autoriza las clases (supuesto S-05: podría ser alguien del colegio; el modelo lo permite porque es un rol aparte) |
| **Coordinador de prácticas** | El Dr. Víctor Parra | Administra el sistema y ve a todos los estudiantes |
| **Jefe de carrera** | El jefe de carrera de PEM | Solo lectura: consulta la cartola de prácticas (HU-31) como evidencia de acreditación. Rol propio desde el 10-oct (resuelve la duda de S-38 sobre su rol) |
| **Tutor** | Profesor contratado por la UBB para Práctica Profesional | Recién aparece en el Sprint 2 (HU-07) |

## 2. Cómo se ordena

Usamos prioridad **Alta / Media / Baja** con el criterio MoSCoW:

- **Alta (Must):** sin esto el sistema no resuelve el problema del cliente o no cumple la rúbrica.
- **Media (Should):** aporta valor, pero el sistema funciona sin ello.
- **Baja (Could):** sería bueno tenerlo.

La tabla va en orden de prioridad y los números de HU siguen ese mismo orden. Las HU-01 a HU-05 son las del Sprint 1.

## 3. Resumen

| ID | Historia | Épica | Prioridad | Puntos | Sprint | Estado |
|---|---|---|---|---|---|---|
| HU-01 | Iniciar sesión con roles | Acceso | Alta | 5 | **1** | Construida y probada (20-sep) |
| HU-02 | Registrar estudiantes e inscribirlos en una práctica | Gestión académica | Alta | 5 | **1** | Construida y probada (20-sep) |
| HU-03 | Evaluar una clase con la pauta digital (indicadores 1 a 7) | Evaluación | Alta | 5 | **1** | Construida y probada (20-sep) |
| HU-04 | Planificar una clase y que la aprueben en dos etapas | Planificación | Alta | 8 | **1** | Construida y probada (20-sep) |
| HU-05 | Ver el historial (recorrido) del estudiante | Monitoreo | Alta | 3 | **1** | Construida y probada (20-sep) |
| HU-06 | Configurar pautas de evaluación | Evaluación | Alta | por estimar | 2 | Pendiente |
| HU-07 | Pautas de Práctica Pedagógica y Profesional (con tutor) | Evaluación | Alta | por estimar | 2 | Pendiente |
| HU-08 | Informes por asignatura y por estudiante | Monitoreo | Alta | por estimar | 2 | Pendiente |
| HU-09 | Nota final ponderada de la asignatura | Evaluación | Media | por estimar | 2 | Pendiente |
| HU-10 | Prácticas en dupla y en grupo | Gestión académica | Media | por estimar | 2 | Pendiente |
| HU-11 | Nota de voz del profesor evaluador | Retroalimentación | Alta | 5 | **1** | **Construida y probada (22-sep, Fase E)** |
| HU-12 | Pasar la nota de voz a texto | Retroalimentación | Alta | 5 | **1** | **Construida y probada (22-sep, Fase E)** |
| HU-13 | Exportar notas finales para Adecca | Integración | Media | por estimar | 3 | Pendiente |
| HU-14 | Panel de monitoreo del coordinador | Monitoreo | Media | por estimar | 3 | Pendiente |
| HU-19 | Avisos por correo de cambios de estado | Planificación | Media (subió desde Baja) | por estimar | 3 (tentativo) | Pendiente |
| HU-15 | Chatbot de retroalimentación según el contexto del estudiante | Chatbot | Alta | por estimar | **2** (adelantada) | **Completa (26-oct):** probada con el modelo real de Groq (`openai/gpt-oss-120b`). Criterios 3 a 8 los cumple el modelo por instrucciones; el criterio 2 (no aceptar la planificación pegada como texto) ahora es una regla del código que responde sin llamar al modelo (S-72). Capa gratuita de Groq: 1-2 mensajes/min para todo el sistema, a decidir con el cliente (S-59) |
| HU-16 | El profesor guía ve el chat y la actividad del estudiante | Chatbot | Alta | por estimar | **2** (adelantada) | **Completa (26-oct)**; solo el guía de esa inscripción lee el chat (S-60) |
| HU-17 | Calibrar el estilo del chatbot con el coordinador | Chatbot | Media | por estimar | **2** (adelantada) | **Completa (26-oct):** pantalla "Configuración del asistente" (solo coordinador) que edita un texto único en la base (migración 008), con el texto base de RetroPlani Mate V3 adaptado (S-65); probada con el modelo real de Groq: el chatbot cambia de estilo desde el siguiente mensaje. Rige también en conversaciones en curso y no guarda versiones (S-68) |
| HU-20 | El jefe de carrera consulta los chats | Chatbot | Media | por estimar | **2** (adelantada) | **Completa (26-oct):** el jefe de carrera lee en solo lectura todos los chats de Práctica Pedagógica y Profesional, con lista filtrable; el estudiante ve en su chat que el jefe puede leer y cuándo lo hizo por última vez (criterio 3), y cada lectura queda en la base (`lectura_chat_jefe_carrera`, migración 010, criterio 4). Sigue siendo SUPUESTO S-66, a confirmar con el cliente |
| HU-18 | Registrar informes y productos de la práctica | Documentos | Baja | por estimar | 5 o más | Pendiente |
| HU-21 | Adjuntar documentos a la planificación | Documentos | Media | 3 | **1** | **Construida y probada (22-sep, Fase E)** |
| HU-22 | Adjuntar el documento formal de la evaluación | Documentos | Media | 3 | **1** | **Construida y probada (22-sep, Fase E)** |
| HU-23 | Buscar estudiante por nombre, RUT o correo | Monitoreo | Media | 2 | **1** | **Construida y probada (22-sep, Fase E)** |
| HU-24 | Accesos rápidos dentro del menú | Interfaz | Baja | 1 | **1** | **Construida (22-sep, Fase E)** |
| HU-25 | Gestión de archivos de pautas formales | Evaluación | Baja | por estimar | 2 o más | Pendiente |
| HU-26 | Cambiar la contraseña | Acceso | Media | por estimar | 2 | Pendiente |
| HU-27 | Configurar los parámetros del sistema | Administración | Baja | por estimar | 2 o más | Pendiente |
| HU-28 | Exportar el historial del estudiante en PDF | Monitoreo | Media | por estimar | 2 o más | Pendiente |
| HU-29 | Cargar la base de conocimiento del chatbot | Chatbot | Media | por estimar | **2** (adelantada) | **Completa (26-oct):** el coordinador sube o reemplaza los 10 documentos de la propuesta (Word/PDF, migración 009) y el chatbot recibe solo los fragmentos pertinentes, ordenados por TF-IDF (S-74; sin embeddings, S-70). Probada con el modelo real y documentos reales. **Limitación conocida, no pendiente de código:** que "responda solo con esos documentos" (criterio 2) lo cumple el modelo por instrucciones; garantizarlo exigiría otra arquitectura |
| HU-30 | Validar el chatbot con jueces expertos y estudiantes | Chatbot | Baja | por estimar | **2** | **Completa (26-oct):** validadores marcados y validación abierta/cerrada por el coordinador (con ella abierta, los demás reciben 409); los validadores responden en el sistema la Escala SUS y la rúbrica de 9 criterios de la propuesta RetroPlani Mate (migración 011) y el coordinador ve los resultados (S-75, a confirmar con el cliente) |
| HU-31 | Cartola de prácticas en PDF para el jefe de carrera | Monitoreo | Alta | por estimar | **2** | **Construida y probada (10-oct)** |

Los sprints 2 en adelante son **tentativos**: se replanifican al empezar cada uno con lo que aprendamos y lo que confirme el cliente. El chatbot queda para más adelante a propósito (supuesto S-02); la nota de voz y la transcripción (HU-11 y HU-12) se adelantaron al Sprint 1 el 22 de septiembre (Fase E), junto con los documentos adjuntos, la búsqueda de estudiantes y los accesos rápidos del menú (HU-21 a HU-24). `sprint-backlog.md`, sección 7, anota que se ampliaron los criterios de aceptación de HU-03 para incluir la nota de voz.

---

## 4. Historias de usuario

### Sprint 1

#### HU-01 · Iniciar sesión con roles
**Como** usuario del sistema (estudiante, profesor guía, profesor del curso, coordinador o jefe de carrera), **quiero** entrar con mi correo y mi contraseña y que el sistema solo me deje hacer lo que mi rol permite, **para** que la información de los estudiantes esté protegida y cada persona vea solo lo suyo.

**Prioridad:** Alta · **Viene de:** el requerimiento 6 (los guías acceden a lo que hace el estudiante) y la necesidad de separar a los actores.

**Criterios de aceptación**
1. Un usuario activo con sus credenciales correctas recibe un token que lo identifica a él y a su rol.
2. Con credenciales incorrectas recibe un error 401 que no dice si falló el correo o la contraseña.
3. Las contraseñas se guardan cifradas (BCrypt), nunca en texto plano ni en las respuestas.
4. Existen cinco roles: `ESTUDIANTE`, `PROFESOR_GUIA`, `PROFESOR_CURSO`, `COORDINADOR` y `JEFE_CARRERA` (este último agregado el 10-oct con HU-31).
5. Si el rol no tiene permiso para un recurso, recibe 403. Si no manda token, recibe 401.
6. Un usuario inactivo no puede entrar.
7. El token vence a las 8 horas (supuesto S-06).

#### HU-02 · Registrar estudiantes e inscribirlos en una práctica
**Como** coordinador de prácticas, **quiero** registrar estudiantes y profesores, ver las 6 asignaturas de práctica e inscribir a cada estudiante en una asignatura por periodo con su profesor guía y su profesor del curso, **para** que cada planificación y cada evaluación quede ligada al recorrido de ese estudiante.

**Prioridad:** Alta · **Viene de:** el requerimiento 1 (un solo sistema para las 6 prácticas).

**Criterios de aceptación**
1. El coordinador registra un estudiante con nombre, correo, RUT, semestre actual y año de ingreso, y el sistema crea su usuario (rol `ESTUDIANTE`) y su ficha. El correo y el RUT no se pueden repetir.
2. El coordinador también registra profesores con rol `PROFESOR_GUIA` o `PROFESOR_CURSO`.
3. Se puede listar y buscar estudiantes por nombre o RUT, y ver la ficha de uno.
4. Las 6 asignaturas ya vienen cargadas, con su ciclo (inicial, intermedio o avanzado), su modalidad (individual, dupla o grupo), si van a un colegio, si piden planificaciones y si el chatbot aplica (solo las dos finales).
   - Reportado por un estudiante (S-16): Aproximación al Sistema Escolar es en **grupos de 4** y no entrega planificaciones; Ambientes de Aprendizaje es en **parejas** y sí las entrega.
5. Al inscribir a un estudiante en una asignatura y un periodo (por ejemplo `2026-2`), la inscripción queda `EN_CURSO`.
6. No se puede inscribir dos veces al mismo estudiante en la misma asignatura y periodo (error 409).
7. El guía debe tener rol `PROFESOR_GUIA` y el profesor del curso, `PROFESOR_CURSO` (si no, error 400). Las asignaturas del ciclo inicial no van a un colegio y **no tienen guía**: asignarlo da 400. En las demás el guía es obligatorio.
8. Solo el coordinador puede hacer todo esto; los demás roles reciben 403.

#### HU-03 · Evaluar una clase con la pauta digital
**Como** profesor guía, **quiero** completar en el sistema la pauta de observación de clase con sus indicadores de 1 a 7, **para** dejar de usar la pauta en papel y que el puntaje se calcule solo.

**Prioridad:** Alta · **Viene de:** el requerimiento 2 (digitalizar la pauta).

**Alcance:** en este sprint solo la *pauta de observación de clase del profesor colaborador* de Trabajo Colaborativo en el Aula 2026: 20 indicadores en 3 dimensiones (estructura de la clase, tareas matemáticas, y ambiente de aula y recursos). Las otras pautas van en HU-06 y HU-07 (supuesto S-07).

**Criterios de aceptación**
1. El guía puede ver la pauta de una asignatura con sus dimensiones e indicadores, en orden.
2. Sobre una inscripción que tiene asignada, crea una evaluación con fecha, establecimiento, curso, hora de inicio y de término, y objetivo de la clase. Queda en `BORRADOR`.
3. El puntaje de cada indicador es un número entero entre 1 y 7 (el rango viene de la pauta). Un valor fuera de rango, o de un indicador que no es de esa pauta, se rechaza con 400.
4. Puede escribir observaciones generales (fortalezas y aspectos a mejorar).
5. Al finalizar, el sistema exige los 20 indicadores puntuados, calcula el **puntaje total** y el **promedio**, y la evaluación pasa a `FINALIZADA`: desde ahí no se modifica.
6. Solo el guía asignado a esa inscripción puede evaluarla; otro guía u otro rol recibe 403.
7. Queda fuera de este sprint convertir el puntaje en la nota oficial de la asignatura (supuesto S-08).

#### HU-04 · Planificar una clase y que la aprueben en dos etapas
**Como** estudiante, **quiero** escribir la planificación de mi clase con el formato de la carrera y presentarla a mi profesor guía y luego a mi profesor del curso, **para** hacer la clase solo cuando esté retroalimentada y autorizada.

**Prioridad:** Alta · **Viene de:** el requerimiento 4 y el "Planificación!!!" que el cliente destacó en la entrevista.

**Formato real de la carrera (S-09 resuelto, 22-sep):** la carrera entregó `FORMATO DE PLANIFICACIÓN DIDÁCTICA DIARIA.docx`. Tiene más campos que los cinco de la entrevista: nombre del profesor/a, establecimiento, año, período (semestre), asignatura, curso, unidad, objetivo de aprendizaje/indicadores de evaluación, objetivo de aprendizaje actitudinal, objetivo de la clase, fecha, duración, recursos, instrumentos de evaluación, y la descripción de la clase en tres partes (inicio, desarrollo y cierre, cada una con su propia duración). **S-09 ya no es un supuesto sobre qué pide la carrera** (el archivo lo confirma), pero el Sprint 1 sigue construido y probado con los cinco elementos de la entrevista: migrar `planificacion` a los campos reales es trabajo nuevo (tocaría la entidad, los DTO, el formulario y las 146 pruebas existentes) y queda para un sprint futuro, marcado como supuesto **S-45** para no arriesgar una historia ya entregada.

**Criterios de aceptación**
1. El estudiante con una inscripción `EN_CURSO` crea una planificación con el formato de la carrera: título, fecha de la clase, **objetivo**, **recursos**, **tiempos**, **evaluación del aprendizaje** y **cómo interactúan los participantes con los recursos**. Queda en `BORRADOR`. (Estos cinco elementos venían de la entrevista, antes de tener el archivo oficial; ver el aviso de arriba y S-45.)
2. Un borrador puede estar incompleto, pero para enviarlo todos los elementos son obligatorios.
3. Al enviarla pasa a `EN_REVISION_GUIA`.
4. El guía asignado puede **devolverla con observaciones** (el comentario es obligatorio y pasa a `CON_OBSERVACIONES`) o **aprobarla** (pasa a `EN_AUTORIZACION`).
5. El estudiante solo edita la planificación cuando está en `BORRADOR` o `CON_OBSERVACIONES`, y la reenvía al guía.
6. El profesor del curso asignado puede **autorizarla** (pasa a `AUTORIZADA`) o devolverla con comentario obligatorio. Si la devuelve, el estudiante corrige y el flujo vuelve a empezar desde el guía (supuesto S-10).
7. Cada decisión queda guardada con quién la tomó, cuándo, en qué etapa (guía o curso) y con qué comentario, y se puede ver en orden cronológico.
8. Una planificación `AUTORIZADA` no se puede modificar. Cualquier cambio de estado que no esté permitido da 409.
9. El guía ve todas las planificaciones de sus estudiantes, en cualquier estado.
10. **Sin profesor guía** (ciclo inicial: Ambientes de Aprendizaje no va a un colegio), al enviarla la planificación pasa **directo a `EN_AUTORIZACION`** y la autoriza el profesor del curso (S-34). Aproximación al Sistema Escolar no entrega planificaciones: crear una da 409.
11. Se entrega con **al menos 3 días de anticipación** a la clase; si no, al enviarla da 400. El número de días se puede configurar (S-35). Se planifican todas las clases con docencia directa, una planificación por clase (S-36).
12. Además de completar el formulario, el estudiante debe adjuntar la planificación formal en Word o PDF (ver HU-21): sin al menos un documento adjunto, enviarla a revisión da 400 (los cinco elementos del formulario quedan como resumen; decisión propia, no confirmada por el cliente, **S-47**).

**Diferencia sin resolver (S-37):** un estudiante contó que la planificación se manda "al profe del ramo y al colaborador", quizás a los dos a la vez. El cliente la describió en secuencia (primero el guía, luego el profesor del curso). Mientras nadie confirme otra cosa, implementamos la secuencia.

#### HU-05 · Ver el historial (recorrido) del estudiante
**Como** estudiante, profesor guía o coordinador, **quiero** ver el recorrido completo de un estudiante por sus prácticas, **para** ver cómo evoluciona en el tiempo y no solo una nota final.

**Prioridad:** Alta · **Viene de:** el requerimiento 1 (el recorrido "de toda su vida de practicante"), el requerimiento 6 y el criterio 3 (desarrollo creciente).

**Criterios de aceptación**
1. Al consultar el historial de un estudiante, se listan sus inscripciones en orden cronológico con asignatura, ciclo, periodo, estado y nota final (si ya tiene).
2. Cada inscripción trae sus evaluaciones finalizadas (fecha, pauta, puntaje total y promedio) y sus planificaciones con su estado actual.
3. Se puede abrir el detalle de una evaluación y ver el puntaje de cada indicador.
4. La respuesta incluye el **semestre actual** y las **asignaturas aprobadas**: es el contexto que usará el chatbot más adelante.
5. Cada uno ve solo lo que le corresponde: el estudiante, su propio historial; el guía, el de los estudiantes que tiene asignados; el profesor del curso, el de sus inscripciones; el coordinador, el de todos. Cualquier otro acceso da 403.

---

### Adelantadas al Sprint 1 el 22 de septiembre (Fase E)

Estas seis historias iban para sprints futuros en la planificación original, pero se adelantaron y ya están construidas y probadas: HU-11 y HU-12 porque el requerimiento 3 del cliente (nota de voz) es sencillo de construir sobre la tabla `NOTA_VOZ` que ya existía, y HU-21 a HU-24 porque son necesidades que ya habían aparecido (documentos adjuntos, búsqueda de estudiantes) o mejoras directas a la interfaz del Sprint 1.

#### HU-11 · Nota de voz del profesor evaluador
**Como** profesor guía evaluador, **quiero** grabar una nota de voz al terminar la clase y dejarla ligada a la evaluación, **para** usarla después en la retroalimentación con el estudiante.
- **Prioridad:** Alta · **Sprint:** 1 · **Estado:** Construida y probada (22-sep) · **Viene de:** el requerimiento 3 del cliente.
- **Criterios:** (1) como máximo una nota de voz por evaluación; (2) solo el profesor guía evaluador de esa evaluación graba o reemplaza la nota (403 para cualquier otro, incluido otro guía); (3) se puede grabar con la evaluación en `BORRADOR` o ya `FINALIZADA`, porque es retroalimentación complementaria y no cambia el puntaje (**S-42**); (4) la escuchan el evaluador, el estudiante evaluado, su profesor del curso y el coordinador (las mismas reglas de acceso de HU-03); (5) el audio se guarda en disco, fuera del repositorio, con la ruta en la base de datos; tamaño y formato máximos son un supuesto propio, no confirmado por el cliente (**S-43**).

#### HU-12 · Pasar la nota de voz a texto
**Como** profesor guía evaluador, **quiero** convertir la nota de voz en texto, **para** revisarla, corregirla y entregársela al estudiante por escrito.
- **Prioridad:** Alta · **Sprint:** 1 · **Estado:** Construida y probada (22-sep) · **Depende de** HU-11.
- **Criterios:** (1) la transcripción inicial la genera el navegador del evaluador (Web Speech API de Chrome) al momento de grabar, y se manda junto con el audio: es una elección propia, sin confirmar con el cliente qué herramienta de transcripción usar (**S-40**); (2) el evaluador puede corregir el texto después, y el sistema marca si ya fue corregido; (3) si el navegador no soporta grabación o reconocimiento de voz, se avisa claramente y el resto de la pantalla de evaluación sigue funcionando.

#### HU-21 · Adjuntar documentos a la planificación
**Como** estudiante, **quiero** subir uno o más documentos a mi planificación (la planificación formal y, si quiero, guías, presentaciones u otros materiales), **para** dejar el respaldo formal junto con el formulario.
- **Prioridad:** Media · **Sprint:** 1 · **Estado:** Construida y probada (22-sep; obligatorio para enviar desde el 22-sep, S-47) · **Depende de** HU-04.
- **Criterios:** (1) solo el estudiante dueño de la planificación sube archivos; (2) Word (.docx) o PDF, máximo 10 MB (400 si no cumple, tamaño y formatos son supuesto propio, **S-44**); (3) quien ya puede ver la planificación (el mismo estudiante, su guía, su profesor del curso y el coordinador) puede listar y descargar los documentos; (4) se guarda quién subió cada archivo y cuándo; (5) **es obligatorio al menos un documento para enviar la planificación a revisión** (HU-04, criterio 12): basta con que exista uno, no hay que volver a subirlo en cada reenvío (**S-47**).

#### HU-22 · Adjuntar el documento formal de la evaluación
**Como** profesor guía, **quiero** subir el documento con la pauta y evaluación formal de una clase, **para** dejar el respaldo escrito junto a la evaluación digital.
- **Prioridad:** Media · **Sprint:** 1 · **Estado:** Construida y probada (22-sep) · **Depende de** HU-03.
- **Criterios:** (1) solo el profesor guía evaluador de esa evaluación sube documentos; (2) Word (.docx), Excel (.xlsx) o PDF, máximo 10 MB; (3) se puede subir con la evaluación en `BORRADOR` o ya `FINALIZADA`, sin exigir un estado particular (**S-41**); (4) quien ya puede ver la evaluación (mismas reglas de HU-03: el evaluador, y si está finalizada también el estudiante, su profesor del curso y el coordinador) puede listar y descargar.

#### HU-23 · Buscar estudiante por nombre, RUT o correo
**Como** coordinador, profesor guía o profesor del curso, **quiero** filtrar la lista de estudiantes por nombre, RUT o correo al elegir a quién ver el historial, **para** encontrarlo rápido cuando tengo muchos estudiantes asignados.
- **Prioridad:** Media · **Sprint:** 1 · **Estado:** Construida (22-sep) · **Depende de** HU-02 y HU-05.
- **Criterios:** (1) la búsqueda no distingue mayúsculas ni tildes; (2) el coordinador reutiliza el parámetro `buscar` de `GET /api/estudiantes`; (3) el guía y el profesor del curso filtran entre los estudiantes de sus propias inscripciones.

#### HU-24 · Accesos rápidos dentro del menú
**Como** cualquier usuario, **quiero** ver mis accesos rápidos también dentro del menú lateral (no solo en Inicio), **para** saltar de una función a otra sin volver primero a la pantalla de inicio.
- **Prioridad:** Baja · **Sprint:** 1 · **Estado:** Construida (22-sep).
- **Criterios:** (1) las mismas tarjetas de accesos rápidos de Inicio aparecen como enlaces en una sección del menú lateral, según el rol; (2) el menú ya no menciona sprints, historias de usuario ni funciones futuras: solo los nombres de las funciones a las que el usuario tiene acceso.

---

### Sprint 2

#### HU-31 · Cartola de prácticas
**Como** jefe de carrera, **quiero** descargar en PDF una cartola, al estilo de una cartola bancaria, con todas las inscripciones de práctica que se cerraron en un rango de fechas, **para** tener evidencia agregada del sistema de formación práctica ante la acreditación (criterio 3) sin hacer el trabajo operativo del coordinador.
- **Prioridad:** Alta · **Sprint:** 2 · **Estado:** Construida y probada (10-oct) · **Depende de** HU-02 y HU-03 · **Viene de:** la necesidad de evidencia para la acreditación; es el diferenciador del equipo para el Sprint 2.

**Criterios de aceptación**
1. Existe el rol `JEFE_CARRERA`, de solo lectura. Su único acceso es la cartola: cualquier otra función del sistema le responde 403 (resuelve la duda de S-38 sobre su rol).
2. `GET /api/reportes/cartola-practicas?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` responde solo al `JEFE_CARRERA`; los otros 4 roles reciben 403 y sin token, 401.
3. Entran las inscripciones cuya **fecha de cierre** está entre `desde` y `hasta`, ambas incluidas. Las que siguen `EN_CURSO` no tienen fecha de cierre y no aparecen. Si `desde` es posterior a `hasta`, o una fecha está mal escrita, responde 400.
4. Cada fila trae: estudiante (nombre y RUT), asignatura, periodo, profesor guía ("Sin guía" en el ciclo inicial), profesor del curso, estado, fecha y nota de cierre, cantidad de evaluaciones finalizadas y el promedio de sus promedios.
5. Es un PDF descargable con encabezado (rango del reporte y fecha de generación), el total de inscripciones y una fila por inscripción. Si el rango no tiene cierres, el PDF lo dice en vez de mostrar una tabla vacía.
6. Es solo lectura: no crea ni modifica nada.
7. Al cerrar una inscripción (HU-02), el sistema guarda la fecha del día como fecha de cierre. A las inscripciones cerradas antes de esta historia se les dedujo la fecha a partir del periodo (**S-48**).

---

### Sprints siguientes (tentativos)

#### HU-06 · Configurar pautas de evaluación
**Como** coordinador, **quiero** crear y editar pautas con sus dimensiones, indicadores, escala y tipo de evaluador, **para** no depender de un programador cada vez que la carrera cambia un instrumento.
- **Prioridad:** Alta · **Sprint:** 2
- **Criterios:** (1) una pauta define su escala mínima y máxima, quién la aplica (colaborador, tutor o académico) y su ámbito (Especialidad, Orientación u observación de clase); (2) si se modifica una pauta que ya se usó, se crea una versión nueva y las evaluaciones anteriores conservan la suya; (3) solo el coordinador las gestiona.

#### HU-07 · Pautas de Práctica Pedagógica y Profesional (con tutor)
**Como** profesor guía o tutor, **quiero** completar las pautas de Especialidad y de Orientación (4 por estudiante en Práctica Pedagógica y 8 en Práctica Profesional), **para** eliminar el papel en las dos prácticas finales.
- **Prioridad:** Alta · **Sprint:** 2
- **Criterios:** (1) soporta escala de 2 a 7 y dimensiones de la A a la E; (2) cada evaluación indica si es la primera o la segunda observación; (3) el tutor solo evalúa con pautas de tutor; (4) el puntaje máximo de cada pauta se calcula con sus indicadores.

#### HU-08 · Informes por asignatura y por estudiante
**Como** coordinador, **quiero** informes por asignatura y por estudiante con una síntesis de sus evaluaciones y planificaciones, **para** no juntar a mano información dispersa y tener evidencia para la acreditación.
- **Prioridad:** Alta · **Sprint:** 2
- **Criterios:** (1) informe por estudiante con todas sus prácticas; (2) informe por asignatura y periodo con promedios y distribución de puntajes; (3) descarga en PDF o Excel.

#### HU-09 · Nota final ponderada de la asignatura
**Como** profesor del curso, **quiero** que el sistema calcule la nota final con la estructura evaluativa de la asignatura, **para** no sacar el promedio a mano.
- **Prioridad:** Media · **Sprint:** 2 · **Depende de** que el cliente confirme las fórmulas (en Práctica Pedagógica: 27 %, 18 %, 8 % y 47 %).
- **Criterios:** (1) los pesos se configuran por asignatura; (2) la nota queda guardada en la inscripción; (3) la escala de notas se confirma con el cliente.

#### HU-10 · Prácticas en dupla y en grupo
**Como** coordinador, **quiero** agrupar a los estudiantes en duplas (Ambientes de Aprendizaje, Trabajo Colaborativo, Investigación en el Aula) o en grupos de 4 (Aproximación al Sistema Escolar), **para** trabajar y planificar en conjunto como pide el proceso.
- **Prioridad:** Media · **Sprint:** 2
- **Criterios:** (1) una dupla comparte guía, colegio y curso; (2) **cada integrante tiene su propia pauta de evaluación** (reportado por un estudiante; el modelo de datos ya lo cumple); (3) el historial de cada uno muestra con quién trabajó.

#### HU-13 · Exportar notas finales para Adecca
**Como** profesor del curso, **quiero** exportar las notas finales en un archivo, **para** subirlas a Adecca sin volver a digitarlas.
- **Prioridad:** Media · **Sprint:** 3 · **Depende de** conocer el formato que acepta Adecca.
- **Criterios:** (1) una exportación por asignatura y periodo; (2) solo incluye evaluaciones finalizadas.

#### HU-14 · Panel de monitoreo del coordinador
**Como** coordinador, **quiero** un panel con el avance de cada estudiante y de cada asignatura, **para** monitorear el sistema de formación práctica (criterio 3).
- **Prioridad:** Media · **Sprint:** 3
- **Criterios:** (1) muestra planificaciones pendientes, evaluaciones hechas y estudiantes sin evaluar; (2) se puede filtrar por periodo y asignatura.

#### HU-19 · Avisos por correo de cambios de estado
**Como** guía o profesor del curso, **quiero** que me avisen por correo cuando llegue una planificación a mi revisión, **para** responder a tiempo.
- **Prioridad:** Media (subió desde Baja el 20-sep) · **Sprint:** 3 (tentativo)
- **Por qué subió:** un estudiante contó que hoy toda la retroalimentación de las planificaciones va **por correo "para que quede respaldo"**. Las revisiones del sistema ya guardan ese respaldo (autor, fecha y comentario); el correo sería solo el aviso, y cómo se envía hay que definirlo con el cliente.

#### HU-15 · Chatbot de retroalimentación según el contexto del estudiante
**Como** estudiante de Práctica Pedagógica o Profesional, **quiero** conversar con un chatbot que retroalimente la planificación que yo ya escribí, según mi semestre y mis asignaturas aprobadas, **para** mejorar mis planificaciones y mi desempeño.
- **Prioridad:** Alta · **Sprint:** 4 (supuesto S-02) · **Contenido corregido el 22-sep** con la propuesta real **"RetroPlani Mate"** (`Documents/ChatBot (1).rar`), el GPT que ya usa el coordinador Víctor Parra para retroalimentar planificaciones de matemática.
- **Criterios:**
  1. Solo está disponible en las dos prácticas finales; usa como contexto el semestre y las asignaturas aprobadas (los entrega HU-05).
  2. El estudiante adjunta su planificación como archivo o imagen (.docx, .pdf, .png, .jpg o .jpeg); si la pega como texto en el chat, el chatbot no la retroalimenta y pide el archivo.
  3. El chatbot **nunca diseña una planificación desde cero**: si el estudiante pide eso ("hazme el inicio", "dame una actividad lista"...), responde con un mensaje fijo de límite y no entrega ningún componente listo para copiar, ni antes ni después de que el estudiante insista.
  4. Antes de retroalimentar hace una verificación documental: qué recursos declara la planificación (guía, PPT, ticket de salida...) y cuáles fueron realmente adjuntados; si falta alguno, lo avisa antes de seguir (o detiene el flujo si es indispensable para evaluar la coherencia).
  5. Flujo dialógico: hace hasta 5 preguntas diagnósticas sobre los puntos débiles o ambiguos antes de la retroalimentación final, y la retroalimentación final integra las respuestas del estudiante.
  6. La retroalimentación revisa coherencia curricular, los tres momentos de la clase (inicio, desarrollo, cierre), Diseño Universal para el Aprendizaje y matemática inclusiva, evaluación para el aprendizaje, recursos didácticos, y errores/dificultades matemáticas; usa la rúbrica de planificación solo como referencia cualitativa y **nunca asigna puntaje ni nota**.
  7. Cierra con preguntas de reflexión para la acción de mejora, y admite como máximo dos retroalimentaciones completas para la misma clase (con el mismo objetivo y recursos).
  8. Responde en español, con tono cercano y formativo (nunca descalificador), y dice explícitamente cuando la información es insuficiente.
  9. Guarda cada mensaje del chat, por inscripción.
- **Uso esperado (reportado por un estudiante):** preguntarle formas de enseñar un contenido y cómo mejorar una planificación.

#### HU-16 · El profesor guía ve el chat y la actividad del estudiante
**Como** profesor guía, **quiero** ver las planificaciones, evaluaciones y conversaciones de mis estudiantes, **para** ayudarlos a mejorar y acompañarlos.
- **Prioridad:** Alta · **Sprint:** 4 · **Depende de** HU-15. La parte de planificaciones y evaluaciones ya queda cubierta en el Sprint 1 (HU-04 y HU-05).
- **Por decidir con el cliente (S-38):** el requerimiento 6 dice que el guía accede a todo, pero un estudiante contó que le molestaría que su guía lea todo lo que conversa con el chatbot. Opciones: que el guía vea el chat completo (con aviso al estudiante), solo un resumen, o solo lo que el estudiante decida compartir.
- **Criterios:** (1) el guía ve el chat de sus estudiantes asignados y de nadie más; (2) el estudiante sabe que su guía puede verlo.

#### HU-17 · Calibrar el estilo de retroalimentación del chatbot
**Como** coordinador, **quiero** ajustar los criterios y el estilo con que retroalimenta el chatbot, **para** que se parezca a la retroalimentación que doy hoy con mi chatbot de ChatGPT.
- **Prioridad:** Media · **Sprint:** 4 · **Contenido corregido el 22-sep:** el estilo base **ya se conoce** (son las instrucciones completas del GPT "RetroPlani Mate", `ChatBot (1).rar`): tono cercano y formativo, expresiones a usar ("Falta incorporar…", "Considera…") y a evitar ("Está mal…", "No sirve…"), y los 8 casos de respuesta modelo (solo objetivo, objetivo + un momento, faltan recursos, piden diseñar, etc.). Ya no depende de que el coordinador aporte los criterios desde cero: el coordinador solo **calibra** sobre esa base.
- **Criterios:** (1) el coordinador puede editar el tono y las expresiones permitidas/evitadas sin tocar código; (2) puede revisar y ajustar los casos de respuesta modelo; (3) los cambios aplican a las conversaciones nuevas, no reescriben las ya guardadas.

#### HU-20 · El jefe de carrera consulta los chats
**Como** jefe de carrera, **quiero** consultar las conversaciones de los estudiantes con el chatbot, **para** llevar un registro y saber cómo ha aportado el chatbot.
- **Prioridad:** Media · **Sprint:** 4 · **Depende de** HU-15 · **Viene de:** una solicitud del jefe de carrera (20-sep).
- **Criterios (borrador):** (1) puede listar las conversaciones por estudiante, asignatura y fecha (los mensajes ya se guardan por inscripción); (2) solo lectura; (3) el estudiante ve un aviso de que el jefe de carrera puede leer sus conversaciones; (4) cada acceso queda registrado.
- **S-38, parte del rol: resuelta el 10-oct.** El equipo decidió que el jefe de carrera tiene un rol propio, `JEFE_CARRERA` (creado con HU-31), porque no hace el trabajo operativo del coordinador. Sigue abierta la otra parte de S-38: qué ve cada rol de los chats.
- **Ojo con la privacidad:** es la misma tensión de HU-16. Hay que definir con el cliente qué se muestra (chat completo, resumen o datos agregados).

#### HU-18 · Registrar informes y productos de la práctica
**Como** estudiante, **quiero** subir mi informe final, mi diario de reflexión y mi proyecto de intervención, **para** que queden en mi recorrido.
- **Prioridad:** Baja · **Sprint:** 5 o más

#### HU-25 · Gestión de archivos de pautas formales
**Como** coordinador, **quiero** subir y reemplazar los archivos oficiales de cada pauta de evaluación (PDF o Word firmados y timbrados), **para** tener siempre a mano el documento formal detrás de cada instrumento digitalizado.
- **Prioridad:** Baja · **Sprint:** 2 o más · **Depende de** HU-06.
- **Criterios:** (1) el coordinador sube un archivo por pauta y puede reemplazarlo; (2) queda un historial de versiones (quién y cuándo); (3) quien puede ver la pauta puede descargar su archivo oficial.

#### HU-26 · Cambiar la contraseña
**Como** cualquier usuario, **quiero** cambiar mi contraseña desde la aplicación, **para** no depender de que el coordinador me asigne una nueva.
- **Prioridad:** Media · **Sprint:** 2 · **Viene de:** limitación conocida del Sprint 1 (no hay cambio ni recuperación de clave, ver `backend/PROGRESO.md`).
- **Criterios:** (1) pide la clave actual y la nueva; (2) la nueva clave cumple el mismo formato mínimo que al registrar un usuario; (3) las sesiones con el token anterior siguen válidas hasta que venzan (no hay revocación en el Sprint 1).

#### HU-27 · Configurar los parámetros del sistema
**Como** coordinador, **quiero** ajustar parámetros como los días mínimos de anticipación de una planificación o el tamaño máximo de los archivos adjuntos, **para** no depender de un programador cada vez que la carrera cambia una regla operativa.
- **Prioridad:** Baja · **Sprint:** 2 o más · **Viene de:** hoy `app.planificacion.dias-anticipacion-minimos` y los límites de tamaño de archivo están fijos en `application.properties`.
- **Criterios:** (1) los parámetros se editan desde una pantalla, no desde el código; (2) el cambio aplica de inmediato a las peticiones siguientes; (3) solo el coordinador los edita.

#### HU-28 · Exportar el historial del estudiante en PDF
**Como** coordinador o profesor guía, **quiero** descargar el historial de un estudiante en PDF, **para** usarlo como evidencia de la acreditación (criterio 3) sin tener que hacer capturas de pantalla.
- **Prioridad:** Media · **Sprint:** 2 o más · **Depende de** HU-05 y HU-08.
- **Criterios:** (1) el PDF trae las mismas inscripciones, evaluaciones y planificaciones que la pantalla de historial; (2) respeta las mismas reglas de acceso que ver el historial; (3) incluye la fecha de generación.

#### HU-29 · Cargar y mantener la base de conocimiento del chatbot
**Como** coordinador, **quiero** cargar y actualizar los documentos que usará el chatbot para retroalimentar (rúbrica de planificación, ejemplos de acciones DUA, bases curriculares de Matemática, banco de comentarios de retroalimentación), **para** que responda con los mismos criterios que usa hoy el coordinador con su chatbot de ChatGPT.
- **Prioridad:** Media · **Sprint:** 4 · **Depende de** HU-15 y HU-17 · **Viene de:** la propuesta "RetroPlani Mate" (`ChatBot (1).rar`), sección "Archivos que deberías cargar".
- **Criterios:** (1) el coordinador sube o reemplaza cada documento de referencia; (2) el chatbot responde solo con la información de esos documentos cargados (Prototipo 1, sin navegación web, igual que la propuesta original); (3) queda registro de cuándo se actualizó cada archivo.

#### HU-30 · Validar el chatbot con jueces expertos y estudiantes
**Como** coordinador, **quiero** que el chatbot pase por una validación con jueces expertos (Escala de Usabilidad SUS y una rúbrica de 9 criterios pedagógicos) y luego con un grupo de estudiantes de 4º y 5º año, **para** asegurar su calidad pedagógica, didáctica e inclusiva antes de liberarlo a todos.
- **Prioridad:** Baja · **Sprint:** 5 o más · **Depende de** HU-15 a HU-17 y HU-29 · **Viene de:** la propuesta "RetroPlani Mate" (`ChatBot (1).rar`), que ya planea esta validación entre septiembre y octubre de 2026 con dos grupos de Práctica Pedagógica y Práctica Profesional.
- **Criterios (borrador):** (1) se puede marcar un grupo de usuarios como "validadores"; (2) se registran las respuestas de la Escala SUS y de la rúbrica de juicio experto; (3) el chatbot no se habilita para el resto de los estudiantes hasta cerrar esta etapa; a definir con el cliente si esto se hace dentro del sistema o fuera de él.

---

## 5. De los requerimientos del cliente a las historias

| # | Lo que pidió el cliente | Historias que lo cubren |
|---|---|---|
| 1 | Un solo sistema para evaluar y monitorear las 6 prácticas, con el recorrido completo | HU-02, HU-05, HU-08, HU-14, HU-23, HU-28, HU-31 |
| 2 | Digitalizar la pauta de evaluación | HU-03, HU-06, HU-07, HU-09, HU-22, HU-25 |
| 3 | Nota de voz con opción de pasarla a texto | HU-11, HU-12 |
| 4 | Flujo de planificación (estudiante, guía y profesor del curso; sin guía en el ciclo inicial) | HU-04, HU-19, HU-21 |
| 5 | Chatbot según el semestre y los ramos aprobados, solo en las prácticas finales | HU-15, HU-17, HU-20, HU-29, HU-30 (el contexto lo da HU-05) |
| 6 | Que los profesores guía accedan a todo lo del estudiante | HU-05, HU-16 (la privacidad del chat está por decidir, S-38) |
| — | Roles y protección de datos (necesidad de todo el sistema) | HU-01, HU-26 |
| — | Terminar con la doble digitación en Adecca | HU-03, HU-13 |
| — | Otras necesidades identificadas por el equipo, no pedidas explícitamente | HU-18, HU-24, HU-27 |

## 6. Supuestos que afectan a este backlog

Hay que confirmarlos con el cliente o con la profesora. Los números son los mismos en todos los documentos.

| ID | Qué asumimos |
|---|---|
| S-01 | El Sprint 1 entrega solo **backend y base de datos** (API con Spring Boot y MySQL), sin pantallas. La demostración se hace con Swagger. *(La Fase D, 21-sep, agregó igual un front-end web sin cambiar el alcance del Sprint 1.)* |
| S-02 | El **chatbot** queda para sprints posteriores. *(La **nota de voz** se adelantó al Sprint 1 el 22-sep, Fase E: ver HU-11 y HU-12.)* |
| S-05 | El **profesor del curso** que autoriza la clase es el académico UBB de la asignatura. Un estudiante contó que se manda "al profe del ramo y al colaborador" |
| S-06 | La sesión dura 8 horas y usamos nuestro propio inicio de sesión (no el de la UBB) |
| S-07 | La primera pauta que se digitaliza es la de observación de clase del colaborador (20 indicadores, escala 1 a 7) |
| S-08 | En el Sprint 1 se calcula puntaje total y promedio; la conversión a nota oficial se define después |
| S-09 | **Resuelto el 22-sep** (ya no es un supuesto sobre qué campos pide la carrera): la carrera entregó `FORMATO DE PLANIFICACIÓN DIDÁCTICA DIARIA.docx`. Sigue como supuesto **S-45** que el Sprint 1 no migró todavía `planificacion` a esos campos reales |
| S-10 | Si el profesor del curso no autoriza, la planificación vuelve al estudiante y el flujo empieza otra vez desde el guía. Un estudiante dijo "debes mejorar tu planificación", sin aclarar si vuelve primero al guía |
| S-11 | Cada usuario tiene un solo rol y cada evaluación pertenece a una inscripción. Un estudiante confirmó que en pareja cada alumno tiene su propia pauta |
| S-12 | El tutor y las 8 pautas de Práctica Profesional entran en el Sprint 2 |
| S-14 | El semestre y las asignaturas aprobadas los ingresa el coordinador. Un estudiante contó que hoy se ven en la malla de la Intranet; importarlos de ahí queda como mejora futura |
| S-16 | Aproximación es en grupos de 4 y sin planificaciones; Ambientes es en parejas y con planificaciones; ninguna va a un colegio, así que no tienen guía (corregido el 20-sep según un estudiante) |
| S-34 a S-38 | Sin guía la planificación va directo al profesor del curso; se entrega con 3 días de anticipación; una planificación por clase; el flujo en secuencia (guía y luego curso) sigue en discusión; la privacidad de los chats sigue abierta (S-38). **El rol del jefe de carrera se resolvió el 10-oct: es un rol propio, `JEFE_CARRERA` (HU-31)**. Detalle en `sprint-backlog.md`, sección 7 |
| S-39 | Generación completa de 20 estudiantes de demostración (`db/seed.sql`): perfiles de avance, y a qué guía o profesor del curso se asigna cada uno, son datos inventados por el equipo, no vienen del cliente |
| S-40 | La transcripción inicial de la nota de voz (HU-12) la genera el navegador del evaluador (Web Speech API de Chrome), sin costo y sin instalar nada: nadie ha confirmado con el cliente qué herramienta de transcripción usar |
| S-41 | El documento formal de la evaluación (HU-22) se puede subir con la evaluación en `BORRADOR` o ya `FINALIZADA`: no se exige un estado particular, porque el cliente no lo especificó |
| S-42 | La nota de voz (HU-11) se puede grabar con la evaluación en `BORRADOR` o ya `FINALIZADA`, porque es retroalimentación complementaria: no cambia el puntaje ni rompe la inmutabilidad de una evaluación finalizada |
| S-43 | Tamaño y formatos de archivo son decisión propia, no confirmada por el cliente: documentos de planificaciones y evaluaciones hasta 10 MB (.docx/.pdf o .docx/.xlsx/.pdf); audio de la nota de voz hasta 25 MB (.webm/.ogg/.mp3/.wav/.m4a) |
| S-44 | Se agregaron endpoints no pedidos explícitamente (listar documentos y el audio de la nota de voz en una ruta aparte): necesarios para que el front-end los muestre y reproduzca |
| S-45 | El Sprint 1 sigue construido y probado con los cinco elementos de planificación de la entrevista (no los del formato oficial real, ver S-09): migrar `planificacion` es trabajo nuevo que queda para un sprint futuro, para no arriesgar una historia ya entregada y sus 146 pruebas |
| S-46 | HU-15, HU-16, HU-17 y HU-20 se reescribieron con el contenido real de "RetroPlani Mate" (`ChatBot (1).rar`), pero **siguen sin implementarse**: es planificación de un sprint futuro, no código de esta fase |
| S-47 | **Añadido el 22-sep:** enviar una planificación a revisión ahora también exige al menos un documento adjunto (la planificación formal en Word o PDF, HU-21): los cinco elementos del formulario quedan como resumen y el documento como el respaldo formal. Decisión propia, no confirmada por el cliente |
| S-48 | Las inscripciones cerradas antes de HU-31 no tenían fecha de cierre: se les dedujo del periodo (el semestre 1 cierra el 31 de julio y el 2 el 31 de diciembre). Desde el 10-oct el sistema guarda la fecha real del cierre |

Los supuestos S-03, S-04, S-13 y S-15 tienen que ver con la organización del sprint y están en `sprint-backlog.md`. Los S-17 a S-33 son decisiones técnicas del backend y están en `backend/PROGRESO.md`.
