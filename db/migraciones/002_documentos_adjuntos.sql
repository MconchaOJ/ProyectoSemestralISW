-- =============================================================================
-- Migracion 002 - documentos adjuntos de planificaciones y evaluaciones
--
-- Motivo (Fase E, 2026-09-21): el estudiante puede subir archivos (Word o PDF,
-- maximo 10 MB) a su planificacion, y el profesor guia puede subir la pauta y
-- evaluacion formal (Word, Excel o PDF) a la evaluacion. NOTA_VOZ ya existia en
-- el esquema desde el Sprint 1 (sin API todavia): esta migracion no la toca.
--
-- Cambios:
--   1. Tabla nueva documento_planificacion (FK a planificacion y a usuario).
--   2. Tabla nueva documento_evaluacion (FK a evaluacion y a usuario).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/002_documentos_adjuntos.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estas tablas.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE documento_planificacion (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    planificacion_id BIGINT      NOT NULL,
    nombre_archivo  VARCHAR(255) NOT NULL,
    ruta_archivo    VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime       VARCHAR(150) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    subido_por      BIGINT       NOT NULL,
    fecha_subida    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_planificacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_planificacion_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT fk_documento_planificacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_planificacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_planificacion_planificacion (planificacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos (Word o PDF, maximo 10 MB) que el estudiante adjunta a su planificacion';

CREATE TABLE documento_evaluacion (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id  BIGINT       NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime      VARCHAR(150) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    subido_por     BIGINT       NOT NULL,
    fecha_subida   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_evaluacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_evaluacion_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id),
    CONSTRAINT fk_documento_evaluacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_evaluacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_evaluacion_evaluacion (evaluacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos (Word, Excel o PDF, maximo 10 MB) con la pauta y evaluacion formal del guia';
