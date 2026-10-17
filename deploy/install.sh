#!/usr/bin/env bash
# =============================================================================
# Instalación del Sistema de Prácticas PEM-UBB en un servidor Linux (Ubuntu/Debian).
# Sprint 2, DESPLIEGUE.md. Paso a paso explicado en deploy/GUIA_DESPLIEGUE_EQUIPO.md.
#
# Uso (desde la raíz del repositorio clonado en el servidor):
#   sudo ./deploy/install.sh --dominio practicas-pem.ubiobio.cl --base-local
#   sudo ./deploy/install.sh --dominio practicas-pem.ubiobio.cl --base-externa
#
# Opciones:
#   --dominio NOMBRE   subdominio que atiende nginx (obligatorio la primera vez)
#   --jar RUTA         .jar compilado (por defecto backend/target/practicas-0.1.0-SNAPSHOT.jar)
#   --base-local       instala MySQL en este servidor, crea la base (solo si está vacía), el coordinador
#                      inicial y el usuario isw_app (opción A de DESPLIEGUE.md)
#   --base-externa     la base la entrega la universidad (opción B): no instala MySQL; hay que completar
#                      ISW_DB_HOST e ISW_DB_PASSWORD en /etc/isw-practicas/isw-practicas.env
#   --sin-nginx        no instala ni configura nginx (por ejemplo, si la universidad pone su propio proxy)
#
# Se puede volver a correr cuantas veces se quiera (por ejemplo, para instalar un .jar nuevo): nunca
# borra datos, nunca vuelve a ejecutar schema.sql sobre una base con tablas y nunca sobrescribe el archivo
# de claves ni un certificado existente.
#
# Qué deja instalado:
#   /opt/isw-practicas/practicas.jar                  la aplicación (la versión anterior queda como .anterior)
#   /etc/isw-practicas/isw-practicas.env              claves y configuración (root:isw, 640)
#   /var/lib/isw-practicas/adjuntos                   documentos y audios (fuera de la carpeta de la app)
#   /var/log/isw-practicas                            registros de la aplicación y del respaldo
#   /etc/systemd/system/isw-practicas.service         servicio que arranca solo y se reinicia si cae
#   /etc/nginx/sites-available/isw-practicas          proxy HTTPS (con certificado autofirmado provisorio)
#   /usr/local/sbin/isw-practicas-backup + cron       respaldo diario a las 03:15 en /var/backups/isw-practicas
# =============================================================================
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOY="$RAIZ/deploy"
JAR="$RAIZ/backend/target/practicas-0.1.0-SNAPSHOT.jar"
DOMINIO=""
MODO_BASE=""
CON_NGINX=1

APP_DIR=/opt/isw-practicas
CONF_DIR=/etc/isw-practicas
ENV_FILE=$CONF_DIR/isw-practicas.env
DATOS_DIR=/var/lib/isw-practicas
ADJUNTOS_DIR=$DATOS_DIR/adjuntos
LOG_DIR=/var/log/isw-practicas
CERT_DIR=/etc/ssl/isw-practicas
USUARIO=isw
BASE=isw_practicas

paso() { echo; echo "==> $*"; }
ok() { echo "    OK  $*"; }
aviso() { echo "    AVISO  $*"; }
fallar() { echo "ERROR: $*" >&2; exit 1; }

# Versión mínima de MySQL: la misma regla que db/VersionMysql.ps1 (bd.ps1) y db/datos-iniciales.sql; las tres se
# prueban con db/pruebas-version-mysql.ps1. Desde 8.0.16 MySQL cumple los CHECK (antes los acepta y los ignora sin
# error). Acepta sufijos como "8.0.36-0ubuntu0.22.04.1"; rechaza MariaDB y textos que no son versiones.
VERSION_MINIMA_MYSQL=8.0.16
version_mysql_ok() {
    [[ "$1" != *MariaDB* ]] || return 1
    [[ "$1" =~ ^[[:space:]]*([0-9]+)\.([0-9]+)\.([0-9]+) ]] || return 1
    local a=$((10#${BASH_REMATCH[1]})) b=$((10#${BASH_REMATCH[2]})) c=$((10#${BASH_REMATCH[3]}))
    (( a > 8 || (a == 8 && (b > 0 || c >= 16)) ))
}

# ----------------------------------------------------------------------------- argumentos
while [[ $# -gt 0 ]]; do
    case "$1" in
        --dominio) DOMINIO="${2:-}"; shift 2 ;;
        --jar) JAR="${2:-}"; shift 2 ;;
        --base-local) MODO_BASE=local; shift ;;
        --base-externa) MODO_BASE=externa; shift ;;
        --sin-nginx) CON_NGINX=0; shift ;;
        -h|--help) sed -n '2,32p' "$0"; exit 0 ;;
        *) fallar "opción desconocida: $1 (usa --help)" ;;
    esac
done

[[ $EUID -eq 0 ]] || fallar "ejecútalo con sudo."
command -v apt-get >/dev/null || fallar "este script es para Ubuntu/Debian (no encontré apt-get)."
[[ -f "$JAR" ]] || fallar "no encuentro el .jar en $JAR (compílalo con 'mvn clean package' o indica --jar)."
[[ -n "$MODO_BASE" ]] || fallar "indica --base-local (MySQL en este servidor) o --base-externa (base de la universidad)."
if [[ $CON_NGINX -eq 1 && -z "$DOMINIO" ]]; then
    # En una reinstalación se reutiliza el dominio que ya quedó configurado
    if [[ -f /etc/nginx/sites-available/isw-practicas ]]; then
        DOMINIO=$(awk '/server_name/ {gsub(";",""); print $2; exit}' /etc/nginx/sites-available/isw-practicas)
    fi
    [[ -n "$DOMINIO" ]] || fallar "indica --dominio (por ejemplo practicas-pem.ubiobio.cl)."
fi
[[ -z "$DOMINIO" || "$DOMINIO" =~ ^[A-Za-z0-9.-]+$ ]] || fallar "dominio inválido: $DOMINIO"

# ----------------------------------------------------------------------------- 1. paquetes
paso "1. Paquetes del sistema"
PAQUETES=(openjdk-17-jre-headless openssl curl cron)
[[ $CON_NGINX -eq 1 ]] && PAQUETES+=(nginx)
if [[ $MODO_BASE == local ]]; then PAQUETES+=(mysql-server); else PAQUETES+=(mysql-client); fi
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq "${PAQUETES[@]}" >/dev/null
ok "instalado: ${PAQUETES[*]}"
JAVA_VERSION=$(/usr/bin/java -version 2>&1 | awk -F'"' '/version/ {print $2}')
[[ "$JAVA_VERSION" == 17* ]] || aviso "/usr/bin/java es la versión $JAVA_VERSION; la aplicación se probó con Java 17 (update-alternatives --config java)."
ok "Java $JAVA_VERSION"
# Versión de MySQL: antes de cambiar nada más y en cada ejecución, existan o no las tablas (en una reinstalación
# el paso 5 no vuelve a cargar schema.sql, así que este es el único control).
if [[ $MODO_BASE == local ]]; then
    systemctl enable --now mysql >/dev/null 2>&1 || systemctl enable --now mysqld >/dev/null
    VERSION_MYSQL=$(mysql -N -e "SELECT VERSION()") || fallar "no pude consultar la versión de MySQL."
    version_mysql_ok "$VERSION_MYSQL" || fallar "MySQL $VERSION_MYSQL no sirve: se necesita MySQL $VERSION_MINIMA_MYSQL o superior \
(se recomienda 8.4 LTS; MariaDB y MySQL 5.7 no sirven). No se creó ni modificó nada fuera de los paquetes del sistema."
    ok "MySQL $VERSION_MYSQL (mínimo $VERSION_MINIMA_MYSQL)"
else
    aviso "base externa: TI debe entregar MySQL $VERSION_MINIMA_MYSQL o superior (se pide 8.4 LTS); datos-iniciales.sql también lo controla."
fi

# ----------------------------------------------------------------------------- 2. usuario y carpetas
paso "2. Usuario del servicio y carpetas"
if ! id "$USUARIO" >/dev/null 2>&1; then
    useradd --system --home-dir "$APP_DIR" --shell /usr/sbin/nologin "$USUARIO"
    ok "usuario de sistema '$USUARIO' creado (sin login)"
else
    ok "el usuario '$USUARIO' ya existía"
fi
install -d -o root -g "$USUARIO" -m 750 "$APP_DIR" "$CONF_DIR"
install -d -o "$USUARIO" -g "$USUARIO" -m 750 "$DATOS_DIR" "$ADJUNTOS_DIR" "$LOG_DIR"
ok "$APP_DIR, $CONF_DIR, $ADJUNTOS_DIR, $LOG_DIR"

# ----------------------------------------------------------------------------- 3. archivo de claves
paso "3. Archivo de claves $ENV_FILE"
if [[ -f "$ENV_FILE" ]]; then
    ok "ya existe: no se modifica"
else
    if [[ $MODO_BASE == local ]]; then CLAVE_DB=$(openssl rand -hex 24); HOST_DB=127.0.0.1
    else CLAVE_DB=COMPLETAR_CLAVE_DE_ISW_APP; HOST_DB=COMPLETAR_HOST_DE_LA_BASE; fi
    ( umask 027
      cat > "$ENV_FILE" <<EOF
# Claves del Sistema de Prácticas PEM-UBB. Generado por install.sh el $(date +%F).
# Solo root lo edita; el servicio (grupo isw) lo lee. NUNCA se copia al repositorio.
# Después de cambiarlo: sudo systemctl restart isw-practicas
ISW_DB_HOST=$HOST_DB
ISW_DB_PORT=3306
ISW_DB_PASSWORD=$CLAVE_DB
# Firma de las sesiones (mínimo 32 caracteres). Si cambia, todos deben volver a iniciar sesión.
ISW_JWT_SECRET=$(openssl rand -hex 32)
APP_ADJUNTOS_DIRECTORIO=$ADJUNTOS_DIR
ISW_LOG_FILE=$LOG_DIR/isw-practicas.log
EOF
    )
    chown root:"$USUARIO" "$ENV_FILE"; chmod 640 "$ENV_FILE"
    ok "creado con una clave de isw_app y un secreto JWT aleatorios"
    [[ $MODO_BASE == externa ]] && aviso "completa ISW_DB_HOST e ISW_DB_PASSWORD en $ENV_FILE con los datos que entregue TI."
fi
leer_env() { grep -E "^$1=" "$ENV_FILE" | head -n 1 | cut -d= -f2-; }

# ----------------------------------------------------------------------------- 4. aplicación
paso "4. Aplicación ($APP_DIR/practicas.jar)"
if [[ -f "$APP_DIR/practicas.jar" ]] && cmp -s "$JAR" "$APP_DIR/practicas.jar"; then
    ok "el .jar instalado ya es esta misma versión"
else
    [[ -f "$APP_DIR/practicas.jar" ]] && cp -p "$APP_DIR/practicas.jar" "$APP_DIR/practicas.jar.anterior" \
        && ok "la versión anterior quedó en practicas.jar.anterior (para volver atrás)"
    install -o root -g "$USUARIO" -m 640 "$JAR" "$APP_DIR/practicas.jar"
    ok "copiado $(basename "$JAR")"
fi
install -o root -g "$USUARIO" -m 644 "$DEPLOY/GUIA_DESPLIEGUE_EQUIPO.md" "$APP_DIR/GUIA_DESPLIEGUE_EQUIPO.md"

# ----------------------------------------------------------------------------- 5. base de datos
paso "5. Base de datos"
escapar_sql() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e "s/'/''/g"; }
if [[ $MODO_BASE == local ]]; then
    MYSQL=(mysql --default-character-set=utf8mb4)   # root por el socket local, sin clave; versión ya comprobada en el paso 1
    TABLAS=$("${MYSQL[@]}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$BASE'")
    if [[ "$TABLAS" -gt 0 ]]; then
        ok "la base $BASE ya tiene $TABLAS tablas: NO se toca (los cambios nuevos van como migración, ver la guía)"
    else
        echo "    La base está vacía: se crean las tablas (schema.sql) y los datos iniciales (datos-iniciales.sql)."
        echo "    Datos del coordinador inicial (desde su cuenta se registra a todos los demás):"
        read -r -p "      Correo institucional: " C_EMAIL
        read -r -p "      Nombre: " C_NOMBRE
        read -r -p "      Apellidos: " C_APELLIDOS
        [[ -n "$C_EMAIL" && -n "$C_NOMBRE" && -n "$C_APELLIDOS" ]] || fallar "faltan datos del coordinador."
        [[ "$C_EMAIL" != *@demo.example ]] || fallar "no se usa un correo de demostración en el servidor."
        # La clave se lee sin eco y llega al generador por la entrada estándar (printf es interno de bash: no
        # aparece en la lista de procesos). Solo se guarda su hash BCrypt.
        read -r -s -p "      Clave del coordinador (mínimo 10 caracteres, no se muestra): " C_CLAVE; echo
        if [[ -t 0 ]]; then
            read -r -s -p "      Repite la clave: " C_CLAVE2; echo
            [[ "$C_CLAVE" == "$C_CLAVE2" ]] || fallar "las claves no coinciden."
        fi
        C_HASH=$(printf '%s\n' "$C_CLAVE" | /usr/bin/java -cp "$APP_DIR/practicas.jar" \
            -Dloader.main=cl.ubb.pem.practicas.security.GeneradorHashClave \
            org.springframework.boot.loader.launch.PropertiesLauncher) || fallar "no se pudo generar el hash (¿clave de menos de 10 caracteres?)."
        unset C_CLAVE C_CLAVE2
        # schema.sql viene con fines de línea de Windows: se limpian para que DELIMITER funcione en Linux
        tr -d '\r' < "$RAIZ/db/schema.sql" | "${MYSQL[@]}" || fallar "falló schema.sql"
        ok "tablas creadas"
        { printf "SET @coordinador_email = '%s';\n" "$(escapar_sql "$C_EMAIL")"
          printf "SET @coordinador_nombre = '%s';\n" "$(escapar_sql "$C_NOMBRE")"
          printf "SET @coordinador_apellidos = '%s';\n" "$(escapar_sql "$C_APELLIDOS")"
          printf "SET @coordinador_hash = '%s';\n" "$C_HASH"
          tr -d '\r' < "$RAIZ/db/datos-iniciales.sql"
        } | "${MYSQL[@]}" || fallar "falló datos-iniciales.sql (la base quedó con tablas vacías: revisa el error, bórrala con DROP DATABASE $BASE y vuelve a correr)"
        ok "datos iniciales cargados; coordinador: $C_EMAIL"
    fi
    # Usuario de la aplicación: solo datos (nunca puede crear ni borrar tablas). Clave del archivo de claves.
    # La clave va por la entrada estándar de mysql, no como argumento (no aparece en la lista de procesos).
    CLAVE_DB=$(leer_env ISW_DB_PASSWORD)
    for HOST in localhost 127.0.0.1; do
        "${MYSQL[@]}" <<EOF
CREATE USER IF NOT EXISTS 'isw_app'@'$HOST' IDENTIFIED BY '$CLAVE_DB';
ALTER USER 'isw_app'@'$HOST' IDENTIFIED BY '$CLAVE_DB';
GRANT SELECT, INSERT, UPDATE, DELETE ON $BASE.* TO 'isw_app'@'$HOST';
EOF
    done
    ok "usuario isw_app con SELECT, INSERT, UPDATE, DELETE sobre $BASE"
else
    ok "base externa: no se instala MySQL. La crea TI con db/schema.sql y db/datos-iniciales.sql (ver la guía)."
fi

# ----------------------------------------------------------------------------- 6. servicio
paso "6. Servicio systemd isw-practicas"
install -o root -g root -m 644 "$DEPLOY/isw-practicas.service" /etc/systemd/system/isw-practicas.service
systemctl daemon-reload
systemctl enable isw-practicas >/dev/null 2>&1
if grep -q COMPLETAR "$ENV_FILE"; then
    aviso "no se arranca: faltan datos en $ENV_FILE. Complétalos y ejecuta: sudo systemctl restart isw-practicas"
    ARRANCADO=0
else
    systemctl restart isw-practicas
    ARRANCADO=1
    ok "habilitado (arranca con el servidor) y reiniciado"
fi

# ----------------------------------------------------------------------------- 7. nginx
if [[ $CON_NGINX -eq 1 ]]; then
    paso "7. Nginx para $DOMINIO"
    install -d -m 755 "$CERT_DIR" /var/www/letsencrypt
    if [[ ! -f "$CERT_DIR/fullchain.pem" || ! -f "$CERT_DIR/privkey.pem" ]]; then
        openssl req -x509 -nodes -newkey rsa:2048 -days 30 -subj "/CN=$DOMINIO" \
            -keyout "$CERT_DIR/privkey.pem" -out "$CERT_DIR/fullchain.pem" 2>/dev/null
        chmod 600 "$CERT_DIR/privkey.pem"
        aviso "certificado AUTOFIRMADO provisorio (30 días): reemplázalo por el definitivo (guía, paso 7)."
    else
        ok "ya hay un certificado en $CERT_DIR: no se toca"
    fi
    sed "s/__DOMINIO__/$DOMINIO/g" "$DEPLOY/nginx.conf" > /etc/nginx/sites-available/isw-practicas
    ln -sf /etc/nginx/sites-available/isw-practicas /etc/nginx/sites-enabled/isw-practicas
    if [[ -L /etc/nginx/sites-enabled/default ]]; then
        rm /etc/nginx/sites-enabled/default
        ok "se desactivó el sitio 'default' de nginx (queda en sites-available)"
    fi
    nginx -t 2>/dev/null || { nginx -t; fallar "la configuración de nginx tiene errores."; }
    systemctl enable nginx >/dev/null 2>&1
    systemctl reload nginx 2>/dev/null || systemctl restart nginx
    ok "sitio isw-practicas activo (80 redirige a 443; subida máxima 30 MB)"
fi

# ----------------------------------------------------------------------------- 8. respaldo
paso "8. Respaldo diario"
install -o root -g root -m 750 "$DEPLOY/backup.sh" /usr/local/sbin/isw-practicas-backup
cat > /etc/cron.d/isw-practicas-backup <<EOF
# Respaldo diario del Sistema de Prácticas PEM-UBB (base + adjuntos). Instalado por deploy/install.sh.
15 3 * * * root /usr/local/sbin/isw-practicas-backup >> $LOG_DIR/backup.log 2>&1
EOF
chmod 644 /etc/cron.d/isw-practicas-backup
ok "todos los días a las 03:15 en /var/backups/isw-practicas (registro en $LOG_DIR/backup.log)"
[[ $MODO_BASE == externa ]] && aviso "con base externa, crea /etc/isw-practicas/backup.cnf (guía, paso 8) para que el respaldo pueda conectarse."

# ----------------------------------------------------------------------------- 9. comprobación
if [[ $ARRANCADO -eq 1 ]]; then
    paso "9. Comprobación"
    for _ in $(seq 1 60); do
        if curl -fsS -o /dev/null http://127.0.0.1:8080/index.html 2>/dev/null; then
            ok "la aplicación responde en 127.0.0.1:8080"
            if [[ $CON_NGINX -eq 1 ]]; then
                if curl -fsSk -o /dev/null --resolve "$DOMINIO:443:127.0.0.1" "https://$DOMINIO/index.html"; then
                    ok "nginx responde por HTTPS para $DOMINIO"
                else
                    aviso "nginx no respondió por HTTPS: revisa sudo nginx -t y /var/log/nginx/isw-practicas.error.log"
                fi
            fi
            echo; echo "Instalación lista. Abre https://$DOMINIO e ingresa con el coordinador."
            exit 0
        fi
        sleep 2
    done
    fallar "la aplicación no respondió en 2 minutos. Revisa: sudo journalctl -u isw-practicas -n 50 y $LOG_DIR/isw-practicas.log"
fi
echo; echo "Instalación lista, pero el servicio no se arrancó (faltan datos en $ENV_FILE)."
