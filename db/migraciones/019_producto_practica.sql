-- =============================================================================
-- Migracion 019 - Informes y productos de la practica (HU-18)
--
-- Motivo (Sprint 2, 2026-11-22): el estudiante sube su informe final, su diario
-- de reflexion, su proyecto de intervencion u otro producto (Word o PDF, maximo
-- 10 MB) a su inscripcion, para que queden en su recorrido (historial).
--
-- SUPUESTO S-93: solo el estudiante duenio sube, reemplaza o borra, y solo con la
--   inscripcion EN_CURSO (la aplicacion responde 409 si no).
-- SUPUESTO S-94: varios archivos por tipo y todos los tipos en las 6 practicas.
--
-- Cambios:
--   1. Tabla nueva producto_practica (FK a inscripcion y a usuario).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/019_producto_practica.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye esta tabla.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE producto_practica (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id  BIGINT       NOT NULL,
    tipo            VARCHAR(30)  NOT NULL COMMENT 'INFORME_FINAL, DIARIO_REFLEXION, PROYECTO_INTERVENCION u OTRO',
    nombre_archivo  VARCHAR(255) NOT NULL,
    ruta_archivo    VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime       VARCHAR(150) NOT NULL,
    tamano_bytes    BIGINT       NOT NULL,
    subido_por      BIGINT       NOT NULL,
    fecha_subida    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_producto_practica PRIMARY KEY (id),
    CONSTRAINT fk_producto_practica_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_producto_practica_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_producto_practica_tipo CHECK (tipo IN ('INFORME_FINAL', 'DIARIO_REFLEXION', 'PROYECTO_INTERVENCION', 'OTRO')),
    CONSTRAINT ck_producto_practica_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_producto_practica_inscripcion (inscripcion_id)
) ENGINE = InnoDB COMMENT = 'Informes y productos (Word o PDF, maximo 10 MB) que el estudiante sube a su inscripcion (HU-18)';
