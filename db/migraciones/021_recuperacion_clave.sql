-- =============================================================================
-- Migracion 021 - Recuperacion de la clave por correo
--
-- Motivo (Sprint 2, 2026-11-23): reemplaza el aviso "Olvido su clave? Contacte al
-- coordinador" por un enlace de un solo uso que llega al correo del usuario.
--
-- SUPUESTO S-97: el enlace vale 30 minutos (app.recuperacion.minutos-validez) y una
--   sola vez. Se guarda solo el SHA-256 del token (64 caracteres hexadecimales),
--   nunca el token: es aleatorio de 32 bytes, asi que no necesita BCrypt.
-- SUPUESTO S-98: como maximo 3 solicitudes por correo cada 15 minutos; se cuentan
--   con las filas de esta tabla (no se pierden al reiniciar la aplicacion).
-- SUPUESTO S-99: sirve para los 6 roles con la cuenta activa; el correo depende del
--   mismo SMTP pendiente de HU-19 (sin SMTP el enlace queda en el log del servidor).
--
-- Cambios:
--   1. Tabla nueva token_recuperacion_clave (FK a usuario).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/021_recuperacion_clave.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye esta tabla.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE token_recuperacion_clave (
    id                BIGINT    NOT NULL AUTO_INCREMENT,
    usuario_id        BIGINT    NOT NULL,
    token_hash        CHAR(64)  NOT NULL COMMENT 'SHA-256 en hexadecimal del token enviado por correo (nunca el token)',
    fecha_creacion    DATETIME  NOT NULL,
    fecha_expiracion  DATETIME  NOT NULL,
    usado             BOOLEAN   NOT NULL DEFAULT FALSE COMMENT 'Usado o invalidado: no vuelve a servir',
    CONSTRAINT pk_token_recuperacion_clave PRIMARY KEY (id),
    CONSTRAINT uq_token_recuperacion_clave_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_recuperacion_clave_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_token_recuperacion_clave_hash CHECK (REGEXP_LIKE(token_hash, '^[0-9a-f]{64}$', 'c')),
    CONSTRAINT ck_token_recuperacion_clave_fechas CHECK (fecha_expiracion > fecha_creacion),
    INDEX ix_token_recuperacion_clave_usuario (usuario_id, fecha_creacion)
) ENGINE = InnoDB COMMENT = 'Enlaces de un solo uso para recuperar la clave por correo (S-97 a S-99)';
