# Resumen del proyecto semestral de ISW (Ingeniería de Software 2026)

> Documento para traspasar contexto a otro asistente que no vio la sesión.
> Fecha de la sesión: 2026-09-20. Idioma de trabajo con el usuario: **español**.
> Estado (actualizado 2026-09-20): **Fases A, B y C del Sprint 1 terminadas.** Documentos de metodología en `Sprint1/`; base de datos MySQL en `db/` (con la migración 001); **backend Spring Boot en `backend/` con las 5 HU, 32 endpoints y 146 pruebas automáticas** (sección 14) y guía de defensa en `docs/GUIA_DEFENSA.md`. Ese mismo día se ajustó el modelo con las respuestas de un estudiante de PEM (sección 13). **Pendiente:** presentación, retrospectiva, tablero, y confirmar los supuestos con el cliente o la profesora.

---

## 1. Contexto en una frase

El proyecto es un sistema web para la carrera **Pedagogía en Educación Matemática (PEM) de la Universidad del Bío-Bío (UBB)**. Debe centralizar, evaluar y monitorear las 6 asignaturas de práctica de cada estudiante y ofrecer un **chatbot de retroalimentación**. El cliente son la carrera y su coordinador de prácticas.

**Cliente (según la entrevista):** Dr. Rodrigo Panes Chavarría y el coordinador de prácticas Dr. Víctor Parra González.

---

## 2. Estructura de la carpeta del proyecto

```
ProyectoSemestralISW/
├── RESUMEN.md                          <- este archivo
├── Sprint1/                            <- entregables de la Fase A (ver sección 11)
├── db/                                 <- scripts de la base de datos MySQL, Fase B (ver sección 12); migraciones/ tiene los cambios de esquema
├── CLAUDE.md                           <- instrucciones cortas para el asistente (léelo primero)
├── .gitignore  y  .env.example         <- exclusión de secretos y plantilla de variables de entorno
├── backend/                            <- proyecto Spring Boot (Fase C, terminada); leer backend/PROGRESO.md y backend/README.md
├── docs/GUIA_DEFENSA.md                <- arquitectura, guion de demo por historia y preguntas de defensa
├── Pautas/
│   └── Pauta1/ISW - Rúbrica Sprint 1 2026 (1).pdf
└── Documents/
    ├── PROCESO DE FORMACIÓN PRÁCTICA_Descripción.docx
    ├── Prácticas Iniciales.rar
    ├── Prácticas Intermedias.rar
    ├── Prácticas Avanzadas.rar
    ├── 5_Práctica Pedagógica.rar
    ├── 6_Práctica Profesional.rar
    ├── Entrevista/Ingenieriasofware.txt
    └── SugerenciaChatBot/SugerenciaChatBot.ipynb
```

Los `.rar` se extrajeron en un directorio temporal de la sesión (no en el proyecto). Para volver a extraerlos: `tar -xf archivo.rar -C destino` (bsdtar de Windows 11 lee RAR). Hay `.rar` y `.zip` anidados. Los `.docx` son ZIP y el texto está en `word/document.xml`. El único `.doc` antiguo (`Indicaciones Presentación TANEP 2025.doc`) se convirtió con Word COM.

---

## 3. Documentos leídos y qué contienen

### 3.1 Rúbrica del Sprint 1 (`Pautas/Pauta1/*.pdf`)
Lista lo que se evaluará en el Sprint 1 (detalle en la sección 5).

### 3.2 Entrevista con el cliente (`Documents/Entrevista/Ingenieriasofware.txt`)
Apuntes del equipo de lo que dijo el cliente (detalle en la sección 4).

### 3.3 `PROCESO DE FORMACIÓN PRÁCTICA_Descripción.docx`
Resumen del proceso de práctica en tres ciclos:

| Ciclo | Asignaturas | Cómo funciona |
|---|---|---|
| Inicial | Aproximación al sistema escolar (IV sem.) y Ambientes de aprendizaje (V sem.) | Simulaciones en la universidad, sin visitas a colegios. Intervienen solo el estudiante y el profesor de la asignatura. *Según un estudiante (2026-09-20, pendiente de confirmar): Aproximación es en grupos de 4 y no entrega planificaciones; Ambientes es en parejas y sí las entrega; ninguna tiene profesor colaborador.* |
| Intermedio | Trabajo Colaborativo en el Aula (VI) e Investigación en el Aula (VII) | Primer contacto real con el aula, en **duplas**. Evalúa solo el profesor colaborador del colegio, con una pauta en papel que el profesor de la asignatura transcribe. |
| Avanzado | Práctica Pedagógica (6 semanas) y Práctica Profesional (13 semanas) | Trabajo **individual**. El estudiante asume como profesor de Matemática y profesor jefe (Orientación). Incorpora diversidad e inclusión (DUA, NEE). |

Actores: estudiante, profesor de la asignatura (académico UBB), profesor colaborador (del colegio, también llamado "guía"), tutor/supervisor contratado por la universidad (solo en Práctica Profesional) y coordinador de prácticas.

Evaluaciones: Práctica Pedagógica = 4 pautas del colaborador (2 Especialidad + 2 Orientación). Práctica Profesional = 8 pautas por estudiante (4 del colaborador + 4 del tutor). Al final, informe final de práctica y proyecto de intervención.

### 3.4 `Prácticas Iniciales.rar` y `Prácticas Intermedias.rar` (protocolo y ciclo intermedio)
- **Protocolo de Prácticas Iniciales e Intermedias PEM** (vigente desde 2025-2): objetivos y resultados de aprendizaje (RA) de las 4 asignaturas, roles (académico, profesor colaborador, coordinador de práctica), actividades en centros y monitoreo. El académico observa **mínimo 4 estudiantes por semestre** en las intermedias. Hay una reunión semestral con los colaboradores.
- **Trabajo Colaborativo 2026-2** (profesor Miguel Friz Carrillo): ficha con 3 semanas y 6 horas/semana = 18 horas. Actividades: entrevista a un docente, reunión del Departamento de Matemáticas o del Equipo PIE, y 3 clases. Informe 3 (160 puntos, 25%) con planificación (55), diario de reflexión (55) y autoevaluación (50). Pautas de observación de clase (colaborador y académico) de **20 indicadores en escala 1 a 7**.
- **Investigación en el Aula**: ficha con 2 semanas y 12 a 16 horas. Semana de acompañamiento y semana de docencia directa (2 a 3 clases), con instrumentos de recogida de datos. Informe reflexivo Eval 2 (60 puntos, 35%). Mismas pautas de observación de 20 indicadores.

### 3.5 `Prácticas Avanzadas.rar` y `5_Práctica Pedagógica.rar`
- **Orientaciones de la Práctica Pedagógica 2026-2**: 6 semanas (1 de inducción y 5 de docencia directa), 10 horas semanales (6 Matemática, 1 Orientación, 3 permanencia). Asistencia 100%, con recuperación de faltas justificadas. Trae contactos de la Unidad de Prácticas.
- **Estructura evaluativa**: Especialidad UBB 27% (Producto 1 Expectativas 10% y Producto 2 Contexto 17%), Colaborador Especialidad 18% (pautas 5% y 13%), Colaborador Orientación 8% (pautas 3% y 5%), Integración curricular 47% (TANEP 20% e Informe Final/Producto 3 27%).
- **Pauta Producto 3**: informe (en dupla, no PDF, con declaración de uso de IA) con dos partes: docencia directa (unidades, planificaciones, guías, evaluaciones, anexos en carpeta Drive con formato estricto) y reflexión pedagógica.
- **TANEP** (Taller de Análisis de Experiencias Pedagógicas): formato de análisis (planificación, ejecución y evaluación), rúbrica de 44 puntos e indicaciones de presentación oral (15 min + 5 de preguntas).

### 3.6 `6_Práctica Profesional.rar` (y copia dentro de Prácticas Avanzadas)
- **Orientaciones de la Práctica Profesional 2026-2 (Plan Renovado)**: 13 semanas de docencia directa, 22 horas semanales (14 Matemática en 2 cursos, 1 Orientación, 7 permanencia). Incluye un **Proyecto de Intervención Educativo**. Tutor y colaborador hacen 2 observaciones en Especialidad y 2 en Orientación cada uno.
- **8 pautas de evaluación 2025-2** (en `.rar` anidado): Especialidad y Orientación, primera y segunda observación, para Profesor Colaborador y para Tutor. Escala 2 a 7 (sin decimales), agrupadas por dimensiones (A-D o A-E). Puntajes totales: 133 (1ª Especialidad), 126 (1ª Orientación), 161 (2ª de ambas). Deben firmarse y timbrarse.

### 3.7 `SugerenciaChatBot.ipynb` (subido por la profesora como referencia)
Chatbot RAG en Google Colab:
- LangChain (`langchain==0.2.14`) con FAISS.
- Embeddings `sentence-transformers/all-MiniLM-L6-v2`.
- LLM `gemini-2.5-flash` (temperatura 0.3).
- Lee PDFs desde Google Drive (`/content/drive/MyDrive/taller_chile_wic/pdfs`).
- Fragmentos de 1000 caracteres con solape de 200, recuperación de los 5 mejores (k=5).
- Memoria de conversación y reformulación de la pregunta según el historial.
- Responde en español, máximo 3 oraciones, y dice que no sabe si el contexto no lo cubre.

Es **solo un ejemplo de arquitectura**: sus PDFs son de ética y género, no de prácticas. La clave de API se lee de los secretos de Colab (no está en el archivo).

---

## 4. Qué pide el cliente (entrevista)

**Problema:** a la carrera le falta algo para la acreditación máxima. El **criterio 3** es clave: *"La carrera diseña, implementa y monitorea un sistema para la formación práctica que permite a los estudiantes desarrollar las competencias del perfil de egreso y demostrar un desarrollo creciente de su efectividad en la docencia en las aulas escolares."*

Puntos a tratar: dispersión de la información, pautas manuales, múltiples profesores intervinientes, informes por asignatura y por estudiante (con síntesis), planificación y evaluación. Hoy las pautas se escriben a mano y luego se suben a una plataforma de la universidad (Adecca), donde se calcula el promedio final.

**Requerimientos:**
1. Un solo sistema de evaluación y monitoreo de las **6 prácticas** de cada estudiante, con su recorrido completo del inicio al final (retroalimentación de toda su vida de practicante, no solo la nota).
2. **Digitalizar la pauta de evaluación.**
3. **Nota de voz** que graba el profesor evaluador al terminar la clase, con opción de pasarla **a texto**. Sirve para la retroalimentación posterior con el estudiante.
4. **Flujo de planificación:** toda clase requiere una planificación escrita con el formato de la carrera (objetivo, recursos, tiempos, evaluación del aprendizaje y cómo interactúan los participantes con los recursos). El estudiante la presenta al **profesor guía**, quien retroalimenta. Con la mejora se la presenta al **profesor del curso**, quien autoriza la clase.
5. **Un chatbot** que retroalimente según el contexto del estudiante (**semestre y ramos aprobados**). La retroalimentación aplica **solo a las dos prácticas finales** (Pedagógica y Profesional). Víctor Parra ya usa un chatbot de ChatGPT que retroalimenta de forma parecida a él.
6. Que los **profesores guía tengan acceso a todo** lo que hace el estudiante (planificaciones, chat, etc.).

**Novedad (2026-09-20):** el **jefe de carrera pidió poder ver los chats de los alumnos** para llevar registro y saber cómo ha aportado el chatbot (HU-20). Un estudiante dijo que le incomodaría que su guía lea todo su chat (privacidad). Hay que definir con el cliente qué ve cada rol (S-38).

---

## 5. Entregables del Sprint 1 (rúbrica) y fechas

**Entregables evaluables:**
- **Metodología:**
  - Diagrama BPMN del proceso actual del cliente (As-Is) y del proceso mejorado propuesto (To-Be), ambos en BPMN.
  - Product Backlog con las historias de usuario (HU) del proyecto.
  - Sprint Backlog con las HU del sprint.
  - Tablero del sprint con tareas y miembros asignados.
  - Retrospectiva en formato "Qué salió bien / Qué mejorar / Acciones" (puede ir dentro del PPT del Sprint 1).
- **Implementación:** presentar las HU y mostrar **al menos 5 HU implementadas y funcionales**, con backend en **Spring Boot**.
- **Modelo y base de datos:** Modelo Entidad-Relación (MER) bien construido y adecuado al problema, y base de datos implementada en **MySQL**.
- **Presentación:** explicar la metodología y el producto, y responder preguntas.

**Fechas:**
- **Fecha del Sprint 1: viernes 25 de septiembre de 2026** (informada por el usuario; equipo de 4 personas). Hora de la presentación: no informada.
- Fechas de las prácticas (contexto del cliente, semestre 2026-2), tomadas de los documentos:
  - Práctica Pedagógica: inducción 21 al 25 de septiembre. 1ª evaluación 5 al 9 de octubre. 2ª evaluación 26 al 30 de octubre. Recuperación 2 al 6 de noviembre. Pautas de colaboradores hasta el 30 de octubre.
  - Práctica Profesional: docencia directa del 17 de agosto al 20 de noviembre. 1ª evaluación 24 de agosto al 25 de septiembre. 2ª evaluación 28 de septiembre al 30 de octubre. Proyecto de intervención, planificación hasta el 2 de octubre y ejecución del 5 de octubre al 20 de noviembre. Recuperación 23 al 27 de noviembre.
  - Trabajo Colaborativo: presentación en el centro del 12 de agosto al 10 de septiembre. Actividades del 21 de septiembre al 9 de octubre.
- Fechas de documentos de 2025 (referenciales, no de este semestre): entrega del Producto 3 el 5 de diciembre 17:00. TANEP presentada el 12 de diciembre y subida el 16 de diciembre.

---

## 6. Tecnologías

**Exigidas por la rúbrica:** BPMN (diagramas), **Spring Boot** (backend), **MySQL** (base de datos), Product/Sprint Backlog y un tablero de tareas (herramienta no especificada).

**Sugeridas como referencia (no decididas):** para el chatbot, el notebook de la profesora usa LangChain, FAISS, embeddings de Hugging Face y Gemini. Es un punto de partida de arquitectura RAG y no una obligación.

**Definido en el Sprint 1:** Java 17, Spring Boot 3.5, Maven, Spring Data JPA, Spring Security con JWT (8 horas) y BCrypt, springdoc/Swagger, MySQL 8, 4 roles (ver sección 14).

**Sin definir:** front-end, herramienta de transcripción de voz a texto, proveedor del LLM del chatbot y herramienta del tablero.

---

## 7. Decisiones tomadas

Acuerdos de trabajo:
- Responder siempre en español y explicar lo que se hace, porque el usuario tiene que defenderlo.
- No hacer rondas largas de preguntas (poco tiempo): asumir lo razonable y marcarlo como **SUPUESTO**.
- Los `.rar` se extraen en el directorio temporal de la sesión, no dentro del proyecto.
- El usuario se encarga del código y la base de datos; los otros 3 integrantes usan los documentos de `Sprint1/` para BPMN, backlogs, tablero y presentación.

Decisiones tomadas en la Fase A (todas como supuestos numerados S-01 a S-16, listados en `Sprint1/sprint-backlog.md`, sección 7; **pendientes de confirmar con el cliente o la profesora**):
- Stack: Java 17, Spring Boot 3.x, Maven, MySQL 8. El Sprint 1 es solo backend y BD (API REST, demo con Swagger o Postman), sin interfaz.
- Chatbot y nota de voz quedan para sprints posteriores (HU-15 y HU-11).
- 4 roles: ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR. El **profesor del curso** (autoriza la clase) se asume académico UBB (S-05, el más importante de confirmar).
- Primera pauta digitalizada: la de observación de clase del profesor colaborador (20 indicadores, 3 dimensiones, escala 1 a 7), leída de `Prácticas Intermedias/3_Trabajo Colaborativo/`.
- Formato de planificación: 5 elementos de la entrevista (objetivo, recursos, tiempos, evaluación del aprendizaje, interacción de participantes con recursos).
- Las 5 HU del Sprint 1: HU-01 autenticación con roles, HU-02 estudiantes e inscripción, HU-03 pauta digital, HU-04 planificación con flujo de aprobación, HU-05 historial del estudiante (26 puntos, ≈ 42 h).

Decisiones de la Fase C (backend, 2026-09-20; supuestos S-17 a S-33 en `backend/PROGRESO.md`, **pendientes de confirmar**):
- Trabajo en modo desatendido: se decidió lo razonable, se anotó como SUPUESTO y se siguió. No se hicieron commits ni push.
- El rol se lee de la base en cada petición (no del token); el login no revela si falló el correo o la clave; los borradores de evaluación son privados de su autor; el guía y el profesor del curso ven el historial completo de un estudiante asignado (S-21); el coordinador cierra las inscripciones con nota (S-22); el RUT se valida con su dígito verificador (S-24).
- Se agregaron endpoints que no estaban en el backlog: `GET /api/auth/me`, `GET /api/roles`, `PUT /api/inscripciones/{id}/cierre`.

Decisiones tomadas por las respuestas de un estudiante (2026-09-20, S-34 a S-38, **orientativas**):
- Sin guía (ciclo inicial) la planificación va directo al profesor del curso; asignar un guía en el ciclo inicial da 400.
- Se entrega con al menos 3 días de anticipación (configurable); Aproximación no entrega planificaciones.
- Se mantuvo el flujo en **secuencia** (guía → profesor del curso) de la entrevista, aunque el estudiante habló de enviarlo a ambos (S-37, sin resolver).
- El jefe de carrera usa el rol COORDINADOR mientras no se aclare (S-38).

---

## 8. Qué falta por definir

> Nota (2026-09-20): los puntos 2 a 5, 7, 8 y 9 se resolvieron **provisionalmente como supuestos** en la Fase A (ver sección 7 y `Sprint1/sprint-backlog.md`). Siguen pendientes de confirmar con el cliente o la profesora. Un estudiante respondió varias preguntas ese día (sección 13): aclaró algunas, pero **no es el cliente**.

1. ~~**Fecha de entrega del Sprint 1.**~~ Resuelto: viernes 25 de septiembre de 2026.
2. **Alcance del Sprint 1:** confirmar si solo se pide backend y base de datos o también interfaz. Confirmar si el chatbot y la nota de voz quedan para sprints posteriores.
3. **Equipo:** integrantes, roles y herramienta de tablero.
4. **Versiones permitidas** de Java, Spring Boot y MySQL, y cómo se entregará el código (repositorio, etc.).
5. **Roles del flujo de planificación:** los documentos llaman "profesor colaborador (o guía)" al docente del colegio. Falta aclarar si el "profesor del curso" que autoriza la clase es el académico de la universidad o alguien del colegio. *Un estudiante dice que la planificación se manda "al profe del ramo y al colaborador": apunta a que es el académico, pero además sugiere un envío en paralelo (S-37).*
6. **Inconsistencias entre documentos** que conviene resolver o citar por fuente:
   - Investigación en el Aula: la entrevista dice **4 semanas**, el protocolo dice 3 semanas de 6 horas (18 h) y la ficha de la asignatura dice 2 semanas (12 a 16 h). *Un estudiante dice que fueron 2 semanas (coincide con la ficha).*
   - Trabajo Colaborativo: el protocolo dice 2 semanas de 5 horas (10 h) y la ficha dice 3 semanas de 6 horas (18 h). Además, la ficha menciona "cuatro (4) semanas" pero su tabla dice 3.
   - Las pautas de observación de Investigación en el Aula dicen "VI semestre" (copiadas de Trabajo Colaborativo, que es el VI; Investigación es el VII).
   - Producto 3: las evaluaciones anexas valen 3 puntos en el texto y 5 en la tabla de la pauta.
   - La rúbrica TANEP lleva el título "2024" aunque el documento es de 2025.
7. **Qué pautas digitalizar primero:** hay varios formatos (Pedagógica y Profesional, colaborador y tutor, Especialidad y Orientación, y las pautas de observación de 20 indicadores de las intermedias).
8. **Formato de planificación de la carrera:** el cliente dice que tiene una estructura propia. No se ha entregado el archivo, solo su descripción.
9. **Datos del chatbot:** de dónde saldrán el semestre y los ramos aprobados de cada estudiante (*un estudiante dice que hoy salen de la malla en la Intranet*), y qué criterios o "estilo" de retroalimentación usa Víctor Parra.
10. **Flujo de planificación en secuencia o en paralelo (S-37):** la entrevista dice guía y luego profesor del curso; un estudiante dice que se manda a ambos. Si es en paralelo, hay que cambiar la máquina de estados de HU-04.
11. **Acceso a los chats (S-38):** el jefe de carrera quiere verlos; un estudiante no quiere que su guía los lea. Definir qué ve cada rol (chat completo, resumen o lo que el estudiante comparta) y el aviso al estudiante.
12. **Quién es el "jefe de carrera"** en el sistema: ¿el coordinador de prácticas o el director (Dr. Panes)? Se asumió el rol COORDINADOR.

---

## 9. Estado actual

- **Hecho (Fase A, `Sprint1/`):** lectura completa de los 23 documentos únicos de los `.rar`, el `.docx`, el notebook, la entrevista y la rúbrica; Product Backlog (ahora 20 HU), Sprint Backlog, BPMN As-Is y To-Be, tablero, retrospectiva (plantilla) y MER (sección 11). El 2026-09-21 se rehicieron los dos BPMN (nombres cortos, notas rojas de dolor en el As-Is y verdes de mejora en el To-Be), el MER (con una vista simple en 3 partes para dibujar a mano) y los dos backlogs (lenguaje más natural, con el estado real del sprint). El To-Be ya incluye el camino sin guía del ciclo inicial, los 3 días de anticipación, el aviso por correo (HU-19) y el acceso del jefe de carrera a los chats (HU-20). Los BPMN se generaron con un script y se validaron (XML bien formado, IDs únicos, sin flechas que atraviesen cajas), pero **todavía no se han abierto en bpmn.io**: hay imágenes guía en `Sprint1/diagramas/`.
- **Hecho (Fase B, base de datos):** `db/schema.sql`, `db/seed.sql`, `db/bd.ps1`, `db/verificacion.sql`, `db/migraciones/001_...sql`, `.gitignore` y `.env.example`; base creada y verificada (sección 12).
- **Hecho (Fase C, backend, 2026-09-20):** proyecto Spring Boot en `backend/` con las 5 HU, 32 endpoints en Swagger, 146 pruebas automáticas (0 fallos), prueba real por HTTP de cada historia y ensayo completo del guion de defensa (sección 14). Documentación: `backend/README.md`, `backend/PROGRESO.md` y `docs/GUIA_DEFENSA.md`.
- **Hecho (ajustes 2026-09-20):** modelo y reglas corregidos con las respuestas de un estudiante (sección 13): modalidad `GRUPO`, `requiere_planificacion`, ciclo inicial sin guía, planificación sin guía directo al profesor del curso, 3 días de anticipación, HU-19 sube de prioridad y nueva HU-20.
- **Pendiente:** presentación (PPT, la arma Persona 4), reunión de retrospectiva, subir el tablero a Trello o GitHub Projects, y confirmar los supuestos S-01 a S-38 con el cliente o la profesora (sobre todo S-05, S-09, S-10, S-21, S-29, S-37 y S-38).
- **Antes de la defensa:** `.\db\bd.ps1` (datos limpios) y arrancar el jar; el guion está en `docs/GUIA_DEFENSA.md`.
- **Archivos del proyecto:** `RESUMEN.md`, `CLAUDE.md`, `Sprint1/` (7 archivos y la carpeta `diagramas/`), `db/` (`schema.sql`, `seed.sql`, `bd.ps1`, `verificacion.sql`, `migraciones/`), `backend/` (112 clases Java, `pom.xml`, `README.md`, `PROGRESO.md`), `docs/GUIA_DEFENSA.md`, `.gitignore` y `.env.example`. Además hay notas de contexto en la memoria del asistente (fuera del proyecto).
- **Entorno de desarrollo:** ya instalado y verificado (ver sección 10).

---

## 10. Entorno de desarrollo instalado (2026-09-20)

Instalado con autorización expresa del usuario, desde fuentes oficiales y solo lo que faltaba:

| Herramienta | Versión | Origen | Ubicación |
|---|---|---|---|
| JDK (Eclipse Temurin) | 17.0.20.1 | winget `EclipseAdoptium.Temurin.17.JDK` | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` (`JAVA_HOME` del sistema) |
| Apache Maven | 3.9.16 | Descarga oficial de Apache (SHA512 verificado) | `C:\Users\coee1\tools\apache-maven-3.9.16` (`MAVEN_HOME` y PATH de usuario) |
| MySQL Community Server | 8.4.9 (LTS) | winget `Oracle.MySQL` (MSI oficial de Oracle) | `C:\Program Files\MySQL\MySQL Server 8.4` |

Ya estaban y no se tocaron: Git 2.49.0, Python 3.14.7 con pip, winget 1.29, JRE 8 de Oracle (sigue instalado, pero el PATH del sistema pone primero el JDK 17). **No se instaló Node/npm** porque el Sprint 1 es solo backend y el front-end no está definido.

**Configuración de MySQL:**
- Servicio de Windows `MySQL84`, inicio automático, escucha solo en `127.0.0.1:3306`. El protocolo X está desactivado.
- Codificación `utf8mb4`. Config en `C:\ProgramData\MySQL\MySQL Server 8.4\my.ini`, datos en `...\Data`.
- **El usuario `root` tiene contraseña** (definida por el usuario el 2026-09-20; se inicializó sin contraseña y luego se le asignó una). **La contraseña no se guarda en este archivo**: pídesela al usuario. Solo es accesible desde el propio equipo. Se recomienda crear un usuario propio para la aplicación (por ejemplo `isw_app`) y no guardar la contraseña de `root` en `application.properties` ni en el repositorio.
- La base del proyecto `isw_practicas` ya existe (creada el 2026-09-20, ver sección 12).

**Notas:**
- Las terminales ya abiertas antes de la instalación no ven el PATH nuevo: hay que abrir una terminal nueva.
- Existe un servicio `mysql` huérfano y detenido, de un XAMPP que ya no está en `C:\xampp`. No es del proyecto y no se modificó. No entra en conflicto con `MySQL84`.
- Spring Boot 3.x requiere Java 17 o superior; Spring Boot 2.x funciona con Java 8, pero no se usará.

---

## 11. Entregables de la Fase A (carpeta `Sprint1/`, 2026-09-20)

| Archivo | Contenido |
|---|---|
| `product-backlog.md` | 20 HU (HU-20 agregada el 2026-09-20) con criterios de aceptación y prioridad. HU-01 a HU-05 son las del Sprint 1; chatbot (HU-15) y nota de voz (HU-11) van en sprints posteriores. Incluye trazabilidad con los 6 requerimientos del cliente |
| `sprint-backlog.md` | 5 HU (26 puntos, ≈ 42 h), justificación, calendario del 21 al 25 de septiembre, definición de "Hecho", riesgos y **lista global de supuestos S-01 a S-16 y S-34 a S-38** |
| `as-is.bpmn` | Proceso actual (papel y Adecca). BPMN 2.0 con diagrama (coordenadas), 1 pool y 5 calles |
| `to-be.bpmn` | Proceso propuesto. Verde = Sprint 1, gris = sprints posteriores |
| `tablero.csv` | Actualizado el 2026-09-21: 41 tarjetas (81 h) con responsable (Persona 1 a 4), lista (Por hacer, En curso, En revisión, Hecho), estimación, fechas y una columna vacía de horas reales para la retrospectiva. T10 a T31 en "Hecho" (código del 20-sep); nuevas T08, T09 y T41. UTF-8 con BOM para que Excel muestre las tildes. Falta subirlo a Trello o GitHub Projects |
| `retrospectiva.md` | Plantilla "Qué salió bien / Qué mejorar / Acciones". **Sin rellenar a propósito**: la completa el equipo tras la reunión |
| `diagramas/` | Imágenes guía (PNG y SVG) del As-Is y del To-Be para que el equipo los dibuje a mano (2026-09-21). Se generan desde los mismos datos que los `.bpmn`. El antiguo `resumenDelSprint1.md` se borró a pedido del usuario |
| `mer.md` | `erDiagram` de Mermaid con 14 entidades (12 usadas en el Sprint 1; `NOTA_VOZ` y `MENSAJE_CHATBOT` solo en la BD), explicación de entidades y relaciones, reglas y preguntas de defensa |

**Verificaciones hechas:** XML de los BPMN bien formado, sin IDs duplicados ni referencias rotas, todos los elementos con su forma o arista, y ninguna flecha atraviesa una tarea. El MER se renderizó con Mermaid sin errores de sintaxis.

### Revisión de la Fase A (2026-09-20)

Se revisó de nuevo todo `Sprint1/` contra la entrevista, el MER real y la base ya construida:
- **BPMN:** `as-is.bpmn` y `to-be.bpmn` siguen íntegros (IDs únicos, sin referencias rotas, todos los nodos con su forma). Su contenido es fiel a la entrevista: planificación → guía → profesor del curso → clase; pauta en papel y doble digitación en Adecca en el As-Is; pauta digital, historial, nota de voz y chatbot en el To-Be.
- **Tablero:** 38 tareas, 74 h en total (Persona 1: 24 tareas y 48 h; Persona 2: 3 y 6 h; Persona 3: 6 y 10 h; Persona 4: 5 y 10 h). T10 y T11 (base de datos y script SQL) pasaron a "Hecho".
- **Correcciones hechas:** (1) `tablero.csv` T04 y `retrospectiva.md` decían "S-01 a S-15" y son 16 supuestos (ahora S-01 a S-16); (2) `mer.md` decía que el rango del puntaje y la inmutabilidad de una evaluación finalizada eran solo de la aplicación, y hoy también los hace cumplir MySQL (triggers y `CHECK`), así que se actualizó la tabla de reglas y la sección 6; (3) `sprint-backlog.md` ahora indica que la base ya está hecha.
- **Observaciones que no se cambiaron:** la numeración de tareas del tablero salta de T07 a T10 (no hay T08 ni T09; es inofensivo y ningún documento las cita); `tablero.csv` va en UTF-8 sin BOM, así que Excel puede mostrar mal las tildes (abrirlo con Datos > Desde texto/CSV en UTF-8 o importarlo a Google Sheets); la retrospectiva sigue sin rellenar a propósito.
- **Limitación:** en esta sesión no se pudo releer el PDF de la rúbrica (no hay `poppler`, Word se cuelga con ese PDF y sus fuentes no permiten extraer el texto a mano). Se contrastó con el resumen de la rúbrica de la sección 5, que se hizo cuando sí se leyó. Conviene que el equipo repase el PDF contra la lista de entregables una última vez.

**Notas para la Fase B (cumplidas el 2026-09-20, ver sección 12):**
- El script SQL debe crear las 14 tablas del MER con sus restricciones `UNIQUE` (email, RUT, inscripción por estudiante-asignatura-periodo, respuesta por evaluación-indicador) y cargar la semilla: 4 roles, 6 asignaturas, pauta de observación con 3 dimensiones y 20 indicadores, y usuarios ficticios (S-15).
- Los 20 indicadores reales están en `Prácticas Intermedias.rar`, archivo `Pauta de Observación de la clase_ Prof Colaborador_a_Trabajo colaborativo 2026.docx` (dimensiones: estructura de la clase 1–6, tareas matemáticas 7–12, ambiente de aula y recursos 13–20).
- Usar un usuario propio de MySQL (`isw_app`) y no `root` en `application.properties`; la contraseña de `root` la tiene el usuario.
- Persona 1 (código) concentra ≈ 48 h del tablero, frente a 6 a 10 h de cada uno de los demás.

---

## 12. Base de datos MySQL (Fase B, 2026-09-20)

Carpeta `db/` y archivos de raíz. **Ninguna clave está escrita en archivos**: viven en variables de entorno de usuario de Windows.

| Archivo | Contenido |
|---|---|
| `db/schema.sql` | Las 14 tablas del MER: 14 PK, 20 FK, 13 UNIQUE, 32 CHECK e índices. Re-ejecutable (borra y recrea las tablas). Incluye un procedimiento y 2 triggers sobre `respuesta_indicador` (puntaje dentro de la escala de la pauta, indicador de la misma pauta, evaluación finalizada no se modifica). Eso va más allá del MER, que lo dejaba a la aplicación |
| `db/seed.sql` | PARTE 1, catálogo real: 4 roles, 6 asignaturas (códigos internos `PEM-P1` a `PEM-P6`), pauta de observación de Trabajo Colaborativo con 3 dimensiones y 20 indicadores. PARTE 2, demo ficticia (S-15): 10 usuarios (correos `@demo.example`), 5 estudiantes, 18 inscripciones, 9 planificaciones (una en cada estado del flujo), 12 revisiones, 3 evaluaciones finalizadas con 60 respuestas |
| `db/bd.ps1` | `-Accion Instalar` (por defecto): carga schema y seed como root, crea el usuario `isw_app` y verifica. `-Accion Verificar`: consultas de `db/verificacion.sql` y 22 pruebas negativas |
| `db/verificacion.sql` | 15 bloques de consultas de solo lectura, cada uno con su resultado esperado (el bloque de integridad tiene ahora 11 controles) |
| `db/migraciones/001_grupo_y_planificacion_por_asignatura.sql` | Migración del 2026-09-20: modalidad `GRUPO` y columna `requiere_planificacion`. Se ejecuta una vez y como root; una base nueva no la necesita porque `schema.sql` y `seed.sql` ya la incluyen. Solo ASCII a propósito |
| `.gitignore`, `.env.example` | Excluyen `.env`, `target/`, claves e IDEs. La plantilla solo trae marcadores |

**Conexión de la aplicación:** base `isw_practicas`, usuario `isw_app` (permisos solo `SELECT, INSERT, UPDATE, DELETE` sobre esa base), host `localhost`, puerto 3306. Variables de entorno: `ISW_DB_PASSWORD` (clave de `isw_app`, generada al azar por `bd.ps1`), `ISW_DB_ROOT_PASSWORD` (solo para instalar; conviene borrarla después). En `application.properties` usar `${ISW_DB_PASSWORD}`, nunca la clave.

**Clave de los usuarios ficticios de demostración:** todos usan `Demo2026!` (hash BCrypt `$2a$10$`, compatible con `BCryptPasswordEncoder` de Spring). **Es solo para desarrollo y demostración, y no debe usarse en ningún sistema real.** **Nunca debe aparecer como valor por defecto en el código de la aplicación** (ni en `application.properties`, ni en clases, ni en pruebas de otro entorno). No es la clave de MySQL ni de ningún usuario real. Inicios de sesión útiles: `coordinador@demo.example`, `guia1@demo.example`, `guia2@demo.example`, `curso1@demo.example`, `camila.rojas@demo.example`.

**Verificación (2026-09-20):** probado primero en un MySQL desechable y luego en el real: 14 tablas, conteos esperados, 10 controles de integridad en 0 (hoy 11 con el nuevo) y 22 de 22 pruebas negativas rechazadas por la restricción prevista. Cada corrida de `Verificar` adelanta unos números el contador de ids (los INSERT rechazados consumen ids); es inofensivo.

**Cambio del 2026-09-20 (migración `db/migraciones/001_grupo_y_planificacion_por_asignatura.sql`, según respuestas de un estudiante):** `asignatura_practica.modalidad` admite ahora `GRUPO` y hay una columna nueva `requiere_planificacion`. Aproximación al Sistema Escolar quedó `GRUPO` sin planificaciones y Ambientes de Aprendizaje `DUPLA`. `schema.sql`, `seed.sql` y `verificacion.sql` ya incluyen el cambio (una base nueva no necesita la migración; una base creada antes se migra ejecutando ese script como root, o reinstalando con `bd.ps1`). Sigue habiendo 14 tablas, 20 FK, 13 UNIQUE y 32 CHECK.

**Limitaciones conocidas:**
- Solo hay pauta para Trabajo Colaborativo (S-07): un estudiante de Investigación en el Aula no se puede evaluar hasta HU-06/HU-07.
- `NOTA_VOZ` y `MENSAJE_CHATBOT` están creadas pero vacías (sus HU son de sprints posteriores).
- `bd.ps1` debe guardarse como UTF-8 **con BOM** (Windows PowerShell 5.1 lo lee como ANSI si no); los `.sql` van sin BOM.
- El directorio no es un repositorio Git todavía y `Documents/` (los `.rar` del cliente) no está en `.gitignore`.

### Revisión de credenciales en los archivos del proyecto (2026-09-20)

Se revisaron los 23 archivos del proyecto, más el contenido de los 5 `.rar` (29 archivos, extraídos fuera del proyecto y borrables) y los 26 `.docx`/`.doc` del cliente:
- **Claves reales de la máquina** (`ISW_DB_ROOT_PASSWORD`, `ISW_DB_PASSWORD`) y las claves de prueba que se usaron en un MySQL desechable: **0 apariciones** en cualquier archivo, texto o binario.
- **Patrones de credenciales** (asignaciones `password=`/`token=`/`api_key=` con valor, claves privadas, claves tipo Google/OpenAI/GitHub/AWS/Slack, URL con usuario y clave, `mysql -p...`): **0 hallazgos reales**. El notebook de la profesora solo *lee* la clave de Colab (`userdata.get('GOOGLE_API_KEY')`) y no trae ningún valor. En los documentos del cliente solo aparece la palabra "Secretaria" en un nombre de contacto (no es una credencial).
- **No existe ningún `.env` real** en el proyecto; `.env.example` solo trae marcadores.
- **Segunda revisión (fin de la Fase C):** se comparó el valor real de `ISW_DB_PASSWORD` y de `ISW_DB_ROOT_PASSWORD` con los 304 archivos del proyecto (incluido el código del backend, sin `Documents/`): **0 coincidencias**. Las pruebas automáticas no usan la clave de demostración: generan los tokens con `JwtService` y crean usuarios con claves aleatorias.
- **Única excepción, deliberada y aprobada por el usuario (2026-09-20):** la clave de los usuarios ficticios de demostración, `Demo2026!`, se mantiene escrita en el encabezado de `db/seed.sql` y en la sección 12 de este archivo, junto con su hash BCrypt (necesario para cargar los datos). Ambos sitios dicen que es solo para desarrollo y demostración y que no debe usarse en ningún sistema real. No es una clave de MySQL ni de ningún sistema real, y no debe aparecer como valor por defecto en el código de la aplicación.

---

## 13. Preguntas pendientes sobre los supuestos (2026-09-20)

Ninguna bloqueó la Fase C: se siguió con los supuestos S-01 a S-16 y las respuestas que fueron llegando se registran aquí (las de un estudiante, el 2026-09-20, ya están al final de esta sección). **Regla:** lo que responda un estudiante amigo del usuario es *orientativo*, no una confirmación del cliente; anotarlo como "reportado por estudiante, pendiente de confirmar".

**Para la profesora (nadie más puede responderlas):**
1. ¿Se puede presentar el Sprint 1 solo con backend y base de datos, mostrando la API con Swagger o Postman, sin interfaz gráfica? (S-01)
2. ¿Está bien dejar el chatbot y la nota de voz para sprints posteriores? (S-02)
3. ¿Hay versiones obligatorias de Java, Spring Boot y MySQL, y cómo se entregan el código y el tablero? (S-01, S-04)
4. ¿A qué hora es la presentación del viernes 25 de septiembre? (S-03)

**Para un estudiante de Pedagogía en Educación Matemática (amigo del usuario; puede servir):**
1. ¿Aproximación al Sistema Escolar y Ambientes de Aprendizaje se hacen solas o en pareja? (S-16)
2. En la práctica, ¿quién autoriza una clase después del profesor guía: el académico de la UBB a cargo de la asignatura o un profesor del colegio? (S-05)
3. ¿Tiene o puede conseguir el **formato de planificación de clase** de la carrera (archivo)? Es lo más valioso: reemplazaría el supuesto S-09.
4. Si el profesor del curso no autoriza una planificación, ¿vuelve al estudiante para corregirla y se presenta otra vez al guía? (S-10)
5. ¿De dónde sale hoy su semestre y la lista de ramos aprobados: de Adecca, de una ficha, del coordinador? (S-14)

**Preguntas adicionales para el mismo estudiante (menos urgentes; cada una dice qué decisión destraba):**
6. En Investigación en el Aula, ¿cuántas semanas van al colegio realmente: 2, 3 o 4? Los documentos se contradicen. (S-13; solo afecta a un dato de la asignatura)
7. En las prácticas en dupla, ¿cada integrante recibe su propia pauta del colaborador o una sola para la pareja? (S-11, HU-10)
8. ¿Cuántas clases planifica y da un estudiante en cada práctica, y cada cuánto entrega planificaciones? (tamaño real de HU-04)
9. Hoy, ¿cómo recibe la retroalimentación del guía sobre la planificación: verbal, por escrito, por correo? (qué debe reemplazar el flujo digital)
10. ¿Qué pautas usan más o le pesan más en la nota: las de observación de clase, las de Especialidad u Orientación? (qué pauta digitalizar primero, HU-06 y HU-07)
11. ¿Usa hoy ChatGPT u otro chatbot para preparar clases o mejorar planificaciones? ¿Qué le preguntaría a un chatbot de la carrera? (contenido de HU-15)
12. ¿Le molestaría que su profesor guía pudiera leer todo su chat con el chatbot? (HU-16 exige avisar al estudiante)
13. ¿Cuántos estudiantes atiende, más o menos, un mismo profesor guía y cuántos hay por asignatura? (volumen realista para la demo)

**Mensaje listo para enviar al estudiante (las mismas 13 preguntas, en lenguaje simple):**

> Hola, estamos haciendo un proyecto de ingeniería de software: un sistema web para que la carrera de Pedagogía en Educación Matemática (UBB) evalúe y siga las 6 prácticas de cada estudiante. Necesito tu ayuda con unas preguntas sobre cómo funcionan las prácticas en la vida real. Si no sabes alguna, di "no sé". Puedes responder con el número y un par de líneas.
>
> **Las más importantes**
> 1. Aproximación al Sistema Escolar y Ambientes de Aprendizaje, ¿se hacen solos o en pareja?
> 2. Cuando planificas una clase y tu profesor guía (el del colegio) ya la revisó, ¿quién la autoriza al final: el profesor de la asignatura de la universidad o alguien del colegio?
> 3. ¿Tienes o puedes conseguir el formato de planificación de clase que usa la carrera (el archivo o plantilla)? Sin datos de nadie, solo el formato vacío.
> 4. Si esa persona no autoriza tu planificación, ¿te la devuelven para corregirla y otra vez la presentas primero a tu profesor guía?
> 5. ¿De dónde sacan tu semestre y los ramos que llevas aprobados: de Adecca, de una ficha, de la coordinación?
>
> **Las de contexto**
> 6. En Investigación en el Aula, ¿cuántas semanas van realmente al colegio: 2, 3 o 4?
> 7. En las prácticas en pareja, ¿cada uno recibe su propia pauta de evaluación o es una sola para los dos?
> 8. ¿Cuántas clases planificas y das en cada práctica? ¿Cada cuánto entregas planificaciones?
> 9. ¿Cómo recibes hoy la retroalimentación de tu profesor guía sobre la planificación: hablando, por escrito, por correo?
> 10. ¿Qué pautas pesan más en la nota o usan más: las de observación de clase, las de Especialidad o las de Orientación?
> 11. ¿Usas ChatGPT u otro chatbot para preparar clases o mejorar planificaciones? ¿Qué le preguntarías a un chatbot hecho por la carrera?
> 12. ¿Te molestaría que tu profesor guía pudiera leer todo lo que conversas con ese chatbot?
> 13. Más o menos, ¿a cuántos estudiantes atiende un mismo profesor guía, y cuántos hay por asignatura?

Correspondencia con los supuestos: 1 → S-16, 2 → S-05, 3 → S-09, 4 → S-10, 5 → S-14, 6 → S-13, 7 → S-11 y HU-10, 8 → tamaño de HU-04, 9 → qué reemplaza el flujo digital, 10 → orden de HU-06 y HU-07, 11 → HU-15, 12 → HU-16, 13 → volumen de la demo. Las más valiosas son la 3, la 2 y la 1.

**Cómo registrar las respuestas:** cuando el usuario las pegue en una conversación nueva, anotarlas debajo con su número y la etiqueta "reportado por estudiante, pendiente de confirmar". Si alguna cambia un supuesto (por ejemplo la 3, con el formato real de planificación), actualizar también `Sprint1/sprint-backlog.md` (sección 7) y el modelo de datos si hace falta.

**Respuestas recibidas (2026-09-20)** de un estudiante de PEM, amigo del usuario. Todas son **"reportado por estudiante, pendiente de confirmar"** (orientativas, no son del cliente):

| # | Pregunta | Lo que respondió | Efecto en el proyecto |
|---|---|---|---|
| 1 | Aproximación y Ambientes: ¿solos o en pareja? | Aproximación: **grupos de 4**, sin planificaciones (solo trabajos hechos en clase). Ambientes: **parejas** y sí pedía planificaciones. Ninguna va a un colegio: **no existe profesor colaborador**. Se enfocaban en leer y presentar en la misma clase | **S-16 corregido.** Migración `db/migraciones/001` (modalidad `GRUPO` y `requiere_planificacion`). Sin guía en el ciclo inicial (asignarlo da 400). Sin guía, la planificación va directo al profesor del curso (S-34) |
| 2 | ¿Quién autoriza tras el guía? | La planificación "se debe mandar al profe del ramo y al colaborador para que la aprueben o hagan sus observaciones" | Confirma que el profesor del curso es el académico (S-05). **Diferencia con la entrevista** (secuencia guía → curso): se mantiene la secuencia y queda como S-37 por resolver |
| 3 | Formato de planificación (archivo) | **Sin respuesta** | Sigue pendiente (S-09) |
| 4 | Si no autorizan, ¿vuelve y se presenta primero al guía? | "Luego de recibir las observaciones debes mejorar tu planificación" | Compatible con S-10; no dice si vuelve primero al guía |
| 5 | ¿De dónde salen semestre y ramos aprobados? | "Me parece que lo ven por Intranet, la malla de los estudiantes" | S-14: la fuente real es la Intranet, no Adecca. Hoy los carga el coordinador; una importación queda como mejora futura |
| 6 | Semanas al colegio en Investigación en el Aula | **2 semanas** | Coincide con la ficha de la asignatura (S-13) |
| 7 | En pareja, ¿pauta propia o compartida? | Cada alumno tiene **su propia pauta** (evaluación individual) | **Confirma** S-11 y el modelo (una evaluación por inscripción). Corregido el texto de HU-10 |
| 8 | ¿Cuántas clases y cada cuánto entrega? | Se planifican **todas** las clases con docencia directa; se entrega **como mínimo unos 3 días antes** | **Nueva regla** S-35 (3 días de anticipación, configurable) y S-36 |
| 9 | ¿Cómo recibe la retroalimentación del guía? | **Todo por correo, para que quede respaldo** | HU-19 (avisos por correo) sube de Baja a Media. Las revisiones del sistema ya dejan el respaldo |
| 10 | ¿Qué pauta pesa más? | No ha cursado Orientación; las observaciones del colaborador "son de forma general" | Sin respuesta útil: sigue pendiente el orden de HU-06/HU-07 |
| 11 | ¿Usa IA? ¿Qué le preguntaría a un chatbot de la carrera? | Sí usa IA para preguntar formas de enseñar y mejorar la planificación; le preguntaría lo mismo | Contenido esperado de HU-15 (anotado en el backlog) |
| 12 | ¿Le molestaría que su guía lea todo su chat? | **Sí**, "no existiría mucha privacidad" | Tensión con el requerimiento 6 y HU-16 (S-38): el acceso del guía al chat queda **por decidir con el cliente** |
| 13 | ¿A cuántos estudiantes atiende un guía? | Entre **15 y 20** | Volúmenes pequeños por guía: no hace falta paginar en el Sprint 1 (S-26) |

**Dato nuevo del usuario (2026-09-20):** el **jefe de carrera solicitó poder ver los chats de los alumnos** para llevar registro y saber cómo ha aportado el chatbot. Se agregó **HU-20** al Product Backlog (Sprint 4) y el supuesto **S-38** (se asume el rol COORDINADOR mientras no se aclare si el jefe de carrera es otra persona). Hay que reconciliarlo con la privacidad que reclama el estudiante (aviso visible, y decidir qué ve cada rol).

**Preguntas que siguen abiertas:** #3 (archivo del formato de planificación), #10 (peso de las pautas) y S-37 (¿el flujo es en secuencia o la planificación va al guía y al profesor del curso a la vez?). Estas tres conviene llevarlas a la profesora o al cliente.

---

## 14. Fase C: backend Spring Boot (2026-09-20)

**Dónde:** `backend/`. **Cómo arrancar y probar:** `backend/README.md`. **Estado, endpoints y supuestos S-17 a S-38:** `backend/PROGRESO.md`. **Demo y preguntas de defensa:** `docs/GUIA_DEFENSA.md`.

**Arquitectura:** Java 17, Spring Boot 3.5, Maven; paquete `cl.ubb.pem.practicas` con `controller, service, repository, entity, dto, security, exception, config`. DTO en lugar de entidades; manejador global de errores en español (`ApiError`); CORS abierto a `http://localhost:*`; Swagger en `/swagger-ui.html` con botón Authorize. Seguridad sin estado: login con BCrypt y token JWT de 8 horas (S-06); `@PreAuthorize` por rol en cada endpoint y `AccesoService` para "quién ve qué". La app usa `isw_app` con `${ISW_DB_PASSWORD}` y `ddl-auto=validate`. El secreto del JWT es `ISW_JWT_SECRET` (opcional; si falta se genera uno temporal).

**Las 5 historias (32 endpoints):**

| HU | Qué hace | Endpoints principales |
|---|---|---|
| HU-01 | Inicio de sesión y roles | `POST /api/auth/login`, `GET /api/auth/me`, `GET /api/roles` |
| HU-02 | Estudiantes, profesores, asignaturas e inscripciones | `/api/estudiantes`, `/api/profesores`, `/api/asignaturas`, `/api/inscripciones` (incluye `PUT .../cierre`) |
| HU-03 | Pauta digital de 20 indicadores (1 a 7) y evaluación con puntaje automático | `/api/pautas`, `/api/evaluaciones` (`/respuestas`, `/finalizar`) |
| HU-04 | Planificación con flujo de aprobación | `/api/planificaciones` (`/enviar`, `/revision-guia`, `/revision-curso`) |
| HU-05 | Historial (recorrido) del estudiante | `GET /api/estudiantes/{id}/historial`, `GET /api/estudiantes/me/historial` |

**Reglas de negocio clave:**
- Correo y RUT únicos (409); RUT con dígito verificador (400); una inscripción por estudiante, asignatura y periodo (409); el guía debe tener rol PROFESOR_GUIA y el profesor del curso PROFESOR_CURSO (400).
- El guía (colaborador) solo existe si la asignatura va a un colegio: en el ciclo inicial asignarlo da 400 y donde va a un colegio es obligatorio.
- Evaluación: puntaje entero dentro de la escala de la pauta e indicador de esa pauta (400); solo el guía asignado a la inscripción evalúa (403); finalizar exige los 20 indicadores, calcula total (máx. 140) y promedio con 2 decimales y deja la evaluación inmutable (409). El borrador es privado del evaluador y del coordinador.
- Planificación: BORRADOR → EN_REVISION_GUIA → EN_AUTORIZACION → AUTORIZADA; devolver exige comentario; si el profesor del curso devuelve, el flujo repite desde el guía (S-10); una AUTORIZADA no se modifica; cada decisión queda en `revision_planificacion`. Sin guía, "enviar" lleva directo a EN_AUTORIZACION (S-34). Se envía con al menos 3 días de anticipación a la clase (`app.planificacion.dias-anticipacion-minimos`, S-35). Aproximación no entrega planificaciones (S-36).
- Historial: inscripciones en orden cronológico con sus evaluaciones finalizadas y planificaciones; incluye semestre actual y asignaturas aprobadas (contexto del futuro chatbot).

**Pruebas (146, 0 fallos):** unitarias con Mockito (reglas de cada servicio) y una clase de integración por historia (`HU0xIntegracionTest`, MockMvc contra la MySQL real con rollback). Además: 131 comprobaciones por HTTP real (una corrida por historia), el ensayo completo del guion de defensa (59) y 11 comprobaciones de las reglas nuevas. **Las pruebas de integración asumen los datos exactos del seed**: probar a mano deja filas nuevas y hay que volver a `.\db\bd.ps1`.

**Trampas ya resueltas:** con `@Enumerated(STRING)` Hibernate 6 espera una columna `enum` de MySQL y `validate` falla (se arregló con `@JdbcTypeCode(SqlTypes.VARCHAR)`); las columnas TEXT llevan `columnDefinition = "text"`; `JwtAuthenticationFilter` no es `@Component` (correría dos veces); Jackson con `accept-float-as-int=false` para que un puntaje `4.5` no se trunque a 4; en este entorno los heredocs largos con Bash fallan (usar la herramienta Write).

**Limitaciones (no bloquean la entrega):** sin `@Version` (control de concurrencia), sin límite de intentos de login, cambio o recuperación de contraseña, refresco de token ni paginación (S-26); un estudiante de Investigación en el Aula no se puede evaluar todavía (solo hay pauta de Trabajo Colaborativo); Swagger abierto (necesario para la demo).

**Siguientes pasos:** confirmar supuestos (S-37 y S-38 primero); Sprint 2: configurar pautas (HU-06), pautas de las prácticas finales y tutor (HU-07), informes (HU-08), nota final (HU-09), duplas y grupos (HU-10); Sprints 3 y 4: nota de voz, avisos por correo (HU-19, sube de prioridad), panel del coordinador, chatbot (HU-15 a HU-17) y acceso del jefe de carrera a los chats (HU-20).
