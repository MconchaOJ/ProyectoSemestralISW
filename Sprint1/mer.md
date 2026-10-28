# Modelo Entidad-Relación (MER) — Sistema de Formación Práctica PEM-UBB

Este es el modelo de la base de datos MySQL del proyecto (`db/schema.sql`). Lo armamos a partir de la entrevista con el cliente y de las pautas reales de la carrera, sobre todo la *pauta de observación de clase del profesor colaborador*: 20 indicadores, escala de 1 a 7, en 3 dimensiones.

Cómo está ordenado este archivo:

1. La idea en simple.
2. **Vista simple en 3 partes**: para dibujar a mano y para la presentación.
3. **Modelo completo**: el que realmente tiene la base de datos (con tipos, claves y todo).
4. Entidades y relaciones explicadas con palabras.
5. Reglas que el diagrama no puede mostrar, y qué entra en el Sprint 1.
6. Supuestos y preguntas de defensa.

## 1. La idea en simple

El cliente quiere poder responder estas preguntas con el sistema:

| Pregunta del cliente | Qué tablas la responden |
|---|---|
| ¿Quién es el estudiante y en qué práctica está? | `USUARIO`, `ESTUDIANTE`, `ASIGNATURA_PRACTICA`, `INSCRIPCION` |
| ¿Quién lo guía y quién autoriza sus clases? | `INSCRIPCION` (guarda su profesor guía y su profesor del curso) |
| ¿Cómo lo evaluaron, indicador por indicador? | `PAUTA`, `DIMENSION_PAUTA`, `INDICADOR`, `EVALUACION`, `RESPUESTA_INDICADOR` |
| ¿Qué planificó y quién se lo aprobó? | `PLANIFICACION`, `REVISION_PLANIFICACION` |
| ¿Cuál es su recorrido por las 6 prácticas? | Sus `INSCRIPCION` ordenadas en el tiempo, con lo que cuelga de cada una |
| ¿Qué documentos adjuntó a su planificación o a su evaluación? (Fase E) | `DOCUMENTO_PLANIFICACION`, `DOCUMENTO_EVALUACION` |
| ¿Qué dijo el profesor en su nota de voz, y su transcripción? (Fase E) | `NOTA_VOZ` |
| (Más adelante) ¿Qué le respondió el chatbot? | `MENSAJE_CHATBOT` |

Tres decisiones que conviene recordar:

1. **La inscripción es el centro.** Una inscripción es "un estudiante cursando una asignatura en un periodo" (por ejemplo, Camila en Trabajo Colaborativo 2026-2). De ahí cuelgan las planificaciones, las evaluaciones y, más adelante, el chat. Por eso el historial de un estudiante es simplemente la lista de sus inscripciones ordenada por fecha.
2. **La pauta es un dato, no código.** Las dimensiones, los indicadores y la escala están en tablas. Si mañana la carrera usa otra pauta (escala de 2 a 7, Orientación, tutor), se agrega como datos nuevos sin tocar el programa.
3. **Nada importante se sobrescribe.** Cada decisión sobre una planificación queda como una fila nueva en `REVISION_PLANIFICACION`, y una evaluación finalizada ya no se puede cambiar. Así hay evidencia para la acreditación.

## 2. Vista simple en 4 partes

Estos cuatro dibujos muestran lo mismo que el modelo completo, pero con pocos datos por tabla y sin claves. Son los que conviene copiar a mano y explicar en la presentación. Se pueden ver pegándolos en <https://mermaid.live>.

**Por qué una Parte D y no meter documentos y nota de voz en B o C:** son entidades nuevas de la Fase E con sus propias relaciones (quién sube, a qué se adjunta); metidas dentro de B o C esos dibujos habrían quedado con demasiadas cajas para copiarlos a mano.

En Mermaid cada dato lleva una palabra de tipo (`string`, `int`...): ignórenla al dibujar. Cómo leer las líneas: `||` es "exactamente uno", `o|` es "cero o uno", `o{` es "cero o muchos" y `|{` es "uno o muchos".

### Parte A. Personas y prácticas

```mermaid
erDiagram
    USUARIO ||--o| ESTUDIANTE : "puede ser"
    ESTUDIANTE ||--o{ INSCRIPCION : "cursa"
    ASIGNATURA_PRACTICA ||--o{ INSCRIPCION : "se ofrece en"
    USUARIO |o--o{ INSCRIPCION : "es guia de"
    USUARIO ||--o{ INSCRIPCION : "es profesor del curso de"
    USUARIO |o--o{ INSCRIPCION : "es tutor de"

    USUARIO {
        string nombre
        string correo
        string rol
    }
    ESTUDIANTE {
        string rut
        int semestre_actual
    }
    ASIGNATURA_PRACTICA {
        string nombre
        string modalidad
        boolean requiere_planificacion
    }
    INSCRIPCION {
        string periodo
        string estado
        decimal nota_final
        date fecha_cierre
    }
```

Cómo se cuenta: "Un usuario puede ser estudiante. Un estudiante cursa muchas inscripciones a lo largo de sus 6 prácticas. Cada inscripción es de una asignatura y tiene su profesor del curso y, si la asignatura va a un colegio, su guía".

### Parte B. Evaluación con la pauta

```mermaid
erDiagram
    INSCRIPCION ||--o{ EVALUACION : "recibe"
    PAUTA ||--o{ EVALUACION : "se usa en"
    PAUTA ||--|{ DIMENSION : "se divide en"
    DIMENSION ||--|{ INDICADOR : "agrupa"
    EVALUACION ||--|{ RESPUESTA : "contiene"
    INDICADOR ||--o{ RESPUESTA : "se puntua en"

    PAUTA {
        string nombre
        int escala_min
        int escala_max
    }
    DIMENSION {
        string nombre
    }
    INDICADOR {
        int numero
        string descripcion
    }
    EVALUACION {
        date fecha_clase
        int puntaje_total
        decimal promedio
    }
    RESPUESTA {
        int puntaje
    }
```

Cómo se cuenta: "Una pauta tiene dimensiones y cada dimensión tiene indicadores (en la pauta real, 20). Cada vez que un guía evalúa una clase se crea una evaluación con una respuesta por indicador. El puntaje total y el promedio se calculan solos al finalizar".

### Parte C. Planificación y sus revisiones

```mermaid
erDiagram
    INSCRIPCION ||--o{ PLANIFICACION : "requiere"
    PLANIFICACION ||--o{ REVISION : "recibe"
    USUARIO ||--o{ REVISION : "escribe"
    PLANIFICACION |o--o{ EVALUACION : "se observa en"

    PLANIFICACION {
        string titulo
        date fecha_clase
        string estado
    }
    REVISION {
        string etapa
        string decision
        string comentario
        datetime fecha
    }
```

Cómo se cuenta: "Un estudiante escribe una planificación por cada clase. El guía y el profesor del curso la revisan: cada decisión queda guardada con quién, cuándo y qué comentó. Si la planificación se usó para una clase evaluada, la evaluación puede apuntar a ella".

### Parte D. Documentos adjuntos y nota de voz (Fase E, 22-sep)

```mermaid
erDiagram
    PLANIFICACION ||--o{ DOCUMENTO_PLAN : "recibe"
    EVALUACION ||--o{ DOCUMENTO_EVAL : "recibe"
    EVALUACION ||--o| NOTA_VOZ : "puede tener"
    USUARIO ||--o{ DOCUMENTO_PLAN : "sube"
    USUARIO ||--o{ DOCUMENTO_EVAL : "sube"

    DOCUMENTO_PLAN {
        string nombre_archivo
        string tipo_mime
        int tamano_bytes
    }
    DOCUMENTO_EVAL {
        string nombre_archivo
        string tipo_mime
        int tamano_bytes
    }
    NOTA_VOZ {
        int duracion_segundos
        string transcripcion
        boolean transcripcion_corregida
    }
```

Cómo se cuenta: "El estudiante puede adjuntar documentos (Word o PDF) a su planificación, y el profesor guía puede adjuntar el documento formal de la pauta a su evaluación. Además, el profesor guía puede grabar como máximo una nota de voz por evaluación, con su transcripción". Documento_plan es `documento_planificacion` y Documento_eval es `documento_evaluacion` en la base; se acortó el nombre solo para que el diagrama se lea cómodo.

### Un ejemplo con datos de la demo

Camila Rojas (estudiante 1) tiene una inscripción en Trabajo Colaborativo 2026-2 (la inscripción 3), con Marcos Bravo como guía y Ricardo Núñez como profesor del curso. De esa inscripción cuelgan tres planificaciones: una autorizada, una con observaciones y un borrador. Cada revisión de Marcos o de Ricardo es una fila en `REVISION_PLANIFICACION`. Si Marcos evalúa una clase suya, aparece una fila en `EVALUACION` con 20 filas en `RESPUESTA_INDICADOR`.

## 3. Modelo completo (el que tiene la base de datos)

Este es el diagrama con todos los campos, tipos y claves (`PK` clave primaria, `FK` clave foránea, `UK` único). Son las 22 tablas del script `db/schema.sql` (14 del Sprint 1, `documento_planificacion` y `documento_evaluacion` de la Fase E, `instrucciones_chatbot` de HU-17, `documento_base_conocimiento` de HU-29, `lectura_chat_jefe_carrera` de HU-20 y `respuesta_sus`, `respuesta_rubrica_experto` y `respuesta_rubrica_criterio` de HU-30).

```mermaid
erDiagram
    ROL ||--o{ USUARIO : "clasifica"
    ROL |o--o{ USUARIO : "segundo rol opcional"
    USUARIO ||--o| ESTUDIANTE : "se especializa en"
    ESTUDIANTE ||--o{ INSCRIPCION : "cursa"
    ASIGNATURA_PRACTICA ||--o{ INSCRIPCION : "se ofrece en"
    USUARIO |o--o{ INSCRIPCION : "es profesor guia de"
    USUARIO ||--o{ INSCRIPCION : "es profesor del curso de"
    USUARIO |o--o{ INSCRIPCION : "es tutor de"
    ASIGNATURA_PRACTICA ||--o{ PAUTA : "define"
    PAUTA ||--|{ DIMENSION_PAUTA : "se compone de"
    DIMENSION_PAUTA ||--|{ INDICADOR : "agrupa"
    INSCRIPCION ||--o{ EVALUACION : "recibe"
    PAUTA ||--o{ EVALUACION : "se aplica en"
    PAUTA |o--o| PAUTA : "version anterior (HU-06)"
    USUARIO ||--o{ EVALUACION : "realiza como evaluador"
    PLANIFICACION |o--o{ EVALUACION : "se observa en"
    EVALUACION ||--|{ RESPUESTA_INDICADOR : "contiene"
    INDICADOR ||--o{ RESPUESTA_INDICADOR : "se puntua en"
    INSCRIPCION ||--o{ PLANIFICACION : "requiere"
    PLANIFICACION ||--o{ REVISION_PLANIFICACION : "recibe"
    USUARIO ||--o{ REVISION_PLANIFICACION : "emite"
    EVALUACION ||--o| NOTA_VOZ : "puede tener"
    INSCRIPCION ||--o{ MENSAJE_CHATBOT : "registra"
    PLANIFICACION |o--o{ MENSAJE_CHATBOT : "se revisa en"
    USUARIO |o--o| INSTRUCCIONES_CHATBOT : "calibra"
    USUARIO ||--o{ DOCUMENTO_BASE_CONOCIMIENTO : "carga"
    INSCRIPCION ||--o{ LECTURA_CHAT_JEFE_CARRERA : "su chat es leido"
    USUARIO ||--o{ LECTURA_CHAT_JEFE_CARRERA : "lee como jefe"
    ESTUDIANTE ||--o| RESPUESTA_SUS : "valida con"
    ESTUDIANTE ||--o| RESPUESTA_RUBRICA_EXPERTO : "valida con"
    RESPUESTA_RUBRICA_EXPERTO ||--|{ RESPUESTA_RUBRICA_CRITERIO : "tiene 9"
    PLANIFICACION ||--o{ DOCUMENTO_PLANIFICACION : "recibe"
    USUARIO ||--o{ DOCUMENTO_PLANIFICACION : "sube"
    EVALUACION ||--o{ DOCUMENTO_EVALUACION : "recibe"
    USUARIO ||--o{ DOCUMENTO_EVALUACION : "sube"

    ROL {
        int id PK
        varchar nombre UK "ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR, JEFE_CARRERA"
        varchar descripcion
    }
    USUARIO {
        bigint id PK
        int rol_id FK
        int rol_secundario_id FK "opcional, migracion 004"
        varchar nombre
        varchar apellidos
        varchar email UK
        varchar password_hash "BCrypt"
        boolean activo
        datetime fecha_creacion
    }
    ESTUDIANTE {
        bigint id PK
        bigint usuario_id FK, UK
        varchar rut UK
        int semestre_actual
        int anio_ingreso
        boolean validador_chatbot "HU-30, migracion 009"
    }
    ASIGNATURA_PRACTICA {
        int id PK
        varchar codigo UK
        varchar nombre
        int orden "1 a 6"
        varchar ciclo "INICIAL, INTERMEDIO, AVANZADO"
        varchar modalidad "INDIVIDUAL, DUPLA, GRUPO"
        boolean requiere_centro_educativo
        boolean chatbot_habilitado "solo las dos finales"
        boolean requiere_planificacion "false solo en Aproximacion"
        boolean lleva_tutor "true solo en Practica Profesional (HU-07)"
    }
    INSCRIPCION {
        bigint id PK
        bigint estudiante_id FK
        int asignatura_id FK
        varchar periodo "ej. 2026-2"
        bigint profesor_guia_id FK "puede ser nulo"
        bigint profesor_curso_id FK
        bigint tutor_id FK "nulo = sin tutor (HU-07)"
        varchar estado "EN_CURSO, APROBADA, REPROBADA"
        decimal nota_final "nulo hasta el cierre"
        date fecha_cierre "nula mientras esta EN_CURSO (HU-31)"
    }
    PAUTA {
        int id PK
        int asignatura_id FK
        varchar nombre
        int version "HU-06, migracion 005"
        int version_anterior_id FK "nula en la version 1"
        varchar tipo_evaluador "COLABORADOR, TUTOR, ACADEMICO"
        varchar ambito "OBSERVACION_CLASE, ESPECIALIDAD, ORIENTACION"
        int numero_observacion "1 o 2; nulo en OBSERVACION_CLASE (HU-07)"
        int escala_min
        int escala_max
        boolean activa
        datetime fecha_creacion
    }
    DIMENSION_PAUTA {
        int id PK
        int pauta_id FK
        varchar nombre
        int orden
    }
    INDICADOR {
        int id PK
        int dimension_id FK
        int numero
        text descripcion
    }
    EVALUACION {
        bigint id PK
        bigint inscripcion_id FK
        int pauta_id FK
        bigint evaluador_id FK
        bigint planificacion_id FK "opcional"
        varchar establecimiento
        varchar curso
        date fecha_clase
        time hora_inicio
        time hora_termino
        text objetivo_clase
        text observaciones_generales
        varchar estado "BORRADOR, FINALIZADA"
        int puntaje_total "calculado al finalizar"
        decimal promedio "calculado al finalizar"
        datetime fecha_finalizacion
    }
    RESPUESTA_INDICADOR {
        bigint id PK
        bigint evaluacion_id FK
        int indicador_id FK
        int puntaje
        varchar comentario
    }
    PLANIFICACION {
        bigint id PK
        bigint inscripcion_id FK
        varchar titulo
        date fecha_clase
        text objetivo
        text recursos
        text tiempos
        int duracion_minutos
        text evaluacion_aprendizaje
        text interaccion_participantes
        varchar estado "BORRADOR, EN_REVISION_GUIA, CON_OBSERVACIONES, EN_AUTORIZACION, AUTORIZADA"
        datetime fecha_creacion
        datetime fecha_actualizacion
    }
    REVISION_PLANIFICACION {
        bigint id PK
        bigint planificacion_id FK
        bigint revisor_id FK
        varchar etapa "GUIA, CURSO"
        varchar decision "APROBADA, OBSERVADA"
        text comentario
        datetime fecha
    }
    NOTA_VOZ {
        bigint id PK
        bigint evaluacion_id FK, UK
        varchar archivo_ruta
        int duracion_segundos
        text transcripcion "nulo hasta transcribir"
        boolean transcripcion_corregida
        datetime fecha_grabacion
    }
    MENSAJE_CHATBOT {
        bigint id PK
        bigint inscripcion_id FK
        bigint planificacion_id FK "nulo = sin planificacion (migracion 007)"
        varchar autor "ESTUDIANTE, CHATBOT"
        text contenido
        datetime fecha
    }
    INSTRUCCIONES_CHATBOT {
        bigint id PK "siempre 1 (migracion 008)"
        text texto "reglas, tono y respuestas modelo"
        bigint editado_por_id FK "nulo = texto base"
        datetime fecha_edicion
        boolean validacion_abierta "HU-30, migracion 009"
    }
    DOCUMENTO_BASE_CONOCIMIENTO {
        bigint id PK
        varchar tipo UK "10 tipos de la propuesta (migracion 009)"
        varchar nombre_archivo
        varchar ruta_archivo "solo en disco, fuera de git"
        varchar tipo_mime
        bigint tamano_bytes "maximo 10 MB"
        mediumtext texto "extraido al subir"
        bigint subido_por_id FK
        datetime fecha_subida
    }
    LECTURA_CHAT_JEFE_CARRERA {
        bigint id PK
        bigint inscripcion_id FK
        bigint usuario_id FK "el jefe de carrera"
        datetime fecha "HU-20, migracion 010"
    }
    RESPUESTA_SUS {
        bigint id PK
        bigint validador_id FK,UK "un estudiante validador"
        int item1 "item1 a item10, de 1 a 5"
        int item10
        decimal puntaje "generado por la base, 0 a 100"
        datetime fecha "HU-30, migracion 011"
    }
    RESPUESTA_RUBRICA_EXPERTO {
        bigint id PK
        bigint validador_id FK,UK "un estudiante validador"
        text fortalezas "4 preguntas abiertas, opcionales"
        text debilidades
        text mejoras
        text recomendacion
        datetime fecha "HU-30, migracion 011"
    }
    RESPUESTA_RUBRICA_CRITERIO {
        bigint id PK
        bigint respuesta_id FK
        int numero "1 a 9, UK con respuesta_id"
        int puntaje "1 a 4"
        varchar comentario "opcional"
    }
    DOCUMENTO_PLANIFICACION {
        bigint id PK
        bigint planificacion_id FK
        varchar nombre_archivo
        varchar ruta_archivo "solo en disco, fuera de git"
        varchar tipo_mime
        bigint tamano_bytes "maximo 10 MB"
        bigint subido_por FK
        datetime fecha_subida
    }
    DOCUMENTO_EVALUACION {
        bigint id PK
        bigint evaluacion_id FK
        varchar nombre_archivo
        varchar ruta_archivo "solo en disco, fuera de git"
        varchar tipo_mime
        bigint tamano_bytes "maximo 10 MB"
        bigint subido_por FK
        datetime fecha_subida
    }
```

> Ejemplo de lectura de una línea: `ESTUDIANTE ||--o{ INSCRIPCION` se lee "un estudiante tiene cero o muchas inscripciones; cada inscripción es de exactamente un estudiante".

## 4. Entidades

Las 12 primeras se usan desde el Sprint 1. `DOCUMENTO_PLANIFICACION`, `DOCUMENTO_EVALUACION` y `NOTA_VOZ` se agregaron o activaron en la Fase E (22-sep): las tres ya tienen API y pruebas. `MENSAJE_CHATBOT` tiene API desde el Sprint 2 (26-oct, HU-15 y HU-16), con la columna nueva `planificacion_id` (migración 007): la planificación que el estudiante adjuntó a ese mensaje.

| Entidad | Qué representa (en simple) | Puntos clave | HU |
|---|---|---|---|
| `ROL` | Catálogo de los tipos de usuario: estudiante, profesor guía, profesor del curso, coordinador y jefe de carrera (este último desde el 10-oct, HU-31, solo lectura) | Es una tabla y no un texto libre para que nadie invente un rol Desde el 16-oct (HU-07, migración 006) también `TUTOR` (6 roles). | HU-01 |
| `USUARIO` | Cualquier persona que entra al sistema | Guarda la contraseña **cifrada** (`password_hash`), nunca en texto plano. Cada usuario tiene un rol (S-11) y, solo en cuentas especiales, un segundo rol opcional `rol_secundario_id` (migración 004, 2026-10-11, confirmado por el cliente: el jefe de carrera que también es guía) | HU-01 |
| `ESTUDIANTE` | Los datos propios de un usuario que es estudiante | Se separa de `USUARIO` porque solo los estudiantes tienen RUT, semestre y año de ingreso. `semestre_actual` es parte del contexto del chatbot (S-14) | HU-02 |
| `ASIGNATURA_PRACTICA` | Cada una de las 6 asignaturas de práctica | `chatbot_habilitado` es verdadero solo en las dos finales. `ciclo` y `modalidad` reflejan la estructura del cliente: Aproximación al Sistema Escolar es `GRUPO` (de 4) y Ambientes de Aprendizaje `DUPLA` (S-16, según un estudiante). `requiere_planificacion` es falso solo en Aproximación (solo trabajos en clase) `lleva_tutor` (HU-07) es verdadero solo en Práctica Profesional. | HU-02 |
| `INSCRIPCION` | Un estudiante cursando una asignatura en un periodo | Es el centro del modelo. Guarda quién es su guía (nulo en el ciclo inicial, que no va al colegio) y quién es su profesor del curso. Las asignaturas aprobadas se obtienen de aquí (`estado = APROBADA`). `fecha_cierre` guarda el día en que se cerró: la cartola de prácticas filtra por ella (migración 003, S-48) Desde HU-07 guarda también su tutor (`tutor_id`, solo Práctica Profesional; CHECK: distinto del guía y del profesor del curso). | HU-02, HU-05, HU-31 |
| `PAUTA` | Un instrumento de evaluación (por ejemplo, la pauta de observación de clase) | Define su escala (`escala_min`, `escala_max`) y quién la aplica (`tipo_evaluador`). Desde HU-06 (migración 005) la crea y edita el coordinador; una pauta ya usada no se modifica: se crea otra fila con `version` + 1 que apunta a la anterior (`version_anterior_id`), y cada evaluación conserva la versión con que se hizo HU-07: `numero_observacion` (1 o 2) en las pautas de Especialidad y Orientación, porque la primera y la segunda observación son instrumentos distintos (S-55); nulo en la de observación de clase. | HU-03, HU-06 |
| `DIMENSION_PAUTA` | Un grupo de indicadores de una pauta | En la pauta real son 3: estructura de la clase, tareas matemáticas, y ambiente de aula y recursos | HU-03 |
| `INDICADOR` | Cada criterio que se puntúa | La pauta real tiene 20 | HU-03 |
| `EVALUACION` | Una clase evaluada con una pauta | Guarda los datos de la cabecera de la pauta en papel (establecimiento, curso, fecha, horas, objetivo, observaciones generales). Al finalizar se calcula y guarda el puntaje. Puede enlazar la planificación de esa clase | HU-03 |
| `RESPUESTA_INDICADOR` | El puntaje que se puso a un indicador en una evaluación | Resuelve la relación muchos a muchos entre evaluaciones e indicadores | HU-03 |
| `PLANIFICACION` | El plan escrito de una clase | Tiene los elementos del formato de la carrera contados en la entrevista: objetivo, recursos, tiempos, evaluación del aprendizaje e interacción de los participantes con los recursos. El formato oficial real (`.docx`) ya se conoce y tiene más campos: ver S-09 y S-45 | HU-04 |
| `REVISION_PLANIFICACION` | Cada decisión del guía o del profesor del curso sobre una planificación | Conserva el historial completo: quién decidió, cuándo, en qué etapa y con qué comentario | HU-04 |
| `DOCUMENTO_PLANIFICACION` | Un archivo (Word o PDF) que el estudiante adjunta a su planificación | Máximo 10 MB (S-43). El archivo se guarda en disco, fuera de git; la tabla solo guarda la ruta relativa | HU-21 |
| `DOCUMENTO_EVALUACION` | Un archivo (Word, Excel o PDF) con la pauta y evaluación formal que sube el profesor guía | Máximo 10 MB (S-43). Se puede subir con la evaluación en BORRADOR o FINALIZADA (S-41) | HU-22 |
| `NOTA_VOZ` | Audio que graba el evaluador al terminar la clase, con su transcripción | Como máximo una por evaluación. La transcripción la genera el navegador (S-40) y la puede corregir el evaluador. Se graba en BORRADOR o FINALIZADA (S-42) | HU-11, HU-12 |
| `MENSAJE_CHATBOT` | Cada mensaje entre el estudiante y el chatbot, y la planificación que el estudiante adjuntó (si adjuntó una) | Se guarda por inscripción: lo leen el profesor guía de esa inscripción (HU-16, SUPUESTO S-60) y el jefe de carrera (HU-20, S-66) | HU-15, HU-16, HU-20 |
| `INSTRUCCIONES_CHATBOT` | Las instrucciones del chatbot que calibra el coordinador: reglas, tono, expresiones y respuestas modelo (una sola fila) | Sin fila, el chatbot usa el texto base de RetroPlani Mate que trae la aplicación; el contexto de cada estudiante no se guarda aquí (migración 008). Desde la migración 009 también guarda si la validación del chatbot está abierta | HU-17, HU-30 |
| `DOCUMENTO_BASE_CONOCIMIENTO` | Un documento de referencia del chatbot (rúbrica, bases curriculares, acciones DUA...), uno vigente por tipo, con el texto ya extraído | El archivo queda en disco; el chatbot recibe en cada mensaje solo los fragmentos pertinentes (migración 009) | HU-29 |
| `LECTURA_CHAT_JEFE_CARRERA` | Una lectura del chat de un estudiante hecha por el jefe de carrera | El estudiante ve la última; las del guía no se registran (migración 010) | HU-20 |
| `RESPUESTA_SUS` | La Escala SUS adaptada al chatbot de un validador: 10 ítems de 1 a 5 | El puntaje SUS (0 a 100) es una columna generada por la base (migración 011) | HU-30 |
| `RESPUESTA_RUBRICA_EXPERTO` | La rúbrica de juicio experto de un validador, con sus 4 preguntas abiertas | Instrumento de la propuesta RetroPlani Mate V3 (migración 011) | HU-30 |
| `RESPUESTA_RUBRICA_CRITERIO` | Un criterio (1 a 9) de esa rúbrica: puntaje de 1 a 4 y comentario | (migración 011) | HU-30 |

## 5. Relaciones

| Relación | Cardinalidad | Explicación |
|---|---|---|
| `ROL` — `USUARIO` | 1 a N | Un rol lo tienen muchos usuarios; cada usuario tiene un rol |
| `USUARIO` — `ESTUDIANTE` | 1 a 0..1 | Un estudiante *es* un usuario; no todo usuario es estudiante |
| `ESTUDIANTE` — `INSCRIPCION` | 1 a N | Un estudiante hace varias inscripciones a lo largo de sus 6 prácticas (y puede repetir una) |
| `ASIGNATURA_PRACTICA` — `INSCRIPCION` | 1 a N | Una asignatura tiene muchas inscripciones, una por estudiante y periodo |
| `USUARIO` — `INSCRIPCION` (profesor guía) | 0..1 a N | Un guía acompaña a muchos estudiantes. El guía es opcional porque el ciclo inicial no va al colegio |
| `USUARIO` — `INSCRIPCION` (profesor del curso) | 1 a N | Cada inscripción tiene un profesor del curso que autoriza las clases |
| `USUARIO` — `INSCRIPCION` (tutor) | 0..1 a N | HU-07 (migración 006, 2026-10-16): solo en Práctica Profesional (`lleva_tutor`), un tutor de la universidad observa y evalúa con sus propias pautas. Opcional y distinto del guía y del profesor del curso |
| `ASIGNATURA_PRACTICA` — `PAUTA` | 1 a N | Una asignatura puede tener varias pautas (por ejemplo, de colaborador y de tutor) |
| `PAUTA` — `DIMENSION_PAUTA` | 1 a 1..N | Una pauta se organiza en una o más dimensiones |
| `DIMENSION_PAUTA` — `INDICADOR` | 1 a 1..N | Una dimensión agrupa uno o más indicadores |
| `INSCRIPCION` — `EVALUACION` | 1 a N | Una inscripción recibe varias evaluaciones (varias clases observadas) |
| `PAUTA` — `EVALUACION` | 1 a N | Cada evaluación usa una pauta; una pauta se usa en muchas evaluaciones |
| `PAUTA` — `PAUTA` (versión anterior) | 0..1 a 0..1 | Una versión reemplaza a lo más a una anterior, y cada versión tiene a lo más una siguiente (HU-06) |
| `USUARIO` — `EVALUACION` | 1 a N | El evaluador (por ejemplo, el profesor guía) realiza muchas evaluaciones |
| `PLANIFICACION` — `EVALUACION` | 0..1 a N | La clase observada puede vincularse con la planificación que la autorizó. Es opcional para no bloquear evaluaciones sin planificación previa |
| `EVALUACION` — `RESPUESTA_INDICADOR` | 1 a 1..N | Una evaluación contiene los puntajes de sus indicadores (20 en la pauta real) |
| `INDICADOR` — `RESPUESTA_INDICADOR` | 1 a N | Un indicador se puntúa en muchas evaluaciones |
| `INSCRIPCION` — `PLANIFICACION` | 1 a N | Un estudiante planifica muchas clases dentro de una práctica |
| `PLANIFICACION` — `REVISION_PLANIFICACION` | 1 a N | Cada envío o devolución agrega una revisión |
| `USUARIO` — `REVISION_PLANIFICACION` | 1 a N | Quien revisa (guía o profesor del curso) emite muchas revisiones |
| `EVALUACION` — `NOTA_VOZ` | 1 a 0..1 | Una evaluación puede tener una nota de voz |
| `PLANIFICACION` — `DOCUMENTO_PLANIFICACION` | 1 a N | Una planificación puede tener varios documentos adjuntos |
| `USUARIO` — `DOCUMENTO_PLANIFICACION` | 1 a N | Quien sube el documento (el estudiante dueño) queda registrado |
| `EVALUACION` — `DOCUMENTO_EVALUACION` | 1 a N | Una evaluación puede tener varios documentos (pauta, evidencias) |
| `USUARIO` — `DOCUMENTO_EVALUACION` | 1 a N | Quien sube el documento (el profesor guía evaluador) queda registrado |
| `INSCRIPCION` — `MENSAJE_CHATBOT` | 1 a N | El chat de un estudiante en una práctica final tiene muchos mensajes |
| `PLANIFICACION` — `MENSAJE_CHATBOT` | 0..1 a N | Un mensaje del estudiante puede adjuntar una planificación de su misma inscripción (migración 007) |
| `USUARIO` — `INSTRUCCIONES_CHATBOT` | 0..1 a 0..1 | El coordinador que editó por última vez las instrucciones del chatbot (migración 008) |
| `USUARIO` — `DOCUMENTO_BASE_CONOCIMIENTO` | 1 a N | El coordinador que subió cada documento de la base de conocimiento (migración 009) |
| `INSCRIPCION` — `LECTURA_CHAT_JEFE_CARRERA` | 1 a N | Cada vez que el jefe de carrera abre el chat de esa inscripción queda una fila (HU-20, migración 010) |
| `USUARIO` — `LECTURA_CHAT_JEFE_CARRERA` | 1 a N | El jefe de carrera que leyó |
| `ESTUDIANTE` — `RESPUESTA_SUS` | 1 a 0..1 | Un validador responde la Escala SUS una sola vez (HU-30, migración 011) |
| `ESTUDIANTE` — `RESPUESTA_RUBRICA_EXPERTO` | 1 a 0..1 | Un validador responde la rúbrica de juicio experto una sola vez |
| `RESPUESTA_RUBRICA_EXPERTO` — `RESPUESTA_RUBRICA_CRITERIO` | 1 a 9 | Cada rúbrica tiene sus 9 criterios, con puntaje y comentario opcional |

## 6. Reglas que el diagrama no puede mostrar

Se implementan como restricciones en MySQL o como validaciones en Spring Boot:

| Regla | Dónde |
|---|---|
| `USUARIO.email`, `ESTUDIANTE.rut` y `ESTUDIANTE.usuario_id` son únicos | Restricción `UNIQUE` |
| Un estudiante no se inscribe dos veces en la misma asignatura y periodo: `UNIQUE (estudiante_id, asignatura_id, periodo)` | Restricción `UNIQUE` en `INSCRIPCION` |
| Una evaluación tiene un solo puntaje por indicador: `UNIQUE (evaluacion_id, indicador_id)` | Restricción `UNIQUE` en `RESPUESTA_INDICADOR` |
| El puntaje debe estar entre `escala_min` y `escala_max` de la pauta (1 a 7 en la pauta de observación), y el indicador debe pertenecer a la pauta de la evaluación | *Trigger* en MySQL (`trg_respuesta_indicador_bi` y `_bu`, porque un `CHECK` no puede consultar otra tabla) **y** validación en la aplicación (400) |
| El guía debe tener rol `PROFESOR_GUIA`, el profesor del curso `PROFESOR_CURSO` y el evaluador debe corresponder al `tipo_evaluador` de la pauta. El guía existe solo si la asignatura va a un colegio (`requiere_centro_educativo`) | Validación en la aplicación (y un control en `verificacion.sql`) |
| Solo se crean planificaciones en asignaturas con `requiere_planificacion`; se envían a revisión con al menos 3 días de anticipación a la clase (configurable); sin guía van directo a `EN_AUTORIZACION` | Validación en la aplicación (S-34, S-35) |
| Una evaluación `FINALIZADA` exige todos los indicadores puntuados y ya no se modifica | Exigir los 20 indicadores: aplicación. No modificar los puntajes: *trigger* en MySQL. Que traiga puntaje total, promedio y fecha de cierre calculados: `CHECK` |
| Una planificación `AUTORIZADA` no se edita; solo se pasa de un estado a otro por las transiciones permitidas | Validación en la aplicación |
| `chatbot_habilitado` es verdadero solo para Práctica Pedagógica y Práctica Profesional | Dato de la tabla, que la aplicación consulta. Además un `CHECK` impide activarlo fuera del ciclo `AVANZADO` |
| Formato de RUT y de periodo (`2026-2`), contraseña con formato BCrypt, estados y tipos válidos, nota final entre 1,0 y 7,0 (y vacía mientras la inscripción está `EN_CURSO`), guía distinto del profesor del curso, horas de la clase en orden, comentario obligatorio al devolver una planificación, planificación completa desde que se envía a revisión | Restricciones `CHECK` en MySQL |

### Estados de una planificación

```mermaid
stateDiagram-v2
    [*] --> BORRADOR
    BORRADOR --> EN_REVISION_GUIA : el estudiante la envía
    CON_OBSERVACIONES --> EN_REVISION_GUIA : el estudiante corrige y reenvía
    EN_REVISION_GUIA --> CON_OBSERVACIONES : el guía la devuelve
    EN_REVISION_GUIA --> EN_AUTORIZACION : el guía la aprueba
    EN_AUTORIZACION --> AUTORIZADA : el profesor del curso autoriza
    EN_AUTORIZACION --> CON_OBSERVACIONES : el profesor del curso la devuelve
    AUTORIZADA --> [*]
```

**Sin profesor guía** (ciclo inicial) el envío lleva directo de `BORRADOR` a `EN_AUTORIZACION` (S-34). Cada flecha (excepto el envío inicial) genera una fila en `REVISION_PLANIFICACION`. Una revisión con `etapa = CURSO` y `decision = APROBADA` es la autorización.

## 7. Qué se implementa en el Sprint 1

- El script SQL (`db/schema.sql`, con `db/seed.sql` y `db/bd.ps1` para cargarlo) crea **las 16 tablas** con sus claves y restricciones (16 `PRIMARY KEY`, 24 `FOREIGN KEY`, 13 `UNIQUE`, 34 `CHECK` y los índices de las consultas de HU-02 a HU-05), y carga datos semilla (roles, las 6 asignaturas, la pauta de observación con sus 3 dimensiones y 20 indicadores, y 20 estudiantes de demostración) más usuarios ficticios (S-39). Ya está creada y verificada en el MySQL local (base `isw_practicas`).
- La aplicación Spring Boot usa **16 entidades** (15 hasta el 26-oct): las 12 de HU-01 a HU-05, más `DOCUMENTO_PLANIFICACION`, `DOCUMENTO_EVALUACION` y `NOTA_VOZ`, activadas en la Fase E (22-sep). Desde el 26-oct (Sprint 2, HU-15) también `MENSAJE_CHATBOT`, y con HU-17 la tabla 17, `INSTRUCCIONES_CHATBOT` (entidad `CalibracionChatbot`, migración 008), y con HU-29 la 18, `DOCUMENTO_BASE_CONOCIMIENTO` (migración 009); en el cierre del chatbot, `LECTURA_CHAT_JEFE_CARRERA` (HU-20, migración 010) y `RESPUESTA_SUS`, `RESPUESTA_RUBRICA_EXPERTO` y `RESPUESTA_RUBRICA_CRITERIO` (HU-30, migración 011): las 22 tablas tienen entidad.
- Las cifras de puntaje (`puntaje_total`, `promedio`) se calculan al finalizar la evaluación y se guardan, para que el historial no tenga que recalcular.
- Los documentos adjuntos y los audios de nota de voz se guardan en disco (`backend/adjuntos/`), fuera del control de versiones; la base solo guarda su ruta relativa.

## 8. Supuestos

| ID | Supuesto |
|---|---|
| S-05 | El profesor del curso es el académico UBB; se modela como un usuario con rol propio, así que si fuera del colegio no cambia la estructura |
| S-09 | **Resuelto el 22-sep:** el formato oficial ya se conoce (`FORMATO DE PLANIFICACIÓN DIDÁCTICA DIARIA.docx`) y trae más campos que los cinco de la entrevista. El modelo sigue con los cinco elementos por ahora: migrar `planificacion` queda para un sprint futuro (S-45) |
| S-11 | ~~Un usuario tiene un solo rol~~ (desde el 2026-10-11, confirmado por el cliente: puede tener un segundo rol opcional en cuentas especiales, migración 004), y cada evaluación pertenece a una inscripción (un estudiante): **un estudiante reportó que en pareja cada alumno tiene su propia pauta**, que es justo lo que el modelo hace. Las duplas y grupos se modelarán en HU-10 con una tabla de agrupación |
| S-13 | No se modela la duración de las prácticas (semanas y horas), porque los documentos del cliente se contradicen (un estudiante reportó 2 semanas en Investigación en el Aula, como dice la ficha) |
| S-14 | El semestre y las asignaturas aprobadas se guardan aquí (los ingresa el coordinador); hoy la carrera los ve en la malla de la Intranet, y una importación desde ahí queda como mejora futura |
| S-16 | Aproximación al Sistema Escolar: `GRUPO` (de 4) y sin planificaciones; Ambientes de Aprendizaje: `DUPLA` con planificaciones; ninguna va a un colegio, así que no tienen guía. Fuente: un estudiante de PEM (2026-09-20), pendiente de confirmar. Cambió el modelo con la migración `db/migraciones/001` |
| S-39 a S-46 | Fase E (22-sep): generación de 20 estudiantes de demostración, transcripción con la Web Speech API del navegador, documentos y nota de voz sin exigir un estado particular de la evaluación, tamaños y formatos de archivo, endpoints extra para el front-end, y la planificación sigue sin migrar al formato oficial real. Detalle en `product-backlog.md`, sección 6, y `sprint-backlog.md`, sección 7 |

## 9. Preguntas probables en la defensa

**¿Por qué `ESTUDIANTE` está separado de `USUARIO`?**
Porque no todos los usuarios son estudiantes. Los datos propios del estudiante (RUT, semestre, año de ingreso) no tienen sentido para un profesor. Así se evitan columnas vacías y la especialización queda explícita.

**¿Por qué existe `INSCRIPCION` y no se relaciona directamente al estudiante con la asignatura?**
Porque la relación estudiante-asignatura es de muchos a muchos y además tiene datos propios: periodo, guía, profesor del curso, estado y nota. Toda la actividad (planificaciones, evaluaciones, chat) pertenece a esa inscripción concreta, no a la asignatura en general.

**¿Por qué las pautas están en tablas y no en código?**
El cliente tiene al menos 10 formatos de pauta con distintas escalas, dimensiones y evaluadores. Guardarlos como datos permite agregarlos sin modificar el programa.

**¿Por qué `REVISION_PLANIFICACION` es una tabla aparte y no un campo de comentario?**
Porque una planificación puede devolverse varias veces y el cliente necesita saber quién dijo qué y cuándo. Un campo único perdería el historial.

**¿Cómo se sabe qué asignaturas aprobó un estudiante para el chatbot?**
Se consultan sus inscripciones con `estado = APROBADA`. El semestre está en `ESTUDIANTE.semestre_actual`. Esa consulta es la que devuelve el historial (HU-05).

**¿Está normalizado?**
Sí, en tercera forma normal: cada atributo depende solo de la clave de su tabla. Los únicos datos calculados (`puntaje_total`, `promedio`) se guardan de forma deliberada al finalizar la evaluación, para congelar el resultado aunque después cambie la pauta.

**¿Qué falta en el modelo?**
Duplas (HU-10), versiones de pautas (HU-06), tutor y pautas de Práctica Profesional (HU-07), ponderaciones de la nota final (HU-09) y los informes/productos finales de la práctica (HU-18). El diseño permite agregarlos sin rehacer lo existente. Los documentos adjuntos y la nota de voz **ya no faltan**: se agregaron en la Fase E (22-sep).
