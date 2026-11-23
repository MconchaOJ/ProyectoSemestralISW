# Estado del proyecto y lo que falta

**Sistema de Formación Práctica PEM-UBB** · Documento de todo el equipo · Actualizado al **22 de noviembre de 2026**

> **Para qué sirve.** Para responder con datos a la pregunta «¿qué les falta?» de la profesora o del jurado.
> Cada sección se puede decir en menos de 30 segundos. La lista oficial de las historias (**31, HU-01 a HU-31**)
> está en `Sprint1/product-backlog.md`. El detalle técnico de cada una está al final de `backend/PROGRESO.md`.
> Los códigos **S-xx** son supuestos: decisiones nuestras que el cliente todavía no confirma.

---

## 1. En 30 segundos

- **29 de las 31 historias están construidas y probadas.** Las 2 que faltan (**HU-13** y **HU-19**) están
  **bloqueadas por datos que tiene que entregar la universidad**, no por trabajo nuestro pendiente.
- Funciona de punta a punta con **6 roles**: estudiante, profesor guía, profesor del curso, coordinador, jefe de
  carrera y tutor. Cubre planificación con aprobación en dos etapas, las 13 pautas digitales, la nota de voz con
  transcripción, la nota final ponderada, los informes en PDF y Excel, el panel de monitoreo y el chatbot RetroPlani Mate.
- **Cifras:** 28 tablas, 20 migraciones, 100 endpoints, **373 pruebas automáticas sin fallos** y 45 pruebas
  negativas de la base de datos. El último repaso completo fue el 22-nov: pruebas, base y los 6 roles en Chrome.
- **Lo que viene no es código:** publicar el sistema en el servidor (guía paso a paso en `DESPLIEGUE.md`) y
  confirmar con el cliente los supuestos de la sección 4.

---

## 2. Estado de las 31 historias

**Cómo leer la tabla:** ✅ construida y probada · ⛔ bloqueada por un dato externo. La fecha es cuándo quedó
completa; si se amplió después, se indica.

### 2.1 Acceso y administración

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-01 | Iniciar sesión con roles | ✅ 20-sep | Desde el 11-oct, un segundo rol opcional (confirmado por el cliente). Límite de intentos fallidos de login |
| HU-26 | Cambiar la contraseña | ✅ 11-oct | Pantalla «Mi cuenta» |
| HU-27 | Configurar los parámetros del sistema | ✅ 22-nov | Anticipación, umbral del panel, tamaños de archivo y exigencia de la escala de notas (S-80, S-92) |

### 2.2 Gestión académica

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-02 | Registrar estudiantes e inscribirlos | ✅ 20-sep | El tutor se elige al inscribir (21-nov) |
| HU-10 | Prácticas en dupla y en grupo | ✅ 21-nov | S-77 y S-78 por confirmar |

### 2.3 Planificación

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-04 | Planificar y aprobar en dos etapas | ✅ 20-sep | Desde el 21-nov, con el **formato oficial** de la carrera (S-45). Revisión en secuencia (S-37) |
| HU-21 | Adjuntar documentos a la planificación | ✅ 22-sep | Borrar y reemplazar desde el 22-nov |
| HU-19 | Avisos por correo de cambios de estado | ⛔ | Ver sección 3 |

### 2.4 Evaluación y pautas

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-03 | Evaluar con la pauta digital | ✅ 20-sep | Cada evaluación guarda su nota (22-nov) |
| HU-06 | Configurar pautas de evaluación | ✅ 11-oct | Con versiones: editar una pauta usada crea la versión siguiente |
| HU-07 | Pautas de Práctica Pedagógica y Profesional (con tutor) | ✅ 21-nov | Las **13 pautas reales** cargadas (269 indicadores), rol tutor y escala 2 a 7 |
| HU-09 | Nota final ponderada | ✅ 22-nov | Estructura evaluativa oficial de Práctica Pedagógica; pesos los define el profesor del curso (S-83) |
| HU-22 | Documento formal de la evaluación | ✅ 22-sep | Borrar y reemplazar desde el 22-nov |
| HU-25 | Archivos oficiales de las pautas | ✅ 22-nov | Con historial de versiones (S-95, S-96) |

### 2.5 Retroalimentación por voz

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-11 | Nota de voz del evaluador | ✅ 22-sep | «Volver a grabar» desde el 21-nov |
| HU-12 | Pasar la nota de voz a texto | ✅ 22-sep | La transcribe el navegador (Chrome); la herramienta no está confirmada (S-40) |

### 2.6 Monitoreo e informes

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-05 | Ver el historial del estudiante | ✅ 20-sep | Desde el 22-nov incluye sus informes y productos (HU-18) |
| HU-08 | Informes por asignatura y por estudiante | ✅ 21-nov | PDF y, desde el 22-nov, Excel |
| HU-14 | Panel de monitoreo del coordinador | ✅ 21-nov | Pendientes, avance por estudiante; umbral de 3 días (S-79) |
| HU-23 | Buscar estudiante | ✅ 22-sep | Por nombre, RUT o correo |
| HU-28 | Exportar el historial en PDF | ✅ 21-nov | También en Excel |
| HU-31 | Cartola de prácticas (jefe de carrera) | ✅ 10-oct | PDF filtrado por fecha de cierre |

### 2.7 Chatbot (RetroPlani Mate)

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-15 | Chatbot de retroalimentación | ✅ 26-oct | Probado con el modelo real (Groq). Solo en las prácticas 5 y 6 |
| HU-16 | El guía ve el chat del estudiante | ✅ 26-oct | Solo el guía de esa inscripción (S-60) |
| HU-17 | Calibrar el estilo del chatbot | ✅ 26-oct | Pantalla «Configuración del asistente» |
| HU-20 | El jefe de carrera consulta los chats | ✅ 26-oct | Con aviso al estudiante y registro de cada lectura; desde el 22-nov también el coordinador (S-66) |
| HU-29 | Base de conocimiento del chatbot | ✅ 26-oct | Los 10 documentos de la propuesta; fragmentos por TF-IDF (S-70, S-74) |
| HU-30 | Validar el chatbot (jueces y estudiantes) | ✅ 26-oct | Escala SUS y rúbrica de 9 criterios dentro del sistema (S-75) |

### 2.8 Documentos del estudiante e interfaz

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-18 | Informes y productos de la práctica | ✅ 22-nov | Informe final, diario de reflexión, proyecto de intervención u otro (S-93, S-94) |
| HU-24 | Accesos rápidos en el menú | ✅ 22-sep | — |

### 2.9 Integración con sistemas de la universidad

| HU | Historia | Estado | Notas |
|---|---|---|---|
| HU-13 | Exportar notas finales para Adecca | ⛔ | Ver sección 3 |

**Totales: 29 ✅ · 2 ⛔ = 31 HU.**

---

## 3. Lo que falta y por qué

Las dos historias pendientes **no se pueden construir bien sin un dato que no depende de nosotros**. Construirlas
adivinando ese dato sería trabajo que probablemente habría que rehacer.

| HU | Qué necesita | Quién lo entrega | Qué ya está listo | Esfuerzo estimado una vez que llegue el dato |
|---|---|---|---|---|
| **HU-13** Exportar notas finales para Adecca | El **formato de archivo que acepta Adecca** (columnas, separador, codificación, cómo se identifica al estudiante y a la asignatura) | La universidad (quien administra Adecca) | La nota final de cada inscripción ya existe (HU-09), y el sistema ya genera PDF y Excel (HU-08). Falta solo armar el archivo con el formato exacto | Pequeño: un endpoint y un botón en «Notas finales» |
| **HU-19** Avisos por correo | La **configuración del correo de la UBB** (servidor SMTP, cuenta remitente y permiso para enviar avisos automáticos) | El área de informática de la UBB (pregunta ya incluida en `DESPLIEGUE.md`, sección 6) | Cada revisión de una planificación ya queda registrada con autor, fecha y comentario: el correo sería solo el aviso | Mediano: enviar el aviso cuando la planificación cambia de etapa, sin que un correo fallido bloquee la revisión |

**Fuera del backlog, como mejora futura:** importar los ramos aprobados desde la Intranet (S-14). Hoy el
coordinador los registra al cerrar cada inscripción. Requiere acceso a la Intranet, que tampoco depende de nosotros.

---

## 4. Construido, pero con supuestos por confirmar con el cliente

Todo esto **funciona hoy**. Si el cliente responde distinto, hay que ajustar el código, pero el cambio está
acotado (se indica dónde). Detalle de cada supuesto en `backend/PROGRESO.md` y `Sprint1/sprint-backlog.md`.

| Tema | Supuesto actual | Si el cliente dice otra cosa |
|---|---|---|
| Revisión de la planificación (S-37) | En secuencia: primero el guía, luego el profesor del curso | Cambia la máquina de estados en `PlanificacionService`; la base y el registro de revisiones sirven igual |
| Privacidad de los chats (S-38, S-60, S-66) | Leen el estudiante, su guía, el jefe de carrera y el coordinador; el estudiante lo sabe y cada lectura queda registrada | Se ajusta en `AccesoService.puedeVerChat` |
| Modelo del chatbot (S-59) | Groq, capa gratuita: uno o dos mensajes por minuto para todo el sistema | Cambiar de plan o de proveedor; la llamada está aislada en `ModeloLenguajeGroq` |
| Transcripción de la nota de voz (S-40) | La hace Chrome (gratis, sin instalar nada) | Cambia solo la captura en el navegador; la API ya recibe el texto |
| Planificación (S-47, S-76) | Enviarla exige un documento adjunto; inicio + desarrollo + cierre = duración | Se quita la regla (servicio y restricción de la base) |
| Duplas y grupos (S-77, S-78) | Cada integrante tiene su propia planificación; dupla = 2, grupo = 2 a 4, mismo guía | Reglas concentradas en `GrupoService` |
| Nota final (S-81, S-83 a S-85, S-92) | 60 % de exigencia (editable de 50 a 70 %); el profesor del curso define los pesos; el coordinador confirma la nota al cerrar | Pesos y exigencia ya son editables; quién los define, en los permisos |
| Panel y parámetros (S-79, S-80) | 3 días para marcar algo como atrasado; rangos de cada parámetro | Se cambian desde «Parámetros del sistema» |
| Documentos (S-43, S-82, S-93 a S-96) | Tamaños y formatos; quién borra o reemplaza; productos del estudiante; archivos oficiales de las pautas | Reglas en cada servicio de documentos |

**Limitaciones conocidas (no son código pendiente):**
- Que el chatbot «responda solo con los documentos de la base de conocimiento» (HU-29, criterio 2) lo cumple el
  modelo por instrucciones. Garantizarlo exigiría otra arquitectura.
- La transcripción solo funciona en Chrome (es una limitación del navegador, S-40).

---

## 5. Deuda técnica: lo resuelto y lo que queda

**Resuelto en el Sprint 2:** límite de intentos de login, Swagger apagado en producción, clave de sesión
obligatoria en producción, carpeta `deploy/` (instalador, servicio, nginx y respaldos), cambio de contraseña
(HU-26), parámetros editables (HU-27) y borrar o reemplazar documentos (HU-21, HU-22).

**Lo que queda** (ninguno impide usar el sistema):

| Tema | Situación actual | Mejora propuesta |
|---|---|---|
| Administración de usuarios | No hay pantalla para editar los datos de un usuario ni para desactivarlo; un coordinador o jefe de carrera nuevo se crea con SQL | Una HU nueva de administración de usuarios **[REVISAR CON EL EQUIPO]** |
| Corrección de errores | No se puede anular una inscripción mal creada ni corregir una evaluación ya finalizada | Definir con el cliente un procedimiento de corrección (quién y con qué registro) |
| Pruebas frágiles | Las pruebas de integración dependen de los datos exactos de `db/seed.sql` | Base de pruebas aislada (por ejemplo, un contenedor MySQL de prueba) |
| Sin integración continua | Las pruebas se corren a mano (`mvn clean package`) | GitHub Actions en cada cambio |
| Front-end sin pruebas automáticas | Se prueba a mano en el navegador | Pruebas de punta a punta de los flujos clave |
| Concurrencia | Si dos personas editan lo mismo a la vez, gana la última | `@Version` en planificación y evaluación |
| Listados sin paginar | Los volúmenes son chicos (S-26) | Paginar si crece el uso |
| Sesión | El token dura 8 horas y no se puede revocar | Token de refresco y cierre de sesión en el servidor |
| Archivos | Se validan por extensión y viven en el disco de un servidor | Revisar el tipo real del archivo; almacenamiento compartido si hay más de un servidor |
| Migraciones a mano | Cada migración se corre una vez, como root y a mano (`DESPLIEGUE.md`) | Una herramienta de migraciones (Flyway) que registre cuáles se aplicaron |
| `docker-compose.yml` (encontrado el 22-nov al probarlo) | Sin `--env-file .env.docker`, MySQL se crea con claves por defecto escritas en el propio compose; el chequeo de salud de MySQL da «sano» mientras aún carga `schema.sql`, y el backend falla una vez al arrancar desde cero | Quitar las claves por defecto (que falle si faltan) y un chequeo de salud que espere el servidor definitivo. Mientras tanto, `DESPLIEGUE.md` sección 5 lo evita con `--env-file` y el arranque en dos pasos **[REVISAR CON EL EQUIPO]** |

---

## 6. Preguntas probables del jurado

**¿Qué les falta?**
Dos historias de 31: exportar a Adecca (HU-13) y avisos por correo (HU-19). Las dos esperan un dato de la
universidad: el formato de Adecca y la configuración del correo. Todo lo demás está construido y probado.

**¿Por qué no construyeron HU-13 y HU-19 de todas formas?**
Porque habría que adivinar el formato de Adecca y la configuración del correo. Preferimos dejar listo todo lo que
depende de nosotros (la nota final ya se calcula y las revisiones ya se registran) y no programar algo que probablemente
habría que rehacer.

**¿Está terminado entonces?**
Está completo en funcionalidades, pero con supuestos por confirmar con el cliente (sección 4). Ninguno bloquea el
uso del sistema; si el cliente responde distinto, el cambio está acotado.

**¿Qué sigue?**
Publicarlo en el servidor (guía paso a paso en `DESPLIEGUE.md`), confirmar los supuestos con el cliente y, cuando
lleguen los datos, construir HU-13 y HU-19.

**¿Cómo saben que funciona?**
373 pruebas automáticas, 45 pruebas negativas de la base y un repaso de los 6 roles en el navegador antes de cada
entrega importante; el último fue el 22-nov.

**¿Qué mejorarían del diseño?**
Pruebas que no dependan de datos exactos, integración continua, control de concurrencia, administración de usuarios
y una herramienta de migraciones (sección 5).

---

## 7. Decisiones pendientes

**Para llevar al cliente o a la profesora:**
- [ ] ¿La planificación se revisa en secuencia o en paralelo? (S-37)
- [ ] ¿Quién puede leer los chats del chatbot? (S-38, S-60, S-66)
- [ ] ¿Quién define los pesos de la nota final y quién la confirma? (S-83, S-84)
- [ ] ¿Qué plan o proveedor del modelo de lenguaje se usará en producción? (S-59)
- [ ] ¿Qué herramienta de transcripción prefiere? (S-40)
- [ ] ¿En dupla, cada integrante entrega su propia planificación? (S-77, S-78)
- [ ] ¿Cómo se corrige una evaluación finalizada o una inscripción mal creada?

**Para pedir a la universidad:**
- [ ] El formato de archivo que acepta Adecca (HU-13).
- [ ] La configuración del correo para avisos automáticos (HU-19).
- [ ] El servidor definitivo y las respuestas de `DESPLIEGUE.md`, sección 6.

**Para decidir entre nosotros:**
- [ ] ¿Se crea una HU de administración de usuarios?
- [ ] ¿Adoptamos integración continua, una base de pruebas aislada y Flyway?
- [ ] ¿Corregimos el `docker-compose.yml` (claves por defecto y chequeo de salud de MySQL)? (sección 5)
- [ ] Hacer la retrospectiva del Sprint 1 (`Sprint1/retrospectiva.md` sigue como plantilla).
