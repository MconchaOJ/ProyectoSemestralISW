-- =============================================================================
-- Migracion 005 - versiones de pauta (HU-06, configurar pautas de evaluacion)
--
-- Motivo (Sprint 2, 2026-10-11): el coordinador crea y edita pautas desde la
-- aplicacion. Criterio 2 de HU-06: si se modifica una pauta que ya se uso, se
-- crea una VERSION NUEVA y las evaluaciones anteriores conservan la suya (sus
-- indicadores, su escala y por lo tanto su puntaje maximo). Por eso una pauta
-- usada nunca se edita ni se borra: la version nueva es otra fila de pauta que
-- apunta a la anterior.
--
-- Cambios:
--   1. pauta.version (1, 2, 3...) y pauta.version_anterior_id (nula en la 1).
--   2. pauta.fecha_creacion.
--   3. La unicidad pasa de (asignatura, nombre) a (asignatura, nombre, version).
--   4. Una version tiene a lo mas UNA version siguiente (UNIQUE version_anterior_id)
--      y la version 1 es justo la que no tiene anterior (CHECK).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/005_versiones_de_pauta.sql
-- En el servidor Linux: sudo mysql < db/migraciones/005_versiones_de_pauta.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- Las pautas que ya existen quedan como version 1. Solo ASCII a proposito.
-- =============================================================================

USE isw_practicas;

ALTER TABLE pauta
    ADD COLUMN version INT NOT NULL DEFAULT 1 COMMENT 'Version de la pauta (HU-06): 1, 2, 3...' AFTER nombre,
    ADD COLUMN version_anterior_id INT NULL COMMENT 'Version que esta reemplaza; nula en la version 1' AFTER version,
    ADD COLUMN fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT uq_pauta_asignatura_nombre_version UNIQUE (asignatura_id, nombre, version),
    DROP INDEX uq_pauta_asignatura_nombre,
    ADD CONSTRAINT uq_pauta_version_anterior UNIQUE (version_anterior_id),
    ADD CONSTRAINT fk_pauta_version_anterior FOREIGN KEY (version_anterior_id) REFERENCES pauta (id),
    ADD CONSTRAINT ck_pauta_version CHECK (version >= 1 AND ((version = 1) = (version_anterior_id IS NULL)));
