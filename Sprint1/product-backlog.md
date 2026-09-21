# Product Backlog — Sistema de Formación Práctica PEM-UBB

| | |
|---|---|
| **Proyecto** | Sistema web para evaluar y monitorear las 6 prácticas de Pedagogía en Educación Matemática (UBB) |
| **Cliente** | La carrera PEM: Dr. Rodrigo Panes Chavarría y el coordinador de prácticas, Dr. Víctor Parra González |
| **Versión** | 1.1, actualizada el 21 de septiembre de 2026 (Sprint 1) |
| **De dónde sale** | La entrevista con el cliente, los protocolos y las pautas de la carpeta `Documents/`, y las respuestas de un estudiante de PEM |
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
| HU-11 | Nota de voz del profesor evaluador | Retroalimentación | Alta | por estimar | 3 | Pendiente |
| HU-12 | Pasar la nota de voz a texto | Retroalimentación | Alta | por estimar | 3 | Pendiente |
| HU-13 | Exportar notas finales para Adecca | Integración | Media | por estimar | 3 | Pendiente |
| HU-14 | Panel de monitoreo del coordinador | Monitoreo | Media | por estimar | 3 | Pendiente |
| HU-19 | Avisos por correo de cambios de estado | Planificación | Media (subió desde Baja) | por estimar | 3 (tentativo) | Pendiente |
| HU-15 | Chatbot de retroalimentación según el contexto del estudiante | Chatbot | Alta | por estimar | 4 | Pendiente |
| HU-16 | El profesor guía ve el chat y la actividad del estudiante | Chatbot | Alta | por estimar | 4 | Pendiente |
| HU-17 | Calibrar el estilo del chatbot con el coordinador | Chatbot | Media | por estimar | 4 | Pendiente |
| HU-20 | El jefe de carrera consulta los chats | Chatbot | Media | por estimar | 4 | Pendiente |
| HU-18 | Registrar informes y productos de la práctica | Documentos | Baja | por estimar | 5 o más | Pendiente |

Los sprints 2 en adelante son **tentativos**: se replanifican al empezar cada uno con lo que aprendamos y lo que confirme el cliente. El chatbot y la nota de voz quedan para más adelante a propósito (supuesto S-02).

---

## 4. Historias de usuario

### Sprint 1

#### HU-01 · Iniciar sesión con roles
**Como** usuario del sistema (estudiante, profesor guía, profesor del curso o coordinador), **quiero** entrar con mi correo y mi contraseña y que el sistema solo me deje hacer lo que mi rol permite, **para** que la información de los estudiantes esté protegida y cada persona vea solo lo suyo.

**Prioridad:** Alta · **Viene de:** el requerimiento 6 (los guías acceden a lo que hace el estudiante) y la necesidad de separar a los actores.

**Criterios de aceptación**
1. Un usuario activo con sus credenciales correctas recibe un token que lo identifica a él y a su rol.
2. Con credenciales incorrectas recibe un error 401 que no dice si falló el correo o la contraseña.
3. Las contraseñas se guardan cifradas (BCrypt), nunca en texto plano ni en las respuestas.
4. Existen cuatro roles: `ESTUDIANTE`, `PROFESOR_GUIA`, `PROFESOR_CURSO` y `COORDINADOR`.
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

**Criterios de aceptación**
1. El estudiante con una inscripción `EN_CURSO` crea una planificación con el formato de la carrera: título, fecha de la clase, **objetivo**, **recursos**, **tiempos**, **evaluación del aprendizaje** y **cómo interactúan los participantes con los recursos**. Queda en `BORRADOR`. (Supuesto S-09: usamos estos cinco elementos porque nos contaron el formato en la entrevista, pero el archivo oficial no nos lo han entregado.)
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

#### HU-11 · Nota de voz del profesor evaluador
**Como** profesor evaluador, **quiero** grabar una nota de voz al terminar la clase y dejarla ligada a la evaluación, **para** usarla después en la retroalimentación con el estudiante.
- **Prioridad:** Alta · **Sprint:** 3 (supuesto S-02)
- **Criterios:** (1) el audio queda asociado a una evaluación; (2) solo lo escuchan el evaluador, el estudiante evaluado, su guía y el coordinador; (3) se define el tamaño y el formato máximos del archivo.

#### HU-12 · Pasar la nota de voz a texto
**Como** profesor evaluador, **quiero** convertir la nota de voz en texto, **para** revisarla, corregirla y entregársela al estudiante por escrito.
- **Prioridad:** Alta · **Sprint:** 3 · **Depende de** HU-11 y de elegir la herramienta de transcripción (todavía sin definir).
- **Criterios:** (1) la transcripción se guarda junto al audio; (2) el evaluador puede corregirla; (3) el sistema indica si el texto es automático o ya corregido.

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
**Como** estudiante de Práctica Pedagógica o Profesional, **quiero** conversar con un chatbot que me retroalimente según mi semestre y mis asignaturas aprobadas, **para** mejorar mis planificaciones y mi desempeño.
- **Prioridad:** Alta · **Sprint:** 4 (supuesto S-02)
- **Criterios:** (1) solo está disponible en las dos prácticas finales; (2) usa como contexto el semestre y las asignaturas aprobadas (los entrega HU-05); (3) responde en español y dice cuando no sabe; (4) guarda cada mensaje del chat.
- **Uso esperado (reportado por un estudiante):** preguntarle formas de enseñar un contenido y cómo mejorar una planificación.

#### HU-16 · El profesor guía ve el chat y la actividad del estudiante
**Como** profesor guía, **quiero** ver las planificaciones, evaluaciones y conversaciones de mis estudiantes, **para** ayudarlos a mejorar y acompañarlos.
- **Prioridad:** Alta · **Sprint:** 4 · **Depende de** HU-15. La parte de planificaciones y evaluaciones ya queda cubierta en el Sprint 1 (HU-04 y HU-05).
- **Por decidir con el cliente (S-38):** el requerimiento 6 dice que el guía accede a todo, pero un estudiante contó que le molestaría que su guía lea todo lo que conversa con el chatbot. Opciones: que el guía vea el chat completo (con aviso al estudiante), solo un resumen, o solo lo que el estudiante decida compartir.
- **Criterios:** (1) el guía ve el chat de sus estudiantes asignados y de nadie más; (2) el estudiante sabe que su guía puede verlo.

#### HU-17 · Calibrar el estilo de retroalimentación del chatbot
**Como** coordinador, **quiero** definir los criterios y el estilo con que retroalimenta el chatbot, **para** que se parezca a la retroalimentación que doy hoy con mi chatbot de ChatGPT.
- **Prioridad:** Media · **Sprint:** 4 · **Depende de** que el coordinador entregue sus criterios.

#### HU-20 · El jefe de carrera consulta los chats
**Como** jefe de carrera, **quiero** consultar las conversaciones de los estudiantes con el chatbot, **para** llevar un registro y saber cómo ha aportado el chatbot.
- **Prioridad:** Media · **Sprint:** 4 · **Depende de** HU-15 · **Viene de:** una solicitud del jefe de carrera (20-sep).
- **Criterios (borrador):** (1) puede listar las conversaciones por estudiante, asignatura y fecha (los mensajes ya se guardan por inscripción); (2) solo lectura; (3) el estudiante ve un aviso de que el jefe de carrera puede leer sus conversaciones; (4) cada acceso queda registrado.
- **Supuesto S-38:** mientras no se aclare si el jefe de carrera es otra persona, usa el rol `COORDINADOR` (si lo fuera, se agrega un rol nuevo).
- **Ojo con la privacidad:** es la misma tensión de HU-16. Hay que definir con el cliente qué se muestra (chat completo, resumen o datos agregados).

#### HU-18 · Registrar informes y productos de la práctica
**Como** estudiante, **quiero** subir mi informe final, mi diario de reflexión y mi proyecto de intervención, **para** que queden en mi recorrido.
- **Prioridad:** Baja · **Sprint:** 5 o más

---

## 5. De los requerimientos del cliente a las historias

| # | Lo que pidió el cliente | Historias que lo cubren |
|---|---|---|
| 1 | Un solo sistema para evaluar y monitorear las 6 prácticas, con el recorrido completo | HU-02, HU-05, HU-08, HU-14 |
| 2 | Digitalizar la pauta de evaluación | HU-03, HU-06, HU-07, HU-09 |
| 3 | Nota de voz con opción de pasarla a texto | HU-11, HU-12 |
| 4 | Flujo de planificación (estudiante, guía y profesor del curso; sin guía en el ciclo inicial) | HU-04, HU-19 |
| 5 | Chatbot según el semestre y los ramos aprobados, solo en las prácticas finales | HU-15, HU-17, HU-20 (el contexto lo da HU-05) |
| 6 | Que los profesores guía accedan a todo lo del estudiante | HU-05, HU-16 (la privacidad del chat está por decidir, S-38) |
| — | Roles y protección de datos (necesidad de todo el sistema) | HU-01 |
| — | Terminar con la doble digitación en Adecca | HU-03, HU-13 |

## 6. Supuestos que afectan a este backlog

Hay que confirmarlos con el cliente o con la profesora. Los números son los mismos en todos los documentos.

| ID | Qué asumimos |
|---|---|
| S-01 | El Sprint 1 entrega solo **backend y base de datos** (API con Spring Boot y MySQL), sin pantallas. La demostración se hace con Swagger |
| S-02 | El **chatbot** y la **nota de voz** quedan para sprints posteriores |
| S-05 | El **profesor del curso** que autoriza la clase es el académico UBB de la asignatura. Un estudiante contó que se manda "al profe del ramo y al colaborador" |
| S-06 | La sesión dura 8 horas y usamos nuestro propio inicio de sesión (no el de la UBB) |
| S-07 | La primera pauta que se digitaliza es la de observación de clase del colaborador (20 indicadores, escala 1 a 7) |
| S-08 | En el Sprint 1 se calcula puntaje total y promedio; la conversión a nota oficial se define después |
| S-09 | El formato de planificación son los cinco elementos que describió el cliente, hasta que nos entreguen el archivo oficial |
| S-10 | Si el profesor del curso no autoriza, la planificación vuelve al estudiante y el flujo empieza otra vez desde el guía. Un estudiante dijo "debes mejorar tu planificación", sin aclarar si vuelve primero al guía |
| S-11 | Cada usuario tiene un solo rol y cada evaluación pertenece a una inscripción. Un estudiante confirmó que en pareja cada alumno tiene su propia pauta |
| S-12 | El tutor y las 8 pautas de Práctica Profesional entran en el Sprint 2 |
| S-14 | El semestre y las asignaturas aprobadas los ingresa el coordinador. Un estudiante contó que hoy se ven en la malla de la Intranet; importarlos de ahí queda como mejora futura |
| S-16 | Aproximación es en grupos de 4 y sin planificaciones; Ambientes es en parejas y con planificaciones; ninguna va a un colegio, así que no tienen guía (corregido el 20-sep según un estudiante) |
| S-34 a S-38 | Sin guía la planificación va directo al profesor del curso; se entrega con 3 días de anticipación; una planificación por clase; el flujo en secuencia (guía y luego curso) sigue en discusión; el jefe de carrera usa el rol coordinador. Detalle en `sprint-backlog.md`, sección 7 |

Los supuestos S-03, S-04, S-13 y S-15 tienen que ver con la organización del sprint y están en `sprint-backlog.md`. Los S-17 a S-33 son decisiones técnicas del backend y están en `backend/PROGRESO.md`.
