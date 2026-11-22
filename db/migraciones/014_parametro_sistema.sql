-- =============================================================================
-- Migracion 014 - Parametros del sistema editables por el coordinador (HU-27)
--
-- Motivo (Sprint 2, 2026-11-22):
--   HU-27: reglas operativas que estaban fijas en application.properties o en el
--   codigo pasan a editarse desde la pantalla "Parametros del sistema" (solo el
--   coordinador) y rigen desde la peticion siguiente. Cada parametro guarda su
--   rango permitido (SUPUESTO S-80) y quien lo cambio por ultima vez y cuando.
--   Los tamanos maximos de archivo solo se pueden BAJAR: el techo de 10 MB de los
--   documentos lo fija tambien la base (ck_documento_*_tamano) y el de 25 MB del
--   audio queda bajo el limite de subida de Spring (30 MB).
--
-- Cambios:
--   1. Tabla parametro_sistema (clave, valor, minimo, maximo, unidad, descripcion,
--      editado_por, fecha_edicion) con ck_parametro_rango.
--   2. Los 4 parametros con sus valores actuales.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/014_parametro_sistema.sql
-- Para una base nueva NO hace falta: db/schema.sql ya crea la tabla y db/seed.sql
-- (o db/datos-iniciales.sql en el servidor) carga los 4 parametros.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE parametro_sistema (
    clave          VARCHAR(50)  NOT NULL,
    valor          INT          NOT NULL,
    minimo         INT          NOT NULL,
    maximo         INT          NOT NULL,
    unidad         VARCHAR(20)  NOT NULL COMMENT 'dias o MB',
    descripcion    VARCHAR(300) NOT NULL,
    editado_por    BIGINT       NULL     COMMENT 'Coordinador que lo cambio por ultima vez; nulo = valor inicial',
    fecha_edicion  DATETIME     NULL,
    CONSTRAINT pk_parametro_sistema PRIMARY KEY (clave),
    CONSTRAINT fk_parametro_editado_por FOREIGN KEY (editado_por) REFERENCES usuario (id),
    CONSTRAINT ck_parametro_rango CHECK (minimo <= maximo AND valor BETWEEN minimo AND maximo)
) ENGINE = InnoDB COMMENT = 'Reglas operativas editables por el coordinador (HU-27)';

-- La unidad 'dias' va como literal hexadecimal UTF-8 para que la pantalla muestre la tilde; las descripciones
-- quedan sin tildes en una base migrada (en una instalacion nueva vienen de seed.sql / datos-iniciales.sql).
INSERT INTO parametro_sistema (clave, valor, minimo, maximo, unidad, descripcion) VALUES
    ('planificacion.dias_anticipacion', 3, 0, 14, _utf8mb4 X'64C3AD6173',   -- 'dias' con tilde (el archivo se mantiene en ASCII)
        'Dias minimos entre enviar una planificacion a revision y la fecha de la clase (S-35)'),
    ('panel.dias_atencion', 3, 1, 30, _utf8mb4 X'64C3AD6173',   -- 'dias' con tilde (el archivo se mantiene en ASCII)
        'Dias que puede esperar revision una planificacion antes de aparecer en "requiere atencion" del panel (S-79)'),
    ('archivos.documento_mb', 10, 1, 10, 'MB',
        'Tamano maximo de un documento adjunto (planificacion, evaluacion o base de conocimiento)'),
    ('archivos.audio_mb', 25, 1, 25, 'MB',
        'Tamano maximo del audio de una nota de voz');
