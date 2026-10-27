-- =============================================================================
-- Migracion 011 - Escala SUS y rubrica de juicio experto del chatbot (HU-30)
--
-- Motivo (Sprint 2, 2026-10-26):
--   HU-30, criterio 2: las respuestas de la validacion quedan en el sistema (antes
--   se proponia un formulario externo, S-71, que se descarta). Los instrumentos
--   son los de la propuesta RetroPlani Mate V3 (Documents/ChatBot (1).rar):
--   - Escala SUS adaptada al chatbot: 10 items de 1 a 5 (Sevilla-Gonzalez et al.,
--     2020). El puntaje SUS (0 a 100) lo calcula la base con la formula estandar
--     de Brooke: impares (respuesta - 1), pares (5 - respuesta), suma x 2,5.
--   - Rubrica de juicio experto: 9 criterios de 1 (logro insuficiente) a 4 (logro
--     alto), con comentario opcional, y 4 preguntas abiertas.
--   Responde cada estudiante validador (estudiante.validador_chatbot, migracion
--   009) una sola vez cada instrumento (UNIQUE). SUPUESTO S-75.
--
-- Cambios:
--   1. Tabla respuesta_sus (una fila por validador, 10 items y puntaje calculado).
--   2. Tabla respuesta_rubrica_experto (una por validador, preguntas abiertas).
--   3. Tabla respuesta_rubrica_criterio (los 9 criterios de cada rubrica).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/011_escala_sus_y_rubrica_experto.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 1. Escala SUS
CREATE TABLE respuesta_sus (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    validador_id  BIGINT       NOT NULL COMMENT 'Estudiante validador que respondio',
    item1         INT          NOT NULL,
    item2         INT          NOT NULL,
    item3         INT          NOT NULL,
    item4         INT          NOT NULL,
    item5         INT          NOT NULL,
    item6         INT          NOT NULL,
    item7         INT          NOT NULL,
    item8         INT          NOT NULL,
    item9         INT          NOT NULL,
    item10        INT          NOT NULL,
    puntaje       DECIMAL(5,2) GENERATED ALWAYS AS (((item1 - 1) + (5 - item2) + (item3 - 1) + (5 - item4)
                      + (item5 - 1) + (5 - item6) + (item7 - 1) + (5 - item8) + (item9 - 1) + (5 - item10)) * 2.5) STORED
                      COMMENT 'Puntaje SUS de 0 a 100 (formula de Brooke)',
    fecha         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_respuesta_sus PRIMARY KEY (id),
    CONSTRAINT uq_respuesta_sus_validador UNIQUE (validador_id),
    CONSTRAINT fk_respuesta_sus_validador FOREIGN KEY (validador_id) REFERENCES estudiante (id),
    CONSTRAINT ck_respuesta_sus_items CHECK (item1 BETWEEN 1 AND 5 AND item2 BETWEEN 1 AND 5 AND item3 BETWEEN 1 AND 5
        AND item4 BETWEEN 1 AND 5 AND item5 BETWEEN 1 AND 5 AND item6 BETWEEN 1 AND 5 AND item7 BETWEEN 1 AND 5
        AND item8 BETWEEN 1 AND 5 AND item9 BETWEEN 1 AND 5 AND item10 BETWEEN 1 AND 5)
) ENGINE = InnoDB COMMENT = 'Escala SUS adaptada al chatbot, una por validador (HU-30)';

-- 2. Rubrica de juicio experto
CREATE TABLE respuesta_rubrica_experto (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    validador_id  BIGINT   NOT NULL COMMENT 'Estudiante validador que respondio',
    fortalezas    TEXT     NULL COMMENT 'Pregunta abierta 1',
    debilidades   TEXT     NULL COMMENT 'Pregunta abierta 2',
    mejoras       TEXT     NULL COMMENT 'Pregunta abierta 3',
    recomendacion TEXT     NULL COMMENT 'Pregunta abierta 4',
    fecha         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_respuesta_rubrica_experto PRIMARY KEY (id),
    CONSTRAINT uq_respuesta_rubrica_validador UNIQUE (validador_id),
    CONSTRAINT fk_respuesta_rubrica_validador FOREIGN KEY (validador_id) REFERENCES estudiante (id)
) ENGINE = InnoDB COMMENT = 'Rubrica de juicio experto del chatbot, una por validador (HU-30)';

-- 3. Los 9 criterios de cada rubrica
CREATE TABLE respuesta_rubrica_criterio (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    respuesta_id BIGINT        NOT NULL,
    numero       INT           NOT NULL COMMENT 'Criterio 1 a 9 de la rubrica',
    puntaje      INT           NOT NULL COMMENT '1 logro insuficiente, 2 parcial, 3 adecuado, 4 alto',
    comentario   VARCHAR(1000) NULL,
    CONSTRAINT pk_respuesta_rubrica_criterio PRIMARY KEY (id),
    CONSTRAINT uq_rubrica_criterio_numero UNIQUE (respuesta_id, numero),
    CONSTRAINT fk_rubrica_criterio_respuesta FOREIGN KEY (respuesta_id) REFERENCES respuesta_rubrica_experto (id),
    CONSTRAINT ck_rubrica_criterio_numero CHECK (numero BETWEEN 1 AND 9),
    CONSTRAINT ck_rubrica_criterio_puntaje CHECK (puntaje BETWEEN 1 AND 4)
) ENGINE = InnoDB COMMENT = 'Puntaje y comentario de cada criterio de la rubrica de juicio experto (HU-30)';
