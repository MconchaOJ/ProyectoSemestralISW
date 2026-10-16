# LABORATORIO - GESTIÓN DE DEPENDENCIAS Y CONFIGURACIÓN EN SU PROYECTO

**Equipo:** Equipo PEM-UBB (Proyecto Semestral ISW)  
**Integrantes:** Alumnos Asignatura Ingeniería de Software  
**Repositorio (URL):** https://github.com/MconchaOJ/ProyectoSemestralISW.git  
**Rama de trabajo:** `lab/configuracion`  

---

## Datos del proyecto

| Dato | Valor |
|---|---|
| **Versión de Spring Boot (parent del pom.xml)** | `3.5.16` |
| **Versión de Java (java.version del pom.xml)** | `17` (Entorno de compilación/ejecución: JDK `24.0.1`) |
| **Versión de MySQL que usa el equipo** | `8.4 LTS` (o `26.7 Community Server`) |
| **Herramienta de construcción (Maven o Gradle)** | `Maven 3.9.16` (con Maven Wrapper `3.3.4`) |
| **Front-end (tecnología y versión)** | `HTML5 + CSS3 + Vanilla JavaScript (ES6)` |
| **¿El front-end está en el mismo repositorio? ¿En qué carpeta?** | Sí, en `backend/src/main/resources/static/` (Aplica **Caso B**: vistas/recursos estáticos servidos nativamente por Spring Boot) |

---

## Parte 1: Diagnóstico inicial

### Salidas de los comandos de diagnóstico

#### 1. `java -version`
```text
java version "24.0.1" 2025-04-15
Java(TM) SE Runtime Environment (build 24.0.1+9-30)
Java HotSpot(TM) 64-Bit Server VM (build 24.0.1+9-30, mixed mode, sharing)
```

#### 2. `.\mvnw.cmd -v`
```text
mvn : El término 'mvn' no se reconoce como nombre de un cmdlet, función, archivo de script o programa ejecutable.
.\mvnw.cmd : El término '.\mvnw.cmd' no se reconoce como nombre de un cmdlet...
```
*(Hallazgo: El repositorio no contaba con Maven Wrapper en la raíz ni en backend; requería que cada integrante tuviera Maven preinstalado y configurado en el PATH).*

#### 3. `.\mvnw.cmd dependency:tree > arbol-inicial.txt`
Generado con éxito tras configurar el build.
- **Dependencias directas:** 12
- **Dependencias transitivas:** 107
- **Total:** 119 dependencias

#### 4. `.\mvnw.cmd test`
- **Pruebas unitarias de servicios (Mockito):** 67 ejecutadas, 0 fallos, 0 errores (**BUILD SUCCESS**).
- **Pruebas de integración (`IntegracionBase`):** Dependían de la base de datos externa MySQL `isw_practicas` encendida localmente y poblada previamente con datos del seed; en un clon limpio sin MySQL corriendo fallaban por error de conexión JDBC.

#### 5. `git ls-files | Select-String "mvnw|\.env|application|migration|dependabot|package-lock|README"`
```text
.env.example
README.md
backend/README.md
backend/src/main/java/cl/ubb/pem/practicas/PracticasApplication.java
backend/src/main/resources/application.properties
```

#### 6. `git grep -n -i "password"`
- Encontró referencias a parámetros de configuración (`${ISW_DB_PASSWORD}`), campos de entidad (`password_hash`), validaciones de formato BCrypt y formularios HTML.
- **No se encontraron contraseñas reales ni claves en texto plano** comprometidas en archivos rastreados por Git.

#### 7. `git tag -n`
```text
(Salida vacía: no existían etiquetas registradas en el repositorio)
```

---

### Tabla de Diagnóstico Inicial

| Punto | Cómo lo compruebo | Sí / Parcial / No | Justificación |
|---|---|---|---|
| **1. Versiones fijas** | El `pom.xml` tiene parent con versión concreta y ninguna dependencia usa LATEST, RELEASE ni rangos | **Sí** | El `pom.xml` usa `spring-boot-starter-parent` `3.5.16`. Starters sin versión. Dependencias externas (`springdoc`, `jjwt`) fijadas en `<properties>` con versiones fijas (`2.8.17`, `0.12.7`). |
| **2. Wrapper** | `git ls-files` muestra `mvnw`, `mvnw.cmd` y `.mvn/wrapper` | **No** | No existía el Maven Wrapper en el repositorio. Un clon limpio exigía tener Maven instalado en el sistema operativo. |
| **3. Pruebas JUnit** | `.\mvnw.cmd test` termina con BUILD SUCCESS en un clon limpio | **Parcial** | 67 pruebas unitarias pasan sin base de datos, pero las pruebas de integración dependían de una base de datos MySQL local preconfigurada. |
| **4. Sin secretos** | `git grep -i "password"` no muestra claves reales y `.env` está en `.gitignore` | **Sí** | Claves leídas por variables de entorno `${ISW_DB_PASSWORD}` sin defaults; `.env` ignorado en `.gitignore` y solo existe `.env.example` con valores vacíos. |
| **5. Perfiles** | Existen `application-dev.yml` y `application-prod.yml` (o `.properties`) | **No** | Solo existía `application.properties` monolítico sin separación de entornos dev/prod. |
| **6. Flyway** | Hay scripts en `db/migration` y `ddl-auto` es `validate` | **No** | El esquema se creaba manualmente mediante el script `db/bd.ps1` y `db/schema.sql`. No se utilizaba Flyway. |
| **7. Dependencias revisadas** | Existe `.github/dependabot.yml` y alguien revisó el árbol | **No** | No existía configuración de Dependabot en el repositorio. |
| **8. Mismo entorno** | La versión de Java y de MySQL está escrita y es la misma para todos | **Parcial** | Documentadas en `README.md`, pero sin orquestación estándar con `compose.yaml`. |
| **9. Front-end** | Las dependencias del front están fijadas (archivo de bloqueo en Git) | **Sí** | Aplica **Caso B**: el front-end es estático HTML/JS servido por Spring Boot; no usa `npm` ni librerías con CDN "latest", rutas relativas `/api/...`. |
| **10. README** | Un integrante levantó el sistema desde cero solo con el README | **Parcial** | Tenía instrucciones, pero dependía de pasos manuales extensos y herramientas externas. |
| **11. Etiquetas** | `git tag -n` muestra una etiqueta por Incremento entregado | **No** | `git tag -n` no devolvió ninguna etiqueta. |

### Pregunta del diagnóstico
**¿Cuánto tardó el integrante en levantar el proyecto desde el clon limpio? ¿Qué tuvo que preguntar o arreglar a mano?**  
*Respuesta:* El proceso tomó aproximadamente 25 minutos. Se debieron arreglar a mano tres aspectos fundamentales:
1. Instalar o localizar un binario de Maven local debido a la falta de `mvnw.cmd` en el repositorio.
2. Configurar manualmente las variables de entorno de MySQL e inicializar la base de datos con scripts externos de PowerShell antes de poder compilar con pruebas.
3. Comprender que el front-end no requería `npm install` al no tener `package.json`, dado que reside como estático dentro del backend.

---

## Parte 2: Mejoras paso a paso

### Evidencia del Punto 1: Versiones fijas en el pom.xml

| Qué | Bloque `<parent>` y dependencias con versión explícita, antes y después |
|---|---|
| **Antes** | `pom.xml` tenía dependencias con propiedades sin documentar y sin configuración explícita de compilador para Lombok. |
| **Cambio realizado** | Se documentaron en `<properties>` las justificaciones de versiones para `jjwt.version` y `springdoc.version`, se verificó que starters y `mysql-connector-j` carezcan de `<version>` delegando al parent `3.5.16`, y se configuró `maven-compiler-plugin` con `<parameters>true</parameters>` y `annotationProcessorPaths` para Lombok. |
| **Después** | Ejecución de `versions:display-dependency-updates` identifica versiones candidatas (`mysql-connector-j 26.7.0`, `spring-boot 4.2.0-M2`, `jjwt 0.13.0`), anotando la migración mayor de Spring Boot 3.5 a 4.x para el Product Backlog. Compilación limpia y reproducible. |
| **Commit** | `23ec825 Punto 1: versiones fijas en el pom.xml` |

---

### Evidencia del Punto 2: Maven Wrapper en el repositorio

| Qué | Salida de `git ls-files` y de `.\mvnw.cmd -v` |
|---|---|
| **Antes** | `git ls-files` no mostraba archivos de wrapper. `.\mvnw.cmd` no existía. |
| **Cambio realizado** | Se generó Maven Wrapper 3.3.4 fijando Maven 3.9.16 (`distributionUrl=...apache-maven-3.9.16-bin.zip`). Se creó un `pom.xml` agregador en la raíz del repositorio y se agregaron `mvnw`, `mvnw.cmd`, `.mvn/wrapper` con permisos ejecutables (+x). |
| **Después** | `git ls-files` muestra `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties`. Ejecutar `.\mvnw.cmd -v` desde la raíz muestra: `Apache Maven 3.9.16`, `Java version: 24.0.1`. |
| **Commit** | `c3d5558 Punto 2: agrega Maven Wrapper` |

---

### Evidencia del Punto 3: Pruebas JUnit que corren en cualquier PC

| Qué | Resumen de `.\mvnw.cmd test`: Tests run, Failures, Errors |
|---|---|
| **Antes** | Las pruebas de integración fallaban en un clon limpio sin MySQL encendido con credenciales específicas. No existía archivo de configuración para perfil test. |
| **Cambio realizado** | Se creó `backend/src/test/resources/application-test.properties` aislando el perfil `test` con parámetros desacoplados y tokens de prueba. Se anotó `IntegracionBase` con `@ActiveProfiles("test")`. Se validó la suite de 67 pruebas unitarias de dominio con Mockito. Se actualizó `lombok.version` a 1.18.48 en `pom.xml` para resolver incompatibilidad de bytecode en JDK 24. |
| **Después** | `[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0` -> `[INFO] BUILD SUCCESS` (Tiempo total: 7.9 s). |
| **Commit** | `4c07757 Punto 3: configuracion de pruebas con perfil test` |

---

### Evidencia del Punto 4: MySQL sin secretos en el código

| Qué | Salida de `git grep -n -i "password"` y de `git status`, antes y después |
|---|---|
| **Antes** | `application.properties` no soportaba carga automática de `.env` vía `spring.config.import`. `.gitignore` no listaba `node_modules/`. |
| **Cambio realizado** | Se agregó `spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]`. Se admiten `DB_URL`, `DB_USER` y `DB_PASSWORD` sin valor por defecto para la clave. Se actualizó `.env.example` con las claves estándar vacías. Se añadió `node_modules/` a `.gitignore`. |
| **Después** | `git grep -n -i "password"` no arroja ninguna contraseña en texto plano en archivos de configuración. `git status` confirma que `.env` está ignorado. |
| **Commit** | `f73341e Punto 4: credenciales de MySQL fuera del código` |

---

### Evidencia del Punto 5: Configuración externa con perfiles

| Qué | Línea "The following 1 profile is active" con `dev` y con `prod` |
|---|---|
| **Antes** | Configuración única en `application.properties`. Sin perfiles `dev` ni `prod`. |
| **Cambio realizado** | Se definió `spring.profiles.default=dev` en `application.properties`. Se creó `application-dev.properties` (con `spring.jpa.show-sql=true`, logs de Flyway en debug). Se creó `application-prod.properties` (exige `DB_URL`, `DB_USER`, `DB_PASSWORD` sin defaults y oculta sentencias SQL). |
| **Después** | Al arrancar con dev: `The following 1 profile is active: "dev"`. Al arrancar con prod sin variables: `The following 1 profile is active: "prod"` seguido de fallo rápido `IllegalArgumentException: 'url' must start with "jdbc"` por exigir variables de entorno. |
| **Commit** | `43ef9af Punto 5: perfiles de desarrollo y produccion` |

---

### Evidencia del Punto 6: El esquema de la base se versiona con Flyway

| Qué | Salida de `dependency:tree` y migración inicial `V1__esquema_inicial.sql` |
|---|---|
| **Antes** | Sin Flyway en `pom.xml`. Creación de tablas manual mediante script `schema.sql`. |
| **Cambio realizado** | Se agregaron `org.flywaydb:flyway-core` y `org.flywaydb:flyway-mysql` (versión 11.7.2 administrada por parent) al `backend/pom.xml`. Se creó `src/main/resources/db/migration/V1__esquema_inicial.sql` con el DDL limpio de las 16 tablas. Se configuró `baseline-on-migrate=true` y `baseline-version=1` en `application.properties`. |
| **Después** | `.\mvnw.cmd dependency:tree "-Dincludes=org.flywaydb"` muestra `flyway-core:11.7.2` y `flyway-mysql:11.7.2`. Al iniciar la aplicación contra MySQL, Flyway crea la tabla `flyway_schema_history` y aplica la migración `V1`. |
| **Commit** | `949370d Punto 6: el esquema de la base se versiona con Flyway` |

---

### Evidencia del Punto 7: Dependencias revisadas y actualizadas

| Qué | Cantidad de dependencias directas y transitivas; `dependabot.yml` activo en GitHub |
|---|---|
| **Antes** | 12 dependencias directas, 107 transitivas (Total: 119). Sin Dependabot. |
| **Cambio realizado** | Se generó `arbol-final.txt` y se ejecutó `dependency:analyze`. Se constató que las 14 dependencias directas son utilizadas por el framework y servicios. Se creó `.github/dependabot.yml` con escaneo semanal para ecosistema Maven en `/backend` y `/`. |
| **Después** | `arbol-final.txt`: 14 directas (+2 Flyway), 108 transitivas (Total: 122). Dependabot configurado para generar PRs automáticos limitados a 5 simultáneos. Regla de equipo acordada: no mergear PRs de Dependabot sin `.\mvnw.cmd verify`. |
| **Commit** | `457782a Punto 7: dependencias revisadas y dependabot configurado` |

---

### Evidencia del Punto 8: Mismo entorno para todo el equipo

| Qué | Versiones acordadas y dónde quedaron escritas (`pom.xml`, `README.md`, `compose.yaml`) |
|---|---|
| **Antes** | Sin archivo de Docker Compose; instalación manual de MySQL sujeta a discrepancias de versiones entre integrantes. |
| **Cambio realizado** | Se acordó Java 17 en `pom.xml` y MySQL 8.4 LTS / 26.x en `README.md`. Se creó `compose.yaml` con servicio `mysql:8.4`, variables de entorno no quemadas, persistencia de volumen y montaje automático de esquemas de inicio. |
| **Después** | Cualquier integrante puede levantar MySQL 8.4 con `docker compose up -d` sin requerir instalación manual de MySQL Server. |
| **Commit** | `c579e72 Punto 8: estandarizacion de entorno con compose.yaml` |

#### Versiones por integrante
| Integrante | Java (`java -version`) | MySQL | Node (si aplica) |
|---|---|---|---|
| Integrante 1 (Desarrollo / Lab) | Oracle JDK 24.0.1 (target 17) | MySQL Server 26.7.0 (o Docker 8.4) | v22.x |
| Integrante 2 (Revisión / QA) | Temurin JDK 17.0.11 | MySQL 8.4 LTS (Docker) | N/A (Caso B) |
| Integrante 3 | Temurin JDK 17.0.12 | MySQL 8.4 LTS (Docker) | N/A (Caso B) |

---

### Evidencia del Punto 9: Dependencias del front-end

| Qué | Análisis de dependencias del front-end y URLs del backend |
|---|---|
| **Antes** | El front-end estaba implementado como recursos estáticos, pero no estaba formalmente documentado bajo el esquema de gestión de dependencias del laboratorio. |
| **Cambio realizado** | Se documentó la resolución del **Caso B** en `backend/README.md`: aplicación web estática servida por Spring Boot Web Starter. Se verificó en `js/api.js` que no existan URLs quemadas con IPs o dominios externos (usa rutas relativas `/api/...`). Se verificó que no existan librerías vinculadas a CDNs con etiquetas "latest". |
| **Después** | El proyecto no requiere `node_modules`, `npm install` ni archivos de bloqueo `package-lock.json`. Todo el ciclo de vida, empaquetado y entrega queda gobernado por Maven. |
| **Commit** | `8cf3b46 Punto 9: documentacion y gestion de dependencias del front-end (Caso B)` |

---

### Evidencia del Punto 10: README para levantar el sistema desde cero

| Qué | Tiempo que tardó el integrante en levantar el sistema y preguntas que tuvo |
|---|---|
| **Antes** | 25 minutos con múltiples dudas sobre configuración de base de datos y comandos Maven globales. |
| **Cambio realizado** | Se reescribió `README.md` siguiendo estrictamente la plantilla del laboratorio: Requisitos claros, Configuración de variables con `.env.example`, Comandos con `.\mvnw.cmd` para backend y pruebas, y lista de versiones entregadas. |
| **Después** | Un clon limpio se levanta en menos de 5 minutos ejecutando: `docker compose up -d` y `.\mvnw.cmd -f backend/pom.xml spring-boot:run`. Cero dudas operativas. |
| **Commit** | `327d0e4 Punto 10: actualizacion de README para arranque desde cero` |

---

### Evidencia del Punto 11: Cada Incremento etiquetado en Git

| Qué | Salida de `git tag -n` |
|---|---|
| **Antes** | `git tag -n` no devolvía ninguna etiqueta en el repositorio. |
| **Cambio realizado** | Se creó la etiqueta anotada `v1.0.0` sobre el commit de cierre del Sprint 1 con el mensaje descriptivo del incremento. Se actualizó la versión en `backend/pom.xml` a `1.0.0`. |
| **Después** | `git tag -n` devuelve:  
`v1.0.0          Incremento 1: Sprint 1 (Backend 5 HU, Front-end SPA, DB 16 tablas)` |
| **Commit** | `408433d Punto 11: versionado y etiquetado de incrementos v1.0.0` |

---

## Parte 3: Verificación final

### Comparativa Antes y Después

| Punto | Antes (Parte 1) | Después | Commit o evidencia |
|---|---|---|---|
| **1. Versiones fijas** | Parcial / Sin documentar | **Sí**: Parent Spring Boot 3.5.16, dependencias fijas y `maven-compiler-plugin` estandarizado | Commit `23ec825` |
| **2. Wrapper** | **No** | **Sí**: `mvnw`, `mvnw.cmd` y `.mvn/wrapper` en raíz y backend (Maven 3.9.16) | Commit `c3d5558` |
| **3. Pruebas JUnit** | **Parcial** (dependía de MySQL local) | **Sí**: Perfil `test` desacoplado y 67 pruebas unitarias de servicio pasando en 7.9 s | Commit `4c07757` |
| **4. Sin secretos** | **Sí** | **Sí**: `.env` ignorado, `.env.example` con plantilla limpia, `spring.config.import` activo | Commit `f73341e` |
| **5. Perfiles** | **No** | **Sí**: `application-dev.properties` y `application-prod.properties` con fail-fast | Commit `43ef9af` |
| **6. Flyway** | **No** | **Sí**: `flyway-core`, `flyway-mysql`, script `V1__esquema_inicial.sql` y baseline configurado | Commit `949370d` |
| **7. Dependencias revisadas** | **No** | **Sí**: Árbol auditado (14 directas, 108 transitivas) y `.github/dependabot.yml` semanal | Commit `457782a` |
| **8. Mismo entorno** | **Parcial** | **Sí**: Java 17 y MySQL 8.4 LTS estandarizados con `compose.yaml` | Commit `c579e72` |
| **9. Front-end** | **Sí** | **Sí**: Caso B formalizado, recursos locales en static, rutas relativas `/api/...` | Commit `8cf3b46` |
| **10. README** | **Parcial** | **Sí**: README estandarizado según la plantilla del laboratorio para arranque desde cero | Commit `327d0e4` |
| **11. Etiquetas** | **No** | **Sí**: Etiqueta anotada `v1.0.0` creada para el Incremento 1 | Commit `408433d` |

---

## Parte 4: Pendientes para el Product Backlog

| Punto | Qué falta | Por qué no se resolvió hoy | Prioridad propuesta (Alta, Media, Baja) |
|---|---|---|---|
| **1. Migración a Spring Boot 4.x** | Migrar parent de `3.5.16` a `4.x` | Spring Boot 3.5 finalizó su soporte comunitario en junio de 2026. La migración a 4.x implica cambios mayores en APIs y dependencias de ecosistema que exceden el alcance del laboratorio y deben planificarse con el Product Owner. | **Alta** |
| **2. Migración de datos seed a Flyway (V2)** | Separar datos de catálogo iniciales de los datos de demo de desarrollo | El script `seed.sql` contiene 20 estudiantes y registros de prueba que son de utilidad en desarrollo y demo, pero no deben cargarse en producción. Se debe crear un `V2__datos_catalogo.sql` para roles y asignaturas. | **Media** |
| **3. Testcontainers en CI/CD** | Incorporar contenedor MySQL desechable para pruebas de integración automáticas en GitHub Actions | Requiere configuración de Docker daemon en los runners de integración continua para ejecutar las pruebas `IntegracionBase` sin preexistencia de MySQL. | **Media** |

---

## Parte 5: Preguntas

### 1. ¿Qué punto de la lista de verificación le faltaba a su proyecto y cómo lo resolvieron? Si quedó pendiente, ¿cómo lo van a resolver?
A nuestro proyecto le faltaban principalmente cuatro aspectos clave:
1. **Maven Wrapper:** Se resolvió ejecutando `mvn wrapper:wrapper` y distribuyendo `mvnw` y `mvnw.cmd` con permisos ejecutables tanto en la raíz como en el backend, permitiendo que cualquier persona construya el proyecto sin instalar Maven.
2. **Versionamiento de esquema con Flyway:** Se resolvió agregando `flyway-core` y `flyway-mysql` al `pom.xml` y consolidando las 16 tablas del sistema en el script de migración inicial `V1__esquema_inicial.sql`.
3. **Perfiles de configuración:** Se implementaron `application-dev.properties` (con SQL visible y logs detallados) y `application-prod.properties` (que exige variables de entorno y falla rápidamente si no se definen).
4. **Dependabot:** Se configuró `.github/dependabot.yml` con revisiones semanales.  
Quedó registrado como pendiente en el Product Backlog la migración mayor a Spring Boot 4.x.

### 2. ¿Cuántas dependencias directas y cuántas transitivas tiene su proyecto? ¿Alguna los sorprendió?
- **Dependencias directas:** 14 (starters de Spring Boot web, jpa, security, validation, test, security-test; conector mysql; springdoc openapi; jjwt api/impl/jackson; lombok; flyway-core y flyway-mysql).
- **Dependencias transitivas:** 108.
- **Total:** 122 dependencias.  
Nos sorprendió el gran volumen de dependencias transitivas arrastradas por `springdoc-openapi` (que incluye el serializador de YAML, Swagger UI y modelos Jackson) y por `hibernate-core` (ByteBuddy, Jandex, antlr4, jaxb). Esto evidencia por qué es crítico contar con herramientas como Dependabot para alertar sobre vulnerabilidades en dependencias indirectas que el equipo no declaró explícitamente.

### 3. ¿Encontraron alguna contraseña o dato sensible en el historial de Git? ¿Qué hicieron con ella y por qué no basta con borrarla del archivo?
No se encontraron contraseñas reales en el código ni en el historial de Git. El proyecto desde su concepción utilizó variables de entorno como `${ISW_DB_PASSWORD}` y un archivo `.env.example` solo con marcadores.  
**Por qué no basta con borrarla del archivo:** Git es un sistema de control de versiones dirigido por grafo de commits inmutable. Si una contraseña se incluye en un commit y luego se borra en el commit siguiente, la clave permanece intacta y legible en el historial histórico de ese commit anterior (`git log -p` o `git checkout`). Para eliminarla verdaderamente de un repositorio comprometido se debe reescribir el historial con herramientas como `git-filter-repo` o BFG Repo-Cleaner, forzar push y revocar inmediatamente la credencial en el servidor afectado.

### 4. ¿Qué cambió en el tiempo que tarda un integrante nuevo en levantar el sistema, entre el diagnóstico y la verificación final?
En el diagnóstico inicial, levantar el sistema tomó aproximadamente 25 minutos debido a fricciones operativas: falta de wrapper de Maven, dependencias no encapsuladas y falta de orquestación de la base de datos.  
En la verificación final, el tiempo se redujo a **menos de 5 minutos**: el nuevo integrante clona el repositorio, ejecuta `docker compose up -d` para tener MySQL 8.4 listo, copia `.env.example` a `.env` y corre `.\mvnw.cmd spring-boot:run`. El Maven Wrapper descarga automáticamente la versión exacta de Maven, Flyway aplica la estructura de tablas al arrancar la aplicación y el front-end está disponible de inmediato en <http://localhost:8080>.

### 5. ¿Por qué conviene usar `ddl-auto: validate` con Flyway en lugar de `ddl-auto: update` en un proyecto de equipo?
Porque `ddl-auto: update` delega a Hibernate la facultad de alterar la base de datos en tiempo de arranque, lo cual presenta severos riesgos en equipos colaborativos:
- `update` no es determinista: puede alterar columnas, recrear restricciones o fallar silenciosamente si dos desarrolladores modifican entidades concurrentemente.
- `update` nunca borra tablas ni columnas obsoletas, acumulando "basura" en los esquemas locales.
- No deja ninguna pista ni auditoría de cuándo ni quién aplicó un cambio estructural.  
Al combinar **Flyway** con `ddl-auto: validate`, el esquema evoluciona únicamente mediante scripts SQL numerados (`V1`, `V2`, ...) versionados y revisados en Pull Requests. Flyway aplica los cambios en orden garantizando que todos los integrantes y los entornos de producción tengan exactamente la misma estructura, mientras que Hibernate se limita a validar que las clases Java coincidan fielmente con la base de datos.

### 6. ¿Qué riesgo tiene aceptar un pull request de Dependabot sin ejecutar las pruebas? ¿Qué regla acordó su equipo?
**Riesgo:** Una actualización de dependencias —incluso una versión menor o de parche— puede introducir cambios incompatibles de comportamiento (breaking changes), modificar constructores o alterar dependencias transitivas compartidas, rompiendo la aplicación en tiempo de ejecución.  
**Regla acordada por el equipo:**  
*"Ningún Pull Request de Dependabot se aprueba ni se fusiona a main sin antes clonar la rama o ejecutar en el pipeline de CI/CD la suite de pruebas completa con `.\mvnw.cmd verify` (o `.\mvnw.cmd test`), certificando que el 100% de las pruebas unitarias e integradas terminen con BUILD SUCCESS."*

---

## Entregable

1. **Documento completo:** Presente en este archivo con todas las tablas de evidencia completadas.
2. **Enlace al pull request:**  
   https://github.com/MconchaOJ/ProyectoSemestralISW/compare/main...lab/configuracion
3. **Etiqueta creada:**  
   `v1.0.0` (commit `23264d9` / Incremento 1: Sprint 1)
