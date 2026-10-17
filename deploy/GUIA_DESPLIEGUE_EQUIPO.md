# Guía de despliegue para el equipo

**Sistema de Formación Práctica PEM-UBB** · Sprint 2 · 11 de octubre de 2026

Esta guía sirve para publicar el sistema en el servidor de la universidad **sin depender de una sola persona**.
Sigue los pasos en orden. El contexto (qué pedir a TI y por qué) está en `DESPLIEGUE.md`, en la raíz del repositorio.

> **Probado** el 11-oct-2026 en un Ubuntu 26.04 LTS (WSL) con MySQL 8.4.11, nginx 1.28 y OpenJDK 17: instalación
> completa desde cero, la aplicación vuelve sola si se mata el proceso (`kill -9`), nginx deja pasar 29 MB y corta
> 31 MB (413), el puerto 80 redirige a HTTPS, el respaldo genera el `.sql` y el `.tar.gz` con la fecha del día y se
> restauró en una base de prueba, y volver a correr la instalación no tocó la base ni las claves.

---

## Qué hay en esta carpeta

| Archivo | Qué hace | Dónde queda en el servidor |
|---|---|---|
| `install.sh` | Instala todo (Java 17, MySQL, nginx, servicio, carpetas, respaldo). Se puede volver a correr | — (se corre desde el repositorio) |
| `isw-practicas.service` | Servicio `systemd`: arranca la app con el servidor y la reinicia si cae | `/etc/systemd/system/` |
| `nginx.conf` | Proxy HTTPS hacia `127.0.0.1:8080`, subida máxima 30 MB, 80 → 443 | `/etc/nginx/sites-available/isw-practicas` |
| `backup.sh` | Respaldo de la base (`mysqldump`) y de los adjuntos, con la fecha en el nombre | `/usr/local/sbin/isw-practicas-backup` (cron 03:15) |

Ninguno lleva claves. Las claves viven solo en `/etc/isw-practicas/isw-practicas.env`, en el servidor.

---

## Paso 0. Antes de empezar

- [ ] TI entregó: servidor Linux (Ubuntu o Debian), acceso SSH con `sudo`, el subdominio (por ejemplo
      `practicas-pem.ubiobio.cl`) apuntando al servidor, y los puertos 80 y 443 abiertos.
- [ ] Sabemos si la base la instalamos nosotros (**opción A**, lo más simple) o la da TI (**opción B**).
- [ ] Sabemos si TI entrega el certificado HTTPS o usamos Let's Encrypt.

Las preguntas exactas para TI están en `DESPLIEGUE.md`, sección 6.

## Paso 1. Compilar en el computador de desarrollo (Windows)

```powershell
.\db\bd.ps1                 # base de desarrollo limpia (las pruebas la necesitan así)
cd backend
mvn clean package           # TODAS las pruebas deben pasar; si alguna falla, no se publica
```

Resultado: `backend\target\practicas-0.1.0-SNAPSHOT.jar`. Es el único archivo que corre en el servidor.

## Paso 2. Llevar el repositorio y el `.jar` al servidor

```bash
ssh usuario@practicas-pem.ubiobio.cl
git clone <url-del-repositorio> isw && cd isw
mkdir -p backend/target
```

Y desde Windows (otra terminal), copiar el `.jar` compilado:

```powershell
scp backend\target\practicas-0.1.0-SNAPSHOT.jar usuario@practicas-pem.ubiobio.cl:~/isw/backend/target/
```

> Si el servidor tiene Java 17 JDK y Maven, también se puede compilar allá (`cd backend && mvn clean package -DskipTests`),
> pero las pruebas necesitan la base de demostración: **córrelas siempre en el computador de desarrollo**.

## Paso 3. Instalar

**Opción A (MySQL en el mismo servidor):**

```bash
sudo ./deploy/install.sh --dominio practicas-pem.ubiobio.cl --base-local
```

Justo después de instalar los paquetes comprueba la versión de MySQL, **cada vez que se corre** (también en una
reinstalación con las tablas ya creadas): si es menor que 8.0.16, o es MariaDB, se detiene con `ERROR: MySQL ... no
sirve` antes de crear el usuario, las carpetas, las claves o las tablas. Con la versión de Ubuntu (8.4) pasa sin más.

Te pedirá los datos del **coordinador inicial** (correo institucional, nombre, apellidos y una clave de al menos 10
caracteres, dos veces y sin mostrarla). Solo se guarda el hash de la clave. Desde esa cuenta se registra a todos los
demás usuarios por la página.

**Opción B (base de datos de TI):**

```bash
sudo ./deploy/install.sh --dominio practicas-pem.ubiobio.cl --base-externa
```

No instala MySQL ni arranca la app: primero hay que hacer el **paso 4**.

El script muestra `OK` en cada etapa. Si algo falla, se detiene con `ERROR:` y el motivo; se corrige y se vuelve a
correr (es seguro: nunca borra datos, nunca re-ejecuta `schema.sql` sobre una base con tablas y nunca sobrescribe el
archivo de claves ni el certificado).

## Paso 4. Solo opción B: la base de TI

Pedirle a TI (o hacerlo con ellos), **una sola vez** y como administrador de MySQL 8.4:

```bash
mysql -h <host> -u <admin> -p < db/schema.sql
mysql -h <host> -u <admin> -p        # y dentro:
  SET @coordinador_email = 'nombre.apellido@ubiobio.cl';
  SET @coordinador_nombre = 'Nombre';
  SET @coordinador_apellidos = 'Apellidos';
  SET @coordinador_hash = '<hash>';   -- ver abajo cómo generarlo
  SOURCE db/datos-iniciales.sql;
  CREATE USER 'isw_app'@'<ip-del-servidor-de-la-app>' IDENTIFIED BY '<clave-larga-aleatoria>';
  GRANT SELECT, INSERT, UPDATE, DELETE ON isw_practicas.* TO 'isw_app'@'<ip-del-servidor-de-la-app>';
```

El hash de la clave del coordinador se genera en el servidor (pide la clave sin mostrarla):

```bash
java -cp /opt/isw-practicas/practicas.jar -Dloader.main=cl.ubb.pem.practicas.security.GeneradorHashClave \
     org.springframework.boot.loader.launch.PropertiesLauncher
```

Después, completar `ISW_DB_HOST` e `ISW_DB_PASSWORD` y arrancar:

```bash
sudo nano /etc/isw-practicas/isw-practicas.env
sudo systemctl restart isw-practicas
```

## Paso 5. Comprobar que quedó funcionando

```bash
sudo systemctl status isw-practicas          # debe decir "active (running)"
curl -I http://127.0.0.1:8080/index.html     # 200
sudo journalctl -u isw-practicas -n 50       # errores al arrancar (por ejemplo, una clave que falta)
sudo tail -n 100 /var/log/isw-practicas/isw-practicas.log
```

Luego abrir `https://<subdominio>` en el navegador e ingresar con el coordinador. Con el certificado provisorio el
navegador mostrará una advertencia: es esperado hasta el paso 7.

**Prueba de que es 24/7** (opcional, una vez): `sudo systemctl kill -s KILL isw-practicas`, esperar 10 segundos y
`systemctl status isw-practicas` debe volver a decir `active (running)`. Si se reinicia el servidor, el servicio
arranca solo (`systemctl is-enabled isw-practicas` → `enabled`).

## Paso 6. Cortafuegos

Si el servidor usa `ufw` (preguntar a TI si ellos manejan el cortafuegos):

```bash
sudo ufw allow OpenSSH       # ¡primero esto, o te quedas fuera del servidor!
sudo ufw allow 'Nginx Full'  # 80 y 443
sudo ufw enable
```

Nunca abrir hacia afuera el 8080 (la app) ni el 3306 (MySQL): solo se usan dentro del servidor.

## Paso 7. Certificado HTTPS definitivo

`install.sh` dejó un certificado **autofirmado de 30 días** en `/etc/ssl/isw-practicas/` solo para que nginx arranque.
Hay que reemplazarlo:

**Si TI entrega el certificado:** copiar la cadena completa y la clave privada con estos nombres y recargar:

```bash
sudo cp certificado-con-cadena.pem /etc/ssl/isw-practicas/fullchain.pem
sudo cp clave-privada.pem          /etc/ssl/isw-practicas/privkey.pem
sudo chmod 600 /etc/ssl/isw-practicas/privkey.pem
sudo nginx -t && sudo systemctl reload nginx
```

**Si usamos Let's Encrypt** (necesita el puerto 80 abierto desde internet):

```bash
sudo apt-get install -y certbot
sudo certbot certonly --webroot -w /var/www/letsencrypt -d practicas-pem.ubiobio.cl
sudo ln -sf /etc/letsencrypt/live/practicas-pem.ubiobio.cl/fullchain.pem /etc/ssl/isw-practicas/fullchain.pem
sudo ln -sf /etc/letsencrypt/live/practicas-pem.ubiobio.cl/privkey.pem   /etc/ssl/isw-practicas/privkey.pem
sudo nginx -t && sudo systemctl reload nginx
# que nginx tome el certificado renovado (certbot renueva solo cada ~60 días):
echo 'deploy-hook = systemctl reload nginx' | sudo tee -a /etc/letsencrypt/cli.ini
```

Verificar: el candado del navegador sin advertencias, y la **nota de voz** (micrófono) debe funcionar, porque el
navegador solo la permite en HTTPS.

## Paso 8. Respaldos

Quedan programados todos los días a las 03:15. Probarlo una vez a mano:

```bash
sudo /usr/local/sbin/isw-practicas-backup
sudo ls -l /var/backups/isw-practicas/      # isw_practicas_AAAA-MM-DD_HHMM.sql y adjuntos_AAAA-MM-DD_HHMM.tar.gz
```

- Se borran solos los de más de 30 días. El registro queda en `/var/log/isw-practicas/backup.log`.
- **Opción B:** el respaldo necesita credenciales para la base de TI. Crear `/etc/isw-practicas/backup.cnf`
  (dueño root, permisos 600) con un usuario que tenga al menos `SELECT, SHOW VIEW, TRIGGER, LOCK TABLES` y permiso
  para leer el procedimiento almacenado:
  ```ini
  [client]
  host=<host>
  user=<usuario-de-respaldo>
  password=<clave>
  ```
- **Un respaldo que solo está en el mismo servidor no protege si se pierde el servidor.** Acordar con TI dónde se
  copian (o copiarlos periódicamente a otro lugar). Los respaldos tienen RUT y notas: tratarlos como datos personales.

**Restaurar** (con la app detenida):

```bash
sudo systemctl stop isw-practicas
sudo mysql < /var/backups/isw-practicas/isw_practicas_AAAA-MM-DD_HHMM.sql
sudo tar -xzf /var/backups/isw-practicas/adjuntos_AAAA-MM-DD_HHMM.tar.gz -C /var/lib/isw-practicas
sudo systemctl start isw-practicas
```

## Paso 9. Publicar una versión nueva

1. En Windows: `.\db\bd.ps1` y `mvn clean package` (todas las pruebas pasan).
2. En el servidor: `cd ~/isw && git pull` y copiar el `.jar` nuevo a `~/isw/backend/target/` (paso 2).
3. **Si cambió la base** (hay un archivo nuevo en `db/migraciones/`): **respaldar primero** (paso 8) y ejecutar
   **solo la migración nueva**, como root: `sudo mysql < db/migraciones/00X_nombre.sql`.
4. `sudo ./deploy/install.sh --base-local` (o `--base-externa`): copia el `.jar`, guarda el anterior como
   `/opt/isw-practicas/practicas.jar.anterior` y reinicia el servicio.
5. Comprobar como en el paso 5.

**Volver atrás** si la versión nueva falla:

```bash
sudo cp /opt/isw-practicas/practicas.jar.anterior /opt/isw-practicas/practicas.jar
sudo systemctl restart isw-practicas
```

(Si la versión nueva traía una migración, volver atrás también la base: restaurar el respaldo hecho en el punto 3.)

> Una base creada con el `schema.sql` actual ya incluye las migraciones 001 a 004: en una instalación nueva no se
> corre ninguna migración.

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `systemctl status` dice `failed` | Falta una clave o MySQL no responde | `sudo journalctl -u isw-practicas -n 50`. Si dice `ISW_JWT_SECRET`, revisar el archivo de claves; si dice `Access denied for user 'isw_app'`, la clave de `ISW_DB_PASSWORD` no coincide con la de MySQL |
| La página da **502 Bad Gateway** | nginx funciona pero la app no | Ver la fila anterior; la app tarda unos 15 segundos en arrancar |
| Al subir un archivo: **413** | Más de 30 MB | Es el límite acordado (los documentos tienen 10 MB máximo) |
| El micrófono no funciona | No es HTTPS o el certificado no es válido | Paso 7 |
| Todos tienen que volver a iniciar sesión | Cambió `ISW_JWT_SECRET` | Normal si se cambió a propósito; no cambiarlo sin motivo |
| `install.sh`: "la base ya tiene N tablas: NO se toca" | Es lo correcto en una reinstalación | Los cambios de la base van como migración (paso 9) |

## Lo que NUNCA se hace en el servidor

- Ejecutar `db/seed.sql` o `db/bd.ps1`: traen usuarios ficticios con una clave conocida y **borran todas las tablas**.
- Ejecutar `db/schema.sql` sobre una base con datos (empieza con `DROP TABLE`).
- Copiar `/etc/isw-practicas/isw-practicas.env` al repositorio, a un chat o a un correo.
- Conectar la aplicación a MySQL como `root` (siempre `isw_app`, que no puede crear ni borrar tablas).
- Abrir los puertos 8080 o 3306 hacia internet.
