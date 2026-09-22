# Sprint Backlog — Sprint 1

| | |
|---|---|
| **Duración** | 5 días: del **lunes 21 al viernes 25 de septiembre de 2026** (el viernes es la presentación y la entrega) |
| **Equipo** | 4 personas. Persona 1: código y base de datos. Personas 2, 3 y 4: diagramas, backlogs y tablero, presentación y pruebas (reparto completo en `tablero.csv`) |
| **Meta del sprint** | *Tener un backend en Spring Boot con base de datos MySQL donde el coordinador registre estudiantes y los inscriba en una práctica, el estudiante planifique una clase y la vea aprobada por su profesor guía y su profesor del curso, el guía evalúe la clase con la pauta digital y se pueda consultar el recorrido completo del estudiante.* |

## Cómo va el sprint (21 de septiembre)

Las 5 historias ya están **construidas y probadas** desde el domingo 20: 32 endpoints, 146 pruebas automáticas y una demostración completa ensayada por HTTP contra MySQL. Eso quedó antes de que empezara el sprint, así que la semana se dedica a lo que falta: tablero, diagramas, presentación, retrospectiva y confirmar supuestos. El detalle técnico está en `backend/PROGRESO.md` y el guion de la demo en `docs/GUIA_DEFENSA.md`.

**Ampliación del 22 de septiembre (Fase E):** se agregaron y probaron, sobre las mismas 5 historias, HU-11 y HU-12 (nota de voz con transcripción), HU-21 y HU-22 (documentos adjuntos en planificaciones y evaluaciones), HU-23 (buscar estudiante) y HU-24 (accesos rápidos del menú). Se amplió el criterio de aceptación de HU-03 para que también cubra la nota de voz (grabarla, escucharla y corregir su transcripción desde la pantalla de la evaluación). Ahora son 159 pruebas automáticas (`mvn clean package`: 146 + 13 nuevas) más las 23 pruebas negativas de `db/bd.ps1`, y la base de demostración creció a 20 estudiantes (S-39). Detalle en `backend/PROGRESO.md` y `RESUMEN.md` secciones 8, 9, 11, 12 y 14.

---

## 1. Historias del sprint

| Orden | ID | Historia | Prioridad | Puntos | Horas estimadas | Depende de |
|---|---|---|---|---|---|---|
| 1 | HU-01 | Iniciar sesión con roles | Alta | 5 | 6 | — |
| 2 | HU-02 | Registrar estudiantes e inscribirlos en una práctica | Alta | 5 | 6 | HU-01 |
| 3 | HU-03 | Evaluar una clase con la pauta digital (indicadores 1 a 7) | Alta | 5 | 8 | HU-01, HU-02 |
| 4 | HU-04 | Planificar una clase y que la aprueben en dos etapas | Alta | 8 | 10 | HU-01, HU-02 |
| 5 | HU-05 | Ver el historial del estudiante | Alta | 3 | 4 | HU-02, HU-03, HU-04 |
| | | **Total de las historias** | | **26** | **34** | |
| | | Trabajo de apoyo (base de datos, esqueleto del proyecto, Swagger, pruebas, datos de demo) | | — | 8 | |
| | | **Total del sprint** | | | **≈ 42** | |

Los criterios de aceptación de cada historia están en `product-backlog.md`.

### Cómo estimamos
- Usamos **puntos de historia** con la escala de Fibonacci (1, 2, 3, 5, 8). Como referencia tomamos **HU-05 (3 puntos)**, porque solo lee datos que ya existen y casi no tiene reglas.
- Comparamos el resto contra esa referencia. HU-01 y HU-02 valen 5 por sus validaciones, la seguridad y las varias operaciones. HU-03 vale 5 por el rango de 1 a 7 y el cálculo del puntaje. **HU-04 vale 8** porque tiene más reglas que ninguna: estados, tres actores, historial de revisiones y permisos por rol.
- Las horas son trabajo efectivo de una sola persona programando con ayuda de herramientas de IA. Es nuestro primer sprint y no tenemos velocidad anterior, así que la estimación se revisa en la retrospectiva con las horas reales.

## 2. Por qué elegimos estas 5 historias

1. **Cumplen la rúbrica.** Se piden al menos 5 HU funcionando con Spring Boot, y estas forman un recorrido completo de punta a punta, no cinco piezas sueltas.
2. **Van a lo que el cliente marcó como central.** En la entrevista destacó la planificación (HU-04), la pauta digital (HU-03) y el seguimiento del recorrido del estudiante (HU-02 y HU-05). Además son las que mejor se muestran en la demostración.
3. **Son la base de todo lo demás.** La nota de voz se asocia a una evaluación (HU-03), el chatbot usa el historial como contexto (HU-05) y los informes leen evaluaciones y planificaciones. Sin usuarios, roles e inscripciones (HU-01 y HU-02) nada más funciona.
4. **Bajan el riesgo antes de invertir en lo grande.** Con la pauta real de 20 indicadores mostramos que el modelo de datos sirve para el problema, antes de meternos con el chatbot.

### Qué dejamos fuera y por qué

| Historia | Motivo |
|---|---|
| HU-15 a HU-17, HU-20, HU-29, HU-30 (chatbot) | Falta definir el modelo de lenguaje; el estilo y los criterios de retroalimentación ya se conocen (RetroPlani Mate, `ChatBot (1).rar`) pero construir el chatbot es un riesgo mayor que 5 días (supuesto S-02) |
| HU-06 y HU-07 (más pautas) | Cargamos la pauta de observación con un script; así se demuestra la digitalización sin construir todavía un editor de pautas |
| HU-08 y HU-09 (informes y nota final) | Dependen de tener evaluaciones registradas y de que el cliente confirme las fórmulas de la nota |

## 3. Tareas

Cada tarea tiene responsable, fechas y estado en `tablero.csv`, que es lo que se sube a Trello. Resumen:

| Parte | Tareas | Estado |
|---|---|---|
| Base de datos | Crear la base y el usuario de la app; script con las 14 tablas y los datos de demo | Hecho |
| Backend (HU-01 a HU-05) | Proyecto Spring Boot; seguridad con roles; API de estudiantes e inscripción; pauta y evaluación; planificación con su flujo; historial; pruebas de cada historia; Swagger | Hecho |
| Demostración | Guion con datos ficticios (hecho); repetir la prueba desde cero en el computador de cada uno (pendiente) | En curso |
| Diagramas | Dibujar el As-Is, el To-Be y el MER a mano o en bpmn.io, con las imágenes guía de `Sprint1/diagramas/` | Por hacer |
| Metodología | Subir los backlogs al tablero; reunión de planificación; confirmar supuestos; retrospectiva | Por hacer |
| Presentación | Las cuatro partes, el plan B de la demo y un ensayo con preguntas | Por hacer |
| Entrega | Crear el repositorio Git; verificación final con base limpia | Por hacer |

## 4. Calendario

| Día | Qué hacemos |
|---|---|
| Domingo 20 | Backend y base de datos terminados y probados. Documentos de metodología y guía de defensa listos |
| **Lunes 21** | Reunión de planificación (30 min). Elegir la herramienta y armar el tablero. Crear el repositorio. Repartir los diagramas y la presentación |
| **Martes 22** | Dibujar el As-Is, el To-Be y el MER. Empezar la presentación. Cada uno prueba la demo en su computador |
| **Miércoles 23** | Diagramas terminados y exportados. Presentación armada. Llevar las dudas a la profesora |
| **Jueves 24** | Ensayo completo con preguntas. Reunión de retrospectiva (30 min). Última prueba de punta a punta |
| **Viernes 25** | Base limpia con `.\db\bd.ps1`, verificación final, presentación y entrega |

## 5. Definición de "Hecho"

Una historia está terminada cuando:

1. Cumple **todos** sus criterios de aceptación.
2. Funciona contra la base MySQL real, sin datos simulados.
3. La seguridad por rol responde bien: 401 sin sesión y 403 con el rol equivocado.
4. Tiene al menos una prueba automática del caso feliz y de un caso de error.
5. Aparece en Swagger y se puede mostrar en vivo con datos de ejemplo.
6. Su tarjeta está en la columna "Hecho" del tablero.

## 6. Riesgos

| Riesgo | Qué pasaría | Qué hacemos |
|---|---|---|
| El código lo hizo una sola persona | Los demás no pueden explicar la demo ni responder preguntas técnicas | Todos prueban la demo en su computador y cada uno prepara las respuestas de su parte (ver `docs/GUIA_DEFENSA.md`, sección 5) |
| Los supuestos siguen sin confirmar (sobre todo S-37, S-38 y S-09) | Habría que cambiar el flujo de planificación o el acceso a los chats | Llevar la lista a la profesora o al cliente esta semana. Ya sabemos qué se toca: la máquina de estados de HU-04 |
| Datos sucios en la base el día de la presentación | La demo no sale como el guion | Ejecutar `.\db\bd.ps1` justo antes de presentar. Toma menos de un minuto |
| Falla la demo en vivo | Nos quedamos sin mostrar las HU | Tener capturas o un video corto como plan B |
| No hay repositorio Git ni tablero todavía | No podemos entregar el código ni mostrar el seguimiento | Crearlos el lunes 21 y asignar quién lo hace |
| No sabemos la hora de la presentación | Mal reparto del tiempo | Preguntarle a la profesora |
| Datos personales reales en la demo | Problema de privacidad | La demo usa solo datos ficticios (S-15) |

## 7. Supuestos

Es la lista de todo el proyecto y los números se repiten en los otros documentos. **Hay que confirmarlos con el cliente o con la profesora.** Lo que viene "de un estudiante" nos orienta, pero no lo confirma el cliente.

| ID | Qué asumimos | Dónde pega |
|---|---|---|
| S-01 | Usamos Java 17, Spring Boot 3, Maven y MySQL 8 (instalada la 8.4). El Sprint 1 es solo **backend y base de datos** (API REST) y se muestra con Swagger o Postman | Alcance, presentación |
| S-02 | El chatbot y la nota de voz quedan para sprints posteriores | Product Backlog, To-Be |
| S-03 | El sprint va del lunes 21 al viernes 25 de septiembre y el viernes es la presentación (no sabemos la hora) | Calendario |
| S-04 | El tablero será en Trello o GitHub Projects (sin elegir) y el código se entrega en un repositorio Git (por definir) | Tablero |
| S-05 | El profesor del curso que autoriza la clase es el académico UBB de la asignatura. **Un estudiante contó** que se manda "al profe del ramo y al colaborador" | HU-04, BPMN, MER |
| S-06 | Inicio de sesión propio (no el de la UBB), con token de 8 horas y contraseñas cifradas | HU-01 |
| S-07 | La primera pauta que se digitaliza es la de observación de clase del colaborador (20 indicadores, escala 1 a 7) | HU-03 |
| S-08 | El Sprint 1 calcula puntaje total y promedio; pasarlo a nota oficial queda pendiente | HU-03 |
| S-09 | El formato de planificación son los cinco elementos que describió el cliente (objetivo, recursos, tiempos, evaluación del aprendizaje e interacción de los participantes con los recursos). Falta el archivo oficial | HU-04, MER |
| S-10 | Si el profesor del curso no autoriza, la planificación vuelve al estudiante y repite el flujo desde el guía. **Un estudiante dijo** "después de recibir las observaciones debes mejorar tu planificación", sin aclarar si vuelve primero al guía. Ver S-37 | HU-04, To-Be |
| S-11 | Cada usuario tiene un solo rol y cada evaluación pertenece a una inscripción. **Un estudiante confirmó** que en pareja cada alumno tiene su propia pauta | MER |
| S-12 | El tutor y las 8 pautas de Práctica Profesional entran en el Sprint 2 | Product Backlog, BPMN |
| S-13 | Los documentos del cliente se contradicen en la duración de las prácticas (Investigación en el Aula: 4 semanas en la entrevista, 3 en el protocolo y 2 en la ficha), así que no la modelamos. **Un estudiante dijo** que fueron 2 semanas, como la ficha | MER |
| S-14 | El semestre y las asignaturas aprobadas los ingresa el coordinador. **Un estudiante contó** que hoy se ven en la malla de la Intranet (no en Adecca); importarlos de ahí queda como mejora | HU-05, MER |
| S-15 | La demo usa datos ficticios | Presentación |
| S-16 | ~~Las dos prácticas iniciales son individuales~~ **Corregido el 20-sep según un estudiante:** Aproximación al Sistema Escolar es en **grupos de 4** y no entrega planificaciones; Ambientes de Aprendizaje es en **parejas** y sí las entrega; ninguna va a un colegio, así que no tienen profesor colaborador. Obligó a hacer la migración `db/migraciones/001` | HU-02, HU-04, MER |
| S-34 | Sin profesor guía (ciclo inicial) la planificación va **directo al profesor del curso**: al enviarla pasa a `EN_AUTORIZACION`. Es consecuencia de S-16 | HU-04 |
| S-35 | La planificación se entrega con al menos **3 días de anticipación** ("como mínimo unos 3 días antes", dijo un estudiante). Es configurable | HU-04 |
| S-36 | Se planifican todas las clases con docencia directa (dato de un estudiante): una planificación por clase | HU-04 |
| S-37 | **Sin resolver:** el cliente describió el flujo en secuencia (estudiante, guía, profesor del curso). Un estudiante dice que se manda al profesor del ramo y al colaborador "para que la aprueben o hagan sus observaciones", quizás a la vez. **Seguimos con la secuencia** hasta que alguien confirme | HU-04, To-Be |
| S-38 | El **jefe de carrera pidió ver los chats** de los estudiantes (HU-20). Asumimos que usa el rol coordinador. Un estudiante dijo que le molestaría que su guía lea todo su chat: el acceso del guía (HU-16) queda por decidir | HU-16, HU-20 |
| S-39 | Generación completa de 20 estudiantes de demostración: perfiles de avance y a qué guía o profesor del curso se asigna cada uno son datos inventados por el equipo, no del cliente | `db/seed.sql` |
| S-40 | La transcripción inicial de la nota de voz la genera el navegador (Web Speech API de Chrome), sin costo y sin instalar nada: nadie ha confirmado con el cliente qué herramienta de transcripción usar | HU-12 |
| S-41 | El documento formal de la evaluación se puede subir con la evaluación en BORRADOR o ya FINALIZADA: no se exige un estado particular | HU-22 |
| S-42 | La nota de voz se puede grabar con la evaluación en BORRADOR o ya FINALIZADA, porque es retroalimentación complementaria y no cambia el puntaje | HU-11 |
| S-43 | Tamaño y formatos de archivo son decisión propia: documentos hasta 10 MB (.docx/.pdf o .docx/.xlsx/.pdf); audio hasta 25 MB (.webm/.ogg/.mp3/.wav/.m4a) | HU-11, HU-21, HU-22 |
| S-44 | Se agregaron endpoints no pedidos explícitamente (listar documentos y el audio de la nota de voz en una ruta aparte), necesarios para el front-end | HU-11, HU-21, HU-22 |
| S-45 | El Sprint 1 sigue construido y probado con los cinco elementos de planificación de la entrevista, no con el formato oficial real del `.docx` (ver S-09): migrar `planificacion` queda para un sprint futuro | HU-04 |
| S-46 | HU-15, HU-16, HU-17 y HU-20 se reescribieron con el contenido real de "RetroPlani Mate" pero siguen sin implementarse: es planificación, no código de esta fase | HU-15, HU-16, HU-17, HU-20 |
| S-47 | Añadido el 22-sep: enviar una planificación a revisión ahora también exige al menos un documento adjunto (la planificación formal en Word o PDF); los campos del formulario quedan como resumen. Decisión propia, no confirmada por el cliente | HU-04, HU-21 |

Los supuestos S-17 a S-33 son decisiones técnicas que se tomaron al programar el backend y están en `backend/PROGRESO.md`. Las respuestas del estudiante de PEM (20-sep) están en `RESUMEN.md`, sección 13.
