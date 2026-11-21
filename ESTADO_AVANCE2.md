# Estado del proyecto para el Avance 2

> Preparado el 2026-10-26, sin tocar código, sin ejecutar pruebas y sin commit. Fuentes: `Sprint1/product-backlog.md`
> (v1.3), `backend/PROGRESO.md`, `objetivosAFUTURO.md`, `CLAUDE.md` y `git` (ramas, commits y etiquetas).
> El código no cambió desde el 17-oct (último commit que toca `backend/src`), así que los números de ese día siguen vigentes.

**Ojo con el número de HU:** el backlog **no tiene 30 HU, tiene 31** (HU-01 a HU-31). Llegó a 30 en la Fase E
(22-sep, commit `4d6badb` "Product Backlog a 30 HU") y el 10-oct se agregó la **HU-31** (cartola, versión 1.3 del
backlog). `objetivosAFUTURO.md` todavía dice "30" en su sección 1: está desactualizado. En la defensa, decir **31**.

---

## 1. Las 31 HU, una por una

Criterio usado:
- **TERMINADA:** cumple todos sus criterios de aceptación y tiene pruebas automáticas.
- **PARCIAL:** construida y probada, pero con algún criterio o parte del alcance pendiente (se dice cuál).
- **PENDIENTE:** sin código.

Etapas: **Sprint 1** = Fases A a D (documentos, base, backend del 20-sep y front-end del 21-sep). **Fase E** = 22-sep,
historias adelantadas. Las **Fases F, G y H** no construyeron HU nuevas (F: datos de demo; G: MER y BPMN; H: ronda de
pruebas, que agregó la regla S-47 a HU-04/HU-21). **Sprint 2** = desde el 10-oct.

| HU | Historia | Estado | Dónde se hizo | Nota |
|---|---|---|---|---|
| HU-01 | Iniciar sesión con roles | TERMINADA | Sprint 1 (Fase C) | En Sprint 2 se amplió: 6 roles, segundo rol opcional, límite de intentos de login |
| HU-02 | Registrar estudiantes e inscribirlos | TERMINADA | Sprint 1 (Fase C) | Sprint 2 agregó fecha de cierre (HU-31) y tutor (HU-07) |
| HU-03 | Evaluar con la pauta digital | PARCIAL | Sprint 1 (Fase C) | Criterios cumplidos, pero no hay forma de corregir una evaluación finalizada; con HU-06/07 ya hay más pautas |
| HU-04 | Planificar y aprobar en dos etapas | PARCIAL | Sprint 1 (Fase C); regla S-47 en Fase H | Usa los 5 campos de la entrevista, no el formato oficial `.docx` (S-45); flujo en secuencia sin confirmar (S-37) |
| HU-05 | Ver el historial del estudiante | TERMINADA | Sprint 1 (Fase C) | — |
| HU-06 | Configurar pautas de evaluación | TERMINADA | **Sprint 2** (11-oct) | Con versiones (migración 005), 9 pruebas, pantalla del coordinador |
| HU-07 | Pautas de Pedagógica y Profesional (con tutor) | PARCIAL | **Sprint 2** (16-oct) | 4 de 4 criterios cumplidos, pero solo 2 de las 12 pautas cargadas (ver sección 3) |
| HU-08 | Informes por asignatura y por estudiante | PENDIENTE | — | Planificada para Sprint 2, no empezada |
| HU-09 | Nota final ponderada | PENDIENTE | — | Bloqueada: faltan las fórmulas del cliente |
| HU-10 | Prácticas en dupla y en grupo | PENDIENTE | — | La asignatura guarda la modalidad, pero no se agrupa |
| HU-11 | Nota de voz del evaluador | PARCIAL | Fase E (22-sep) | El audio no se puede reemplazar, solo corregir el texto |
| HU-12 | Pasar la nota de voz a texto | PARCIAL | Fase E (22-sep) | Solo funciona en Chrome (Web Speech API); herramienta sin confirmar (S-40) |
| HU-13 | Exportar notas para Adecca | PENDIENTE | — | Sprint 3; falta el formato de Adecca |
| HU-14 | Panel de monitoreo del coordinador | PENDIENTE | — | Sprint 3 |
| HU-15 | Chatbot de retroalimentación | PENDIENTE | — | Sprint 4; solo diseñado (RetroPlani Mate, S-46) |
| HU-16 | El guía ve el chat y la actividad | PENDIENTE | — | La parte "planificaciones y evaluaciones" ya la cubren HU-04/HU-05; el chat no existe |
| HU-17 | Calibrar el estilo del chatbot | PENDIENTE | — | Sprint 4 |
| HU-18 | Registrar informes y productos | PENDIENTE | — | Sprint 5 o más |
| HU-19 | Avisos por correo | PENDIENTE | — | Sprint 3 (tentativo) |
| HU-20 | El jefe de carrera consulta los chats | PENDIENTE | — | Sprint 4; el rol `JEFE_CARRERA` ya existe |
| HU-21 | Adjuntar documentos a la planificación | TERMINADA | Fase E (22-sep); obligatorio desde Fase H (S-47) | — |
| HU-22 | Documento formal de la evaluación | TERMINADA | Fase E (22-sep) | — |
| HU-23 | Buscar estudiante | TERMINADA | Fase E (22-sep) | — |
| HU-24 | Accesos rápidos en el menú | TERMINADA | Fase E (22-sep) | — |
| HU-25 | Archivos oficiales de las pautas | PENDIENTE | — | Ya destrabada por HU-06 |
| HU-26 | Cambiar la contraseña | TERMINADA | **Sprint 2** (11-oct) | `PUT /api/auth/password`; el token anterior sigue valiendo (S-50) |
| HU-27 | Configurar parámetros del sistema | PENDIENTE | — | — |
| HU-28 | Exportar el historial en PDF | PENDIENTE | — | Puede reutilizar OpenPDF de HU-31 |
| HU-29 | Base de conocimiento del chatbot | PENDIENTE | — | Sprint 4 |
| HU-30 | Validar el chatbot con jueces | PENDIENTE | — | Sprint 5 o más |
| HU-31 | Cartola de prácticas en PDF | TERMINADA | **Sprint 2** (10-oct) | Rol `JEFE_CARRERA`, migración 003, 14 pruebas |

**Totales:** 10 TERMINADAS + 5 PARCIALES = **15 con código** · **16 PENDIENTES** · 31 en total.

**Aviso:** el Product Backlog (sección 3) está desactualizado: todavía muestra HU-06, HU-07 y HU-26 como "Pendiente".
Conviene corregirlo antes del Avance 2 para que el documento coincida con lo que se muestra.

---

## 2. Los dos conteos

### a) Todo el proyecto hasta hoy (26-oct)

**15 de 31 HU tienen código y pruebas** (evidencia real):
- **10 terminadas:** HU-01, 02, 05, 06, 21, 22, 23, 24, 26, 31.
- **5 parciales:** HU-03, 04, 07, 11, 12.

Si te piden solo las completas sin reservas: **10**.

### b) Solo del Sprint 2 (desde el 10-oct, sin Sprint 1 ni Fases E/F/G/H)

**4 HU nuevas:**

| HU | Fecha | Estado |
|---|---|---|
| HU-31 Cartola de prácticas | 10-oct | TERMINADA |
| HU-06 Configurar pautas | 11-oct | TERMINADA |
| HU-26 Cambiar la contraseña | 11-oct | TERMINADA |
| HU-07 Pautas con tutor | 16-oct | PARCIAL (primera parte) |

= **3 terminadas + 1 parcial.**

Además, en el Sprint 2 se hizo trabajo que **no es una HU nueva** pero sí se puede mostrar:
- Segundo rol opcional ("birol") y jefe de carrera con lectura total (confirmado por el cliente, migración 004): amplía HU-01.
- Límite de intentos de login (5 fallos → 15 min de bloqueo, 429): amplía HU-01.
- Perfil de producción, `datos-iniciales.sql` y carpeta `deploy/` (instalador Linux, nginx con HTTPS, systemd,
  respaldos), probada en Ubuntu 26.04 en WSL. Aún sin servidor real de la UBB.
- Corrección de las 7 observaciones de la ronda de pruebas del 11-oct (front-end).
- Laboratorio de gestión de configuración de un compañero en la rama `lab/configuracion` (ver sección 4).

---

## 3. HU-07 en detalle

**Qué pide:** completar las pautas de Especialidad y de Orientación de las dos prácticas finales: **4 por estudiante en
Práctica Pedagógica y 8 en Práctica Profesional = 12 pautas**, con el rol tutor.

### Lo que está hecho (16-oct)

| Criterio de aceptación | Estado |
|---|---|
| 1. Escala de 2 a 7 y dimensiones A a E | Cumplido (la escala es configurable desde HU-06; fuera de rango da 400). Las pautas cargadas usan A-D |
| 2. Indica si es 1.ª o 2.ª observación | Cumplido, como dato de la **pauta** (`pauta.numero_observacion`, S-55) |
| 3. El tutor solo evalúa con pautas de tutor | Cumplido en los dos sentidos (tutor + pauta de colaborador = 400; guía + pauta de tutor = 400; tutor ajeno = 403) |
| 4. Puntaje máximo calculado con los indicadores | Cumplido (`puntajeMaximo` = indicadores × escala máxima) |

- **Base:** migración `006_tutor_y_numero_de_observacion.sql`: rol `TUTOR` (id 6), `asignatura_practica.lleva_tutor`,
  `inscripcion.tutor_id`, `pauta.numero_observacion`. 3 pruebas negativas nuevas en `bd.ps1` (una encontró un error
  real: el CHECK dejaba pasar `NULL`, corregido).
- **Backend:** endpoint nuevo `PUT /api/inscripciones/{id}/tutor` (solo coordinador; asigna, cambia o quita); tutor
  opcional al inscribir; el coordinador puede registrar tutores; el tutor ve en lectura lo de sus inscripciones (S-58).
- **Front-end:** menú del tutor, botón «Evaluar», "Nueva evaluación" con solo sus pautas, botón «Tutor» del
  coordinador, campo "Observación" en el editor de pautas.
- **Pruebas:** 8 nuevas (7 en `HU07TutorIntegracionTest` + 1 en `AccesoServiceTest`). Recorrida en Chrome headless:
  el tutor evaluó a Sebastián Vargas con 123 de 133 (promedio 6,47).
- **Demo:** `tutor1@demo.example` (Rodrigo Saavedra) es tutor de la inscripción 17.

### Pautas: **12 de 12 cargadas** (las 10 restantes el 2026-11-21)

| Práctica | Pauta | Evaluador | Estado |
|---|---|---|---|
| Profesional | Especialidad, 1.ª observación | Colaborador y tutor | **Cargadas** (ids 2, 3; 19 indicadores, A-D, máx. 133) |
| Profesional | Especialidad, 2.ª observación | Colaborador y tutor | **Cargadas** (ids 4, 5; 23 indicadores, A-D, máx. 161) |
| Profesional | Orientación, 1.ª observación | Colaborador y tutor | **Cargadas** (ids 6, 7; 18 indicadores, A-E, máx. 126) |
| Profesional | Orientación, 2.ª observación | Colaborador y tutor | **Cargadas** (ids 8, 9; 23 indicadores, A-E, máx. 161) |
| Pedagógica | Especialidad, 1.ª y 2.ª | Colaborador | **Cargadas** (ids 10, 11; 19 y 23 indicadores, A-D) |
| Pedagógica | Orientación, 1.ª y 2.ª | Colaborador | **Cargadas** (ids 12, 13; 18 y 23 indicadores, A-E) |

Todas son **reales**, copiadas tal cual de los `.docx` de `Documents/6_Práctica Profesional.rar` y
`Documents/5_Práctica Pedagógica.rar` (escala 2 a 7; el puntaje máximo de cada una coincide con el "PUNTAJE TOTAL"
impreso). Están en `db/seed.sql` y `db/datos-iniciales.sql`. Las de Orientación traen **5 dimensiones (A a E)**. Los
errores de tipeo del original ("PEDAGÓGICAes", "desempeño PEDAGÓGICA", "las los estudiantes") se corrigieron con el
texto de las pautas de Práctica Profesional. Detalle en `backend/PROGRESO.md`, sección del 2026-11-21.

### Lo que falta
1. ~~Cargar las otras 10 pautas~~ (hecho el 2026-11-21).
2. ~~Elegir el tutor en el mismo formulario de "Nueva inscripción"~~ (hecho el 2026-11-21: un solo POST con `tutorId`; el botón «Tutor» sigue para cambiarlo después).
3. Preguntar al cliente: ¿una evaluación por pauta e inscripción? (S-55); ¿el tutor participa en planificaciones? (S-58);
   ¿hay tutor en Práctica Pedagógica? (hoy solo en Profesional).
4. La "NOTA FINAL" de la pauta impresa (puntaje → nota) no se calcula: es parte de HU-09.

**Cómo decirlo en la defensa:** "HU-07 tiene el modelo, la lógica y los 4 criterios de aceptación cumplidos y probados;
falta cargar 10 de las 12 pautas, que es carga de datos con la herramienta de HU-06, no desarrollo nuevo."

---

## 4. Git

Revisado el 26-oct con `git rev-list --count`, `git branch -a`, `git tag` y `git ls-remote`.

| Dato | Valor |
|---|---|
| Commits en `main` | **14** (13 + 1 merge) |
| Commits en todo el repositorio (todas las ramas) | **29** |
| Commits del Sprint 2 en `main` (desde el 10-oct) | **2**: `367a995` (17-oct, todo el Sprint 2 en un commit) y `0eeff34` (17-oct, merge) |
| Commits del Sprint 2 en todas las ramas | **17**: los 2 de `main` + 15 de `lab/configuracion` (16-oct, AleJC77) |

**Ramas:**

| Rama | Dónde | Estado |
|---|---|---|
| `main` | local y `origin` | Rama principal (`0eeff34`) |
| `frontend` | solo local | Front-end de la Fase D. Ya integrada en `main` (apunta a `c5f968f`, que está en `main`) |
| `lab/configuracion` | solo `origin` | 15 commits del 16-oct de AleJC77: laboratorio de gestión de configuración (versiones fijas en el pom, Maven Wrapper, perfil test, credenciales fuera del código, perfiles dev/prod, Flyway, dependabot, `compose.yaml`, README, versión `v1.0.0`). **No está integrada en `main`** |

**Etiqueta:** `v1.0.0`, sobre `23264d9` (22-sep, mejora de `bd.ps1`).

**Para la defensa, ojo:**
- Todo el Sprint 2 del sistema está en **un solo commit** (`367a995`). Si preguntan por la frecuencia de commits, es un
  punto débil: el trabajo se hizo del 10 al 17-oct y se subió junto.
- `lab/configuracion` usa Flyway y un esquema de pruebas propio; `main` usa `db/migraciones/` y `bd.ps1`. Si se integra,
  hay que reconciliar (su último commit habla de "161 pruebas", el número del Sprint 1).
- Sin seguimiento en git: `CONTEXTO_CHAT.md`, `DESPLIEGUE.md`, `GUIA_ESTUDIO_SPRINT1.md`, `objetivosAFUTURO.md`, este
  archivo y un archivo suelto llamado `how 23264d9` (parece salida de un `git show` guardada por error).

---

## 5. Pruebas y endpoints

| Dato | Valor | Cómo se sabe |
|---|---|---|
| Pruebas automáticas (JUnit) | **218, 0 fallos** | Último `mvn clean package`, 17-oct. El código no cambió desde entonces. (Son 207 métodos `@Test`; las pruebas parametrizadas suman casos) |
| Endpoints REST | **50** | Contados hoy en el código (`@Get/Post/Put/DeleteMapping`) y 50 operaciones en Swagger el 17-oct |
| Pruebas negativas de la base | 27 | `db/bd.ps1` |
| Pruebas del control de versión de MySQL | 40 | `db/pruebas-version-mysql.ps1` |
| Roles / tablas / migraciones | 6 / 16 / 6 | — |

Evolución de las pruebas: 146 (Sprint 1) → 161 (Fase E y H) → 175 (HU-31) → 201 (segundo rol) → 210 (HU-06) → 218 (HU-07).
Endpoints: 43 tras HU-31 → 49 tras HU-06 → 50 tras HU-07.

Antes de mostrar algo: `.\db\bd.ps1` y luego `mvn clean package` (las pruebas exigen los datos limpios de `seed.sql`).

---

## 6. Supuestos sin confirmar que probablemente pregunten

Ordenados de más a menos probable. Ninguno está confirmado por el cliente (lo único confirmado en el Sprint 2 es el
segundo rol y la lectura total del jefe de carrera).

| ID | Supuesto | Por qué lo pueden preguntar |
|---|---|---|
| **S-37** | La planificación va en **secuencia** (guía → profesor del curso), aunque un estudiante dijo que se manda a los dos a la vez | Es el corazón de HU-04 y sigue abierto desde el Sprint 1 |
| **S-45** | La planificación usa los 5 campos de la entrevista, **no el formato oficial `.docx`** que entregó la carrera el 22-sep | La carrera ya entregó el formato: van a preguntar por qué no se migró |
| **S-38** | Privacidad de los chats: qué ve el guía y el jefe de carrera (la parte del rol ya se resolvió) | Bloquea HU-16 y HU-20 |
| **S-55** | El número de observación (1.ª/2.ª) es dato de la **pauta**, no de la evaluación | Decisión de diseño nueva de HU-07 |
| **S-58** | El tutor ve en lectura lo de sus estudiantes y **no** participa en las planificaciones | HU-07, nadie lo confirmó |
| **S-08 / HU-09** | Solo se calcula puntaje total y promedio; la conversión a nota y el **peso de las pautas** no están | El cliente lo espera; faltan sus fórmulas |
| **S-47** | Enviar una planificación exige un documento adjunto | Regla que agregamos nosotros |
| **S-54** | El estudiante puede agregar documentos a la planificación en cualquier estado (incluso autorizada) | Puede verse como un hueco |
| **S-40** | La transcripción la hace Chrome (Web Speech API), sin herramienta acordada | Solo funciona en Chrome |
| **S-10** | Si el profesor del curso devuelve la planificación, el flujo vuelve a empezar desde el guía | Relacionado con S-37 |
| **S-21** | El guía o profesor del curso ve el historial **completo** del estudiante, no solo su práctica | Tema de privacidad |
| **S-49 / S-50** | Bloqueo de login en memoria (se pierde al reiniciar); el token sigue valiendo 8 h tras cambiar la clave | Preguntas de seguridad para producción |
| **S-06** | Login propio, no el de la UBB, con token de 8 horas | Relevante para el servidor de la universidad |
| **S-34 / S-35 / S-36** | Sin guía va directo al profesor del curso; 3 días de anticipación; una planificación por clase | Vienen de un estudiante, no del cliente |
| **S-16** | Aproximación en grupos de 4 sin planificaciones; Ambientes en parejas | Viene de un estudiante; además HU-10 (duplas) no está hecha |
| **S-51 / S-52 / S-53** | Una pauta con solo un borrador ya cuenta como usada; editar crea versión y la anterior queda inactiva; una errata también versiona | Diseño de HU-06 |
| **S-56 / S-57** | Indicadores numerados corrido (1 a 19) y no A1, B1...; el tutor solo se cambia con la inscripción en curso | Detalles de HU-07 |
| **S-48** | Fecha de cierre deducida del periodo para lo cerrado antes del 10-oct | Afecta la cartola (HU-31) |
| **S-43** | Tamaños y formatos de archivo (10 MB documentos, 25 MB audio) | Decisión propia |

Supuestos ya resueltos (no vale la pena defenderlos como abiertos): **S-09** (el formato oficial llegó; queda S-45),
**S-11** (ya no aplica para cuentas con segundo rol, confirmado por el cliente el 11-oct) y la **parte del rol de S-38**
(rol `JEFE_CARRERA`).
