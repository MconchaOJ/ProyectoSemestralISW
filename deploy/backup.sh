#!/usr/bin/env bash
# =============================================================================
# Respaldo del Sistema de Prácticas PEM-UBB (Sprint 2, DESPLIEGUE.md).
#
# Genera, con la fecha y hora en el nombre:
#   <destino>/isw_practicas_AAAA-MM-DD_HHMM.sql        volcado completo de la base (mysqldump)
#   <destino>/adjuntos_AAAA-MM-DD_HHMM.tar.gz           copia de la carpeta de documentos y audios
# y borra los respaldos con más de DIAS_RETENCION días.
#
# Lo instala deploy/install.sh en /usr/local/sbin/isw-practicas-backup y lo programa todos los días a las
# 03:15 (/etc/cron.d/isw-practicas-backup). También se puede correr a mano:
#   sudo /usr/local/sbin/isw-practicas-backup
#
# Conexión a MySQL (sin claves en este archivo):
#   - Si existe /etc/isw-practicas/backup.cnf (formato [client] user=... password=... host=...), se usa ese.
#     Sirve cuando la base es de la universidad (opción B). Debe ser de root y con permisos 600.
#   - Si no, se conecta como root por el socket local (en Ubuntu, root entra a MySQL sin clave desde el
#     propio servidor). Es el caso de la base instalada por install.sh (opción A).
#
# Restaurar (con la aplicación detenida):
#   sudo systemctl stop isw-practicas
#   sudo mysql < isw_practicas_AAAA-MM-DD_HHMM.sql
#   sudo tar -xzf adjuntos_AAAA-MM-DD_HHMM.tar.gz -C /var/lib/isw-practicas
#   sudo systemctl start isw-practicas
#
# Variables opcionales (para probar o cambiar destinos):
#   ISW_BACKUP_DIR (por defecto /var/backups/isw-practicas), ISW_ADJUNTOS_DIR (por defecto
#   /var/lib/isw-practicas/adjuntos), DIAS_RETENCION (por defecto 30), ISW_BACKUP_CNF.
# =============================================================================
set -euo pipefail

DESTINO="${ISW_BACKUP_DIR:-/var/backups/isw-practicas}"
ADJUNTOS="${ISW_ADJUNTOS_DIR:-/var/lib/isw-practicas/adjuntos}"
DIAS_RETENCION="${DIAS_RETENCION:-30}"
CNF="${ISW_BACKUP_CNF:-/etc/isw-practicas/backup.cnf}"
BASE="isw_practicas"
FECHA="$(date +%F_%H%M)"

registrar() { echo "[$(date '+%F %T')] $*"; }
fallar() { registrar "ERROR: $*" >&2; exit 1; }

umask 077                       # los respaldos tienen RUT y notas: solo root los lee
mkdir -p "$DESTINO"
chmod 700 "$DESTINO"

# --- 1. Base de datos ---------------------------------------------------------
OPCIONES_CONEXION=()
if [[ -f "$CNF" ]]; then
    OPCIONES_CONEXION=(--defaults-extra-file="$CNF")
fi
ARCHIVO_SQL="$DESTINO/${BASE}_${FECHA}.sql"
TEMPORAL="$ARCHIVO_SQL.parcial"
registrar "Respaldando la base $BASE en $ARCHIVO_SQL"
# --single-transaction: copia consistente sin bloquear a los usuarios (todas las tablas son InnoDB).
# --routines y --triggers: incluye el procedimiento y el trigger que validan los puntajes.
# --databases: el archivo trae su CREATE DATABASE/USE, así se restaura con un simple "mysql < archivo".
if ! mysqldump "${OPCIONES_CONEXION[@]}" --single-transaction --routines --triggers --no-tablespaces \
        --default-character-set=utf8mb4 --databases "$BASE" > "$TEMPORAL"; then
    rm -f "$TEMPORAL"
    fallar "mysqldump falló (¿MySQL está corriendo? ¿credenciales de $CNF?)."
fi
# mysqldump escribe esta línea al final solo si terminó bien
tail -n 1 "$TEMPORAL" | grep -q "Dump completed" || { rm -f "$TEMPORAL"; fallar "el volcado quedó incompleto."; }
mv "$TEMPORAL" "$ARCHIVO_SQL"
registrar "OK base: $(du -h "$ARCHIVO_SQL" | cut -f1)"

# --- 2. Documentos y audios ---------------------------------------------------
ARCHIVO_ADJ="$DESTINO/adjuntos_${FECHA}.tar.gz"
if [[ -d "$ADJUNTOS" ]]; then
    registrar "Respaldando $ADJUNTOS en $ARCHIVO_ADJ"
    tar -czf "$ARCHIVO_ADJ.parcial" -C "$(dirname "$ADJUNTOS")" "$(basename "$ADJUNTOS")" \
        || { rm -f "$ARCHIVO_ADJ.parcial"; fallar "no se pudo comprimir $ADJUNTOS."; }
    mv "$ARCHIVO_ADJ.parcial" "$ARCHIVO_ADJ"
    registrar "OK adjuntos: $(du -h "$ARCHIVO_ADJ" | cut -f1)"
else
    registrar "AVISO: no existe $ADJUNTOS; no hay adjuntos que respaldar."
fi

# --- 3. Limpieza de respaldos antiguos -------------------------------------------
BORRADOS=$(find "$DESTINO" -maxdepth 1 -type f \( -name "${BASE}_*.sql" -o -name 'adjuntos_*.tar.gz' \) \
    -mtime +"$DIAS_RETENCION" -print -delete | wc -l)
registrar "Listo. Respaldos de más de $DIAS_RETENCION días borrados: $BORRADOS."
registrar "Recuerda: un respaldo que solo está en este servidor no sirve si se pierde el servidor (copiar fuera)."
