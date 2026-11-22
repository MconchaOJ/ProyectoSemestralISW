-- =============================================================================
-- Migracion 018 - El coordinador tambien lee los chats del chatbot (HU-20, S-66)
--
-- Motivo (Sprint 2, 2026-11-22, decision del usuario):
--   El coordinador lee los chats de las practicas con chatbot igual que el jefe
--   de carrera: en solo lectura, el estudiante lo sabe y cada lectura queda
--   registrada. Se usa el MISMO registro que el jefe de carrera, no uno aparte:
--   es la misma regla y el mismo aviso al estudiante, y una columna rol dice
--   quien leyo. Por eso la tabla deja de llamarse "jefe_carrera".
--
-- Cambios:
--   1. lectura_chat_jefe_carrera pasa a llamarse lectura_chat.
--   2. Columna rol (JEFE_CARRERA o COORDINADOR): el rol con el que se leyo. Las
--      lecturas que ya existian son del jefe de carrera.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/018_lectura_chat_coordinador.sql
-- Para una base nueva NO hace falta: db/schema.sql ya crea lectura_chat.
-- Nota: en una base migrada las claves conservan sus nombres (fk_lectura_chat_*, ix_lectura_chat_inscripcion_fecha).
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

RENAME TABLE lectura_chat_jefe_carrera TO lectura_chat;

ALTER TABLE lectura_chat
    ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'JEFE_CARRERA'
        COMMENT 'Rol con el que se leyo: JEFE_CARRERA o COORDINADOR (migracion 018)' AFTER usuario_id,
    ADD CONSTRAINT ck_lectura_chat_rol CHECK (rol IN ('JEFE_CARRERA', 'COORDINADOR')),
    COMMENT = 'Cada lectura de un chat del chatbot hecha por el jefe de carrera o el coordinador (HU-20)';

-- Las filas viejas ya quedaron con JEFE_CARRERA; desde ahora la aplicacion siempre indica el rol.
ALTER TABLE lectura_chat ALTER COLUMN rol DROP DEFAULT;
