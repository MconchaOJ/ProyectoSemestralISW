-- =============================================================================
-- Migracion 009 - base de conocimiento del chatbot (HU-29) y su validacion (HU-30)
--
-- Motivo (Sprint 2, 2026-10-26):
--   HU-29: el coordinador carga los documentos de referencia que el chatbot usa
--   para retroalimentar (rubrica de planificacion, bases curriculares, acciones
--   DUA, banco de comentarios, etc.: la lista "Archivos que deberias cargar" de
--   la propuesta RetroPlani Mate V3). El archivo va a disco (igual que los
--   documentos adjuntos, Fase E); la base guarda su ruta y el TEXTO ya extraido,
--   para no convertir un PDF de cientos de paginas en cada mensaje.
--   HU-30: mientras la validacion con estudiantes esta abierta, el chatbot solo
--   responde a los estudiantes marcados como validadores.
--
-- Cambios:
--   1. Tabla documento_base_conocimiento: un documento vigente por tipo (UNIQUE),
--      con quien lo subio y cuando (criterio 3 de HU-29).
--   2. estudiante.validador_chatbot (HU-30): FALSE por defecto.
--   3. instrucciones_chatbot.validacion_abierta (HU-30): FALSE por defecto. La
--      tabla de la migracion 008 pasa a guardar la configuracion del chatbot.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/009_base_conocimiento_y_validacion.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 1. Base de conocimiento
CREATE TABLE documento_base_conocimiento (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tipo           VARCHAR(40)  NOT NULL COMMENT 'Uno de los 10 documentos de la propuesta RetroPlani Mate',
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco (fuera de git)',
    tipo_mime      VARCHAR(100) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    texto          MEDIUMTEXT   NOT NULL COMMENT 'Texto extraido del Word o PDF al subirlo',
    subido_por_id  BIGINT       NOT NULL,
    fecha_subida   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_base_conocimiento PRIMARY KEY (id),
    CONSTRAINT uq_documento_base_tipo UNIQUE (tipo),
    CONSTRAINT fk_documento_base_subido_por FOREIGN KEY (subido_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_documento_base_tipo CHECK (tipo IN ('MOMENTOS_CLASE', 'RUBRICA_PLANIFICACION',
        'DUA_DIVERSIFICACION', 'BANCO_COMENTARIOS', 'ENLACES_RECURSOS', 'BASES_CURRICULARES_7B_2M',
        'BASES_CURRICULARES_3M_4M', 'ACCIONES_DUA', 'PLANIFICACION_COMENTADA_1', 'PLANIFICACION_COMENTADA_2')),
    CONSTRAINT ck_documento_base_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760)
) ENGINE = InnoDB COMMENT = 'Documentos de referencia del chatbot cargados por el coordinador (HU-29)';

-- 2. Estudiantes validadores
ALTER TABLE estudiante
    ADD COLUMN validador_chatbot BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'TRUE = participa en la validacion del chatbot (HU-30)';

-- 3. Validacion abierta
ALTER TABLE instrucciones_chatbot
    ADD COLUMN validacion_abierta BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'TRUE = el chatbot solo responde a los estudiantes validadores (HU-30)';
