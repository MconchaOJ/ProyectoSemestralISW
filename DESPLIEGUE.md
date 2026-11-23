# Despliegue en el servidor

**Sistema de Formación Práctica PEM-UBB** · Actualizado al **22 de noviembre de 2026**

> **Para quién es:** para quien suba el sistema a un servidor. La **sección 5** es la guía paso a paso para
> **actualizar el servidor de demostración de Alej** (Podman rootless con SELinux, puertos 8017 y 8018). Una
> instalación con `deploy/install.sh` (el servidor definitivo de la UBB) está en `deploy/GUIA_DESPLIEGUE_EQUIPO.md`.
> Lo marcado **[PREGUNTAR A TI]** depende del área de informática de la universidad; **[DECIDIR]**, del equipo.

---

## 1. ¿Cuántos repositorios necesitamos?

**Uno solo: el que ya tenemos** (`main` en GitHub). No hace falta separarlo en "front-end" y "backend":

- El front-end (HTML, CSS y JS) vive dentro del backend (`backend/src/main/resources/static/`) y se empaqueta en
  el mismo `.jar`. Un solo programa sirve la página **y** la API.
- Los scripts de la base de datos (`db/`) y de instalación (`deploy/`) también están en el mismo repositorio.

**Lo que nunca va al repositorio:** las claves (variables de entorno), los documentos y audios subidos por los
usuarios (carpeta de adjuntos en el servidor) y el `.jar` compilado (se genera con Maven).

## 2. Qué piezas corren en el servidor

```
Navegador (los 6 roles)
      │  https://<subdominio>.ubiobio.cl   (puerto 443)
      ▼
Nginx  ── proxy inverso: certificado HTTPS, límite de subida de 30 MB
      │  http://127.0.0.1:8080   (solo dentro del servidor)
      ▼
Aplicación Spring Boot (el .jar con Java 17), servicio systemd que arranca solo y se reinicia si cae
      │                                   │                         │
      ▼  127.0.0.1:3306                   ▼                         ▼ (solo si se usa el chatbot)
MySQL 8.4 (base isw_practicas,        Carpeta de adjuntos        Groq por internet (HTTPS saliente)
usuario isw_app)                       (documentos, audios, productos, archivos oficiales de pautas)
      │                                   │
      └──────────── respaldo diario ──────┘
```

## 3. Lo que tiene que entregar la universidad y lo que instalamos

### 3.1 La universidad

| # | Qué | Para qué | Detalle |
|---|---|---|---|
| 1 | **Un servidor (máquina virtual)** | Donde corre todo | Linux (Ubuntu o Debian). 2 CPU, 2 a 4 GB de RAM y 20 GB de disco o más (SUPUESTO: volumen chico) |
| 2 | **Acceso remoto** | Instalar y actualizar | SSH con un usuario con `sudo`. Si solo se entra desde la red de la UBB, también la VPN **[PREGUNTAR A TI]** |
| 3 | **Un subdominio** | La dirección de la página | Por ejemplo `practicas-pem.ubiobio.cl`. Debe ser un subdominio propio (en la raíz `/`), no una subcarpeta, porque el front-end llama a la API con rutas absolutas (`/api/...`) |
| 4 | **Certificado HTTPS** | Seguridad y micrófono | **Obligatorio:** el navegador solo deja usar el micrófono (nota de voz) en HTTPS, y el login envía contraseñas. Certificado de la universidad o Let's Encrypt (requiere el puerto 80 abierto) |
| 5 | **Puertos** | Que se pueda entrar | Abrir 443 (y 80 para redirigir a HTTPS). **Cerrados hacia afuera** el 8080 (la app) y el 3306 (MySQL) |
| 6 | **Base de datos** | Guardar la información | **MySQL 8.4 LTS** (mínimo técnico 8.0.16; MySQL 5.7 y MariaDB no sirven). Opción A: la instalamos en el mismo servidor. Opción B: la da TI |
| 7 | **Respaldos** | No perder datos | ¿TI respalda la máquina o lo hacemos nosotros? ¿Dónde se guardan las copias? **[PREGUNTAR A TI]** |
| 8 | **Salida a internet (HTTPS)** | Chatbot (HU-15) | El modelo se consulta en Groq. Sin salida, el chatbot responde 503 y el resto funciona |
| 9 | **Correo (SMTP)** | Avisos por correo (HU-19) | Es lo que bloquea HU-19 (ver `objetivosAFUTURO.md`, sección 3) |

### 3.2 Lo que instala `deploy/install.sh`

Java 17, MySQL 8.4 (opción A), nginx, el servicio `systemd` `isw-practicas`, las carpetas
(`/opt/isw-practicas`, `/var/lib/isw-practicas/adjuntos`, `/var/log/isw-practicas`), el archivo de claves
`/etc/isw-practicas/isw-practicas.env` (solo legible por root y el servicio) y el respaldo diario (03:15).

## 4. Estado de los cambios para producción

Todos los cambios que pedía la versión de octubre de este documento **están hechos**:

| Cambio | Estado |
|---|---|
| Datos iniciales de producción (`db/datos-iniciales.sql`): catálogo completo y un solo coordinador, sin usuarios de demo | ✅ (13 pautas, parámetros y estructura evaluativa incluidos) |
| HU-26 cambiar la contraseña | ✅ |
| Instalador para Linux (`deploy/install.sh`), servicio, nginx y respaldo | ✅ probado en Ubuntu (WSL) el 11-oct |
| Perfil `prod` (Swagger apagado, adjuntos y logs con ruta absoluta, detrás de nginx) | ✅ `application-prod.properties` |
| `ISW_JWT_SECRET` obligatorio en producción | ✅ |
| Límite de intentos de login | ✅ |
| **[DECIDIR]** ¿Un servidor de demostración (con datos ficticios) además del real? | Pendiente: los datos reales de estudiantes no se deben mezclar con los de prueba |

---

## 5. Actualizar el servidor de Alej (Podman rootless) — guía paso a paso

> **Para quién es:** el servidor de demostración de Alej, que corre con **Podman 5.8.2 rootless** (sin sudo),
> **podman-compose 1.5.0** y **SELinux**. Front-end (nginx) en el puerto **8017**, API en el **8018**; MySQL no expone
> puerto. Esa máquina ya tiene ajustados los nombres de imagen completos (`docker.io/library/...`), `:ro,Z` en los
> volúmenes que montan archivos del proyecto y saltos de línea LF en `docker-compose.yml`, `nginx-frontend.conf`,
> `backend/Dockerfile` y `.env.docker.example`. El servidor definitivo de la UBB (con `deploy/install.sh`) es otro
> camino: `deploy/GUIA_DESPLIEGUE_EQUIPO.md`.
>
> **Qué hay hoy en ese servidor:** solo los datos de demostración del 28-oct (nada real que cuidar). Por eso la ruta
> recomendada es la **instalación limpia (5.2)**: se borra el volumen de la base y MySQL vuelve a cargar `schema.sql`
> y `seed.sql` actuales desde cero. No hay que aplicar migraciones una por una. La alternativa (conservar la base y
> migrarla) está en el 5.6 y es más larga y delicada.
>
> **Qué se probó (22-nov-2026):** el mismo `docker-compose.yml`, de punta a punta, con **Docker Compose** (Docker
> Desktop 28.5, Windows): instalación limpia, 28 tablas, 13 pautas, 269 indicadores, login por nginx en el 8017,
> pantallas en Chrome, subida de un archivo al volumen de adjuntos, y que la base y los adjuntos sobreviven a un
> `down` + `up`. Los comandos SQL de la alternativa (5.6) se probaron sobre una copia de la base de Alej del 28-oct.
> **No se probó:** Podman, podman-compose ni SELinux (no se pueden replicar en este equipo). Si algo se comporta
> distinto en el servidor, anota el mensaje exacto y avisa.

### 5.1 Tres cosas que hay que saber antes de empezar

1. **Usa siempre `--env-file .env.docker`** en cada comando `podman-compose`. El `docker-compose.yml` arma las
   claves de MySQL con `${ISW_DB_ROOT_PASSWORD:-rootdev2026}` y `${ISW_DB_PASSWORD:-devpass2026}`: si no se le pasa
   `--env-file`, **usa esas claves por defecto (que están en git)** e ignora las de tu `.env.docker` (comprobado con
   Docker Compose; podman-compose tiene la misma opción `--env-file`). Por eso, en este documento:
   ```bash
   alias pc='podman-compose --env-file .env.docker'     # válido solo en la terminal donde lo escribas
   ```
   y todos los comandos dicen `pc ...`. Si abres otra terminal, vuelve a escribir el alias.

2. **Para entrar a MySQL como root no escribas la clave**: usa la que el contenedor ya tiene guardada.
   ```bash
   podman exec -i isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 isw_practicas' < archivo.sql
   ```
   La forma `podman exec -i isw-mysql mysql -uroot -p isw_practicas < archivo.sql` **no sirve**: sin terminal,
   `mysql` toma la primera línea del archivo como clave y responde `Access denied` (comprobado con Docker).
   Las comillas simples importan: así `$MYSQL_ROOT_PASSWORD` se lee **dentro** del contenedor.

3. **Sin «linger», los contenedores no se quedan corriendo solos.** En Podman rootless, si cierras tu sesión o el
   servidor se reinicia, los contenedores pueden detenerse (y `restart: unless-stopped` no los levanta tras un
   reinicio). Compruébalo con `loginctl show-user "$USER" --property=Linger`; activarlo (`loginctl enable-linger`)
   normalmente requiere un administrador. **Para la demo del miércoles:** levanta los contenedores ese mismo día,
   un rato antes (paso 5.4), revisa el 5.5 y no dependas de que sigan arriba desde el día anterior. Si puedes,
   deja la sesión abierta durante la demo (por ejemplo dentro de `tmux`).

### 5.2 Instalación limpia (RECOMENDADA): traer el código y borrar la base de demo

Todo desde la carpeta donde está clonado el repositorio:

```bash
cd ~/isw                     # o como se llame tu carpeta
git status                   # deben aparecer solo tus ajustes locales (compose, Dockerfile, nginx, LF)
git pull
ls db/migraciones            # deben aparecer de la 001 a la 020
grep -c "^CREATE TABLE" db/schema.sql     # debe decir 28
git diff --stat              # tus ajustes locales siguen ahí (los archivos de Docker no cambiaron desde el 28-oct)
```

Si `git pull` dice `Your local changes ... would be overwritten`, haz `git stash`, `git pull` y `git stash pop`.

Revisa tu `.env.docker` **sin mostrar las claves**:

```bash
grep -c '^ISW_DB_ROOT_PASSWORD=.\+' .env.docker          # 1
grep -c '^ISW_DB_PASSWORD=.\+' .env.docker               # 1
grep -c -E 'rootdev2026|devpass2026' .env.docker         # 0 (si da 1 o más, cambia esas claves por otras tuyas)
grep -c '^ISW_JWT_SECRET=.\{32,\}' .env.docker           # 1 recomendado (si da 0, cada reinicio cierra las sesiones)
```

Para crear un secreto JWT: `openssl rand -hex 32` y pégalo en `ISW_JWT_SECRET=` editando el archivo. Si quieres el
chatbot, agrega también una línea `ISW_GROQ_API_KEY=<clave>` (el servidor necesita salida HTTPS a internet; sin la
clave la app funciona igual y el chatbot responde 503).

*(Opcional)* respaldo de la base de demo actual, por si quieres mirar algo después:

```bash
podman exec isw-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction isw_practicas' > respaldo_demo_$(date +%F).sql
```

Detén todo y borra los volúmenes (la base de demo y sus adjuntos del 28-oct):

```bash
pc down
podman volume ls                                  # deben aparecer isw-mysql-data e isw-adjuntos-data
podman volume rm isw-mysql-data isw-adjuntos-data
```

Si `podman volume ls` muestra los nombres con otro prefijo, usa los que muestre. Se borra también el de adjuntos
porque `seed.sql` no trae archivos: los del 28-oct quedarían huérfanos.

### 5.3 Construir la imagen del backend

```bash
pc build backend         # compila con Maven dentro del contenedor; la primera vez tarda varios minutos
```

### 5.4 Levantar: primero la base, después el resto

Se hace en dos pasos a propósito. Mientras MySQL carga `schema.sql` y `seed.sql` usa un servidor temporal que ya
responde al chequeo de salud, así que el backend arranca antes de tiempo, falla y se reinicia; con `up` de una vez,
el front-end puede quedar sin crear (pasó en la prueba con Docker).

```bash
pc up -d database
until podman logs isw-mysql 2>&1 | grep -q "port: 3306 "; do sleep 5; done; echo "MySQL listo"
podman logs isw-mysql 2>&1 | grep -E "running /docker-entrypoint|ERROR"
```

La última línea debe mostrar `01-schema.sql` y `02-seed.sql` y **ningún `ERROR`**. (Si el `until` no termina en
3 minutos, córtalo con Ctrl+C y mira `podman logs isw-mysql | tail -40`.)

```bash
pc up -d
podman ps --format '{{.Names}} {{.Status}}'      # isw-mysql (healthy), isw-backend (healthy), isw-frontend Up
```

Si `isw-backend` dice `starting`, espera un minuto y vuelve a mirar. Si `isw-frontend` no aparece, ejecuta
`pc up -d` otra vez.

### 5.5 Comprobar

**La base** (pega el bloque completo):

```bash
podman exec isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N isw_practicas -e "
 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = \"isw_practicas\";
 SELECT COUNT(*) FROM pauta; SELECT COUNT(*) FROM indicador; SELECT COUNT(*) FROM parametro_sistema;
 SELECT COUNT(*) FROM componente_evaluativo; SELECT COUNT(*) FROM usuario;" 2>/dev/null'
```

Debe dar, en orden: **28, 13, 269, 5, 8, 33**.

**Que la base no se creó con las claves por defecto** (no muestra la clave):

```bash
podman exec isw-mysql sh -c 'case "$MYSQL_ROOT_PASSWORD" in rootdev2026) echo "MAL: clave por defecto";; *) echo "bien";; esac'
```

Si dice `MAL`, faltó el `--env-file`: vuelve al 5.2 (`pc down`, borrar volúmenes, 5.4).

**El log del backend:**

```bash
podman logs --tail 50 isw-backend | grep -E "Started PracticasApplication|Schema-validation|ERROR"
```

Debe aparecer `Started PracticasApplication` y no `Schema-validation`.

**En el navegador**, `http://<servidor>:8017`, con `coordinador@demo.example` (la clave de demo está al inicio de
`db/seed.sql`):
1. «Pautas de evaluación»: 13 pautas; abre una y súbele un PDF en «Archivo oficial».
2. «Parámetros del sistema»: 5 parámetros (incluida la exigencia de la escala de notas, 60 %).
3. «Notas finales» de Práctica Pedagógica: la estructura con 8 componentes.
4. «Informes por asignatura»: descarga el PDF y el Excel.
5. Con un estudiante (por ejemplo `camila.rojas@demo.example`): «Historial», sección «Informes y productos».

**Nota de voz en la demo:** el micrófono del navegador solo funciona con HTTPS o en `localhost`. En
`http://<servidor>:8017` Chrome no deja grabar. Si quieres mostrarla, en el computador de la demo abre
`chrome://flags/#unsafely-treat-insecure-origin-as-secure`, agrega `http://<servidor>:8017`, actívalo y reinicia
Chrome. Si no, muestra una nota de voz ya grabada.

### 5.6 Alternativa (NO recomendada): conservar la base de demo y migrarla

Solo si decides **no** borrar el volumen. Es más larga, y la base queda con tablas sobrantes de tu código del 28-oct.

1. **Código y `.env.docker`:** como en el 5.2, pero **sin** `pc down` ni `podman volume rm`.
2. **Detén la app** (la base sigue arriba): `pc stop backend frontend`.
3. **Respaldo obligatorio** (las migraciones no se pueden deshacer):
   ```bash
   podman exec isw-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers isw_practicas' > respaldo_$(date +%F_%H%M).sql
   tail -n 1 respaldo_*.sql          # debe decir "-- Dump completed on ..."
   ```
4. **Diagnóstico.** Las migraciones no se registran en ninguna tabla y los números no sirven (tu código del 28-oct
   tenía otras «012» y «013», que se descartaron). Guarda esta consulta como `diagnostico.sql` y ejecútala con la
   forma del 5.1 (punto 2):
   ```sql
   SELECT m.migracion, IF(m.hecha, 'SI', 'NO') AS aplicada
   FROM (
     SELECT '011' AS migracion, EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'respuesta_sus') AS hecha
     UNION ALL SELECT '012', EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = 'isw_practicas' AND table_name = 'planificacion' AND column_name = 'inicio_minutos')
     UNION ALL SELECT '013', EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'grupo_practica')
     UNION ALL SELECT '014', EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'parametro_sistema')
     UNION ALL SELECT '015', EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'componente_evaluativo')
     UNION ALL SELECT '018', EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = 'isw_practicas' AND table_name = 'lectura_chat' AND column_name = 'rol')
     UNION ALL SELECT '019', EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'producto_practica')
     UNION ALL SELECT '020', EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema = 'isw_practicas' AND table_name = 'archivo_oficial_pauta')
   ) m ORDER BY m.migracion;
   SELECT id, nombre FROM pauta ORDER BY id;
   ```
   ```bash
   podman exec -i isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 isw_practicas' < diagnostico.sql
   ```
   Lo esperado en tu base del 28-oct: 011 = `SI`, de la 012 en adelante = `NO`, y pautas con ids 1, 2 y 3. Si da
   otra cosa, **detente y avisa**.
5. **Las pautas 4 a 13** (no vienen en ninguna migración; sin ellas la 015 falla). Solo si el diagnóstico mostró
   únicamente los ids 1 a 3:
   ```bash
   { echo "START TRANSACTION;"; sed -n '/^-- HU-07 (2026-11-21)/,/^-- Coordinador inicial/p' db/datos-iniciales.sql | sed '$d'; echo "COMMIT;"; } > pautas_4_a_13.sql
   grep -c "^INSERT INTO" pautas_4_a_13.sql          # debe decir 3; si no, detente
   podman exec -i isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 isw_practicas' < pautas_4_a_13.sql
   ```
6. **Las migraciones, una por una y en este orden** (cada una se ejecuta **una sola vez**; si termina sin mensajes,
   salió bien; si muestra `ERROR`, **detente** y avisa):
   ```bash
   for f in 012_formato_oficial_planificacion 013_grupo_practica 014_parametro_sistema 015_nota_final \
            016_nota_minima_de_la_pauta 017_exigencia_parametro 018_lectura_chat_coordinador \
            019_producto_practica 020_archivo_oficial_pauta; do
     echo "== $f"
     podman exec -i isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 isw_practicas' < db/migraciones/$f.sql || break
   done
   ```
   El `|| break` corta en la primera que falle. Revisa antes que los nombres calcen con `ls db/migraciones`.
7. **Comprueba:** vuelve a ejecutar `diagnostico.sql` (todas `SI`) y el bloque de conteos del 5.5. Aquí las tablas
   dan **31** en vez de 28: sobran `componente_nota`, `nota_componente` y `equipo_practica` (y las columnas
   `inscripcion.nota_calculada` e `inscripcion.equipo_id`), de tu código del 28-oct. **No molestan** (la app no las
   usa) y **no se borran** por ahora: queda decidido dejarlas tal cual.
8. **Arranca:** `pc build backend` y `pc up -d`, y sigue con el 5.5.

| Si pasa esto | Qué significa | Qué hacer |
|---|---|---|
| La 012 dice `Migracion 012 detenida sin cambios...` | Hay planificaciones con minutos que no se pueden leer | No cambió nada. Anota los ids y avisa |
| La 015 da `ERROR 1452 ... foreign key constraint fails` | Faltan las pautas 4 a 13 | Restaura el respaldo (abajo) y repite desde el punto 5 |
| `Duplicate column` o `Table ... already exists` | Esa migración ya estaba aplicada | Detente y vuelve al diagnóstico |
| Cualquier otro `ERROR` | — | Detente, copia el mensaje completo y avisa |

Restaurar el respaldo:
`podman exec -i isw-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4' < respaldo_AAAA-MM-DD_HHMM.sql`
(o, más simple, como no hay nada real: haz la instalación limpia del 5.2).

### 5.7 Si algo falla (cualquiera de los dos caminos)

| Síntoma | Qué hacer |
|---|---|
| `isw-backend` se reinicia una y otra vez | `podman logs --tail 80 isw-backend`. `Communications link failure`: MySQL aún no estaba listo; espera y mira de nuevo. `Access denied for user 'isw_app'`: la base se creó con otras claves (faltó `--env-file`); instalación limpia de nuevo |
| El log dice `Schema-validation: missing column/table` | La base no corresponde al código: en la instalación limpia, el volumen no se borró (5.2); en la alternativa, falta una migración |
| `Permission denied` al leer `schema.sql`, `seed.sql`, `nginx-frontend.conf` o la carpeta `static` | Es SELinux: revisa que esos montajes en tu `docker-compose.yml` sigan con `:ro,Z` (`git diff docker-compose.yml`) |
| La página en el 8017 carga pero el login falla | `podman logs --tail 30 isw-frontend` y que `isw-backend` esté `healthy` |
| Hay que volver a la versión anterior | Como la base es de demo: `git checkout <commit anterior>`, `pc down`, borrar volúmenes, 5.3 y 5.4 |

### 5.8 Lo que NO se hace en este servidor

- **Cargar datos reales de estudiantes.** Esta instalación tiene usuarios de demostración con una clave conocida,
  `isw_app` con todos los permisos sobre la base (MySQL del contenedor se los da), Swagger encendido en el 8018 y
  todo por HTTP sin cifrar. Es una demo; para producción está `deploy/install.sh`.
- Exponer el puerto de MySQL (3306) hacia afuera.
- Subir `.env.docker` al repositorio, pegarlo en un chat o mandarlo por correo.
- Ejecutar `db/bd.ps1` (es de Windows y de desarrollo) o `db/schema.sql` a mano sobre una base con datos (empieza
  con `DROP TABLE`).

### 5.9 Las próximas actualizaciones

Mientras el servidor solo tenga datos de demo: `git pull`, `pc down`, borrar los dos volúmenes, 5.3, 5.4 y 5.5.
Si algún día tiene datos que cuidar: respaldo y **solo** las migraciones nuevas (021 en adelante), con la forma del
5.1 (punto 2). **Mejora pendiente:** una herramienta de migraciones (por ejemplo Flyway) que registre cuáles se
aplicaron (`objetivosAFUTURO.md`).

---

## 6. Preguntas para el área de informática de la UBB

- [ ] ¿Qué sistema operativo tendrá el servidor? ¿Cuánta memoria, CPU y disco?
- [ ] ¿Cómo entramos: SSH directo o por VPN? ¿Con permisos de administrador?
- [ ] ¿Qué subdominio nos asignan? ¿Ustedes dan el certificado HTTPS o usamos Let's Encrypt?
- [ ] ¿Abren los puertos 80 y 443 hacia internet, o el sistema solo se verá dentro de la red de la UBB?
- [ ] ¿Instalamos MySQL nosotros o nos dan una base de datos?
- [ ] ¿Respaldan ustedes la máquina? ¿Con qué frecuencia y por cuánto tiempo guardan las copias?
- [ ] ¿Hay reglas para guardar datos personales de estudiantes (RUT, notas, audios)?
- [ ] ¿Podemos usar el correo de la universidad (SMTP) para avisos automáticos? (bloquea HU-19)
- [ ] ¿El servidor puede conectarse por HTTPS a servicios externos (para el chatbot)?
- [ ] ¿Quién administra Adecca y qué formato de archivo acepta para cargar notas? (bloquea HU-13)
- [ ] ¿Quién es el contacto técnico y hasta cuándo queda el servidor disponible después del semestre?
