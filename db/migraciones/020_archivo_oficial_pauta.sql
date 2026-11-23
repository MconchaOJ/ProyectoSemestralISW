-- =============================================================================
-- Migracion 020 - Archivos oficiales de las pautas (HU-25)
--
-- Motivo (Sprint 2, 2026-11-22): el coordinador sube el documento formal de cada
-- pauta (PDF o Word firmado y timbrado) y lo puede reemplazar. Cada subida es una
-- fila nueva con el numero siguiente (version 1, 2, 3...): la de numero mayor es
-- la vigente y las anteriores quedan como historial (quien y cuando).
--
-- SUPUESTO S-95: se aceptan PDF o Word (.pdf, .docx), con el tamanio maximo vigente.
-- SUPUESTO S-96: el archivo va con cada version de la pauta (HU-06). Una version
--   nueva de la pauta NO hereda el archivo de la anterior; el de la anterior queda.
-- Borrar una pauta sin uso borra tambien sus archivos oficiales.
--
-- Cambios:
--   1. Tabla nueva archivo_oficial_pauta (FK a pauta y a usuario).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/020_archivo_oficial_pauta.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye esta tabla.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE archivo_oficial_pauta (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    pauta_id        INT          NOT NULL,
    version         INT          NOT NULL COMMENT '1, 2, 3...: la mayor es la vigente',
    nombre_archivo  VARCHAR(255) NOT NULL,
    ruta_archivo    VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime       VARCHAR(150) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    subido_por      BIGINT       NOT NULL,
    fecha_subida    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_archivo_oficial_pauta PRIMARY KEY (id),
    CONSTRAINT uq_archivo_oficial_pauta_version UNIQUE (pauta_id, version),
    CONSTRAINT fk_archivo_oficial_pauta_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT fk_archivo_oficial_pauta_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_archivo_oficial_pauta_version CHECK (version >= 1),
    CONSTRAINT ck_archivo_oficial_pauta_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760)
) ENGINE = InnoDB COMMENT = 'Archivos oficiales (PDF o Word firmados) de cada pauta, con su historial (HU-25)';
