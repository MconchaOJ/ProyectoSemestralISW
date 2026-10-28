-- =============================================================================
-- Migracion 012 - nota final ponderada de la asignatura (HU-09)
--
-- Motivo (Sprint 2, 2026-10-28):
--   HU-09: el sistema calcula la nota final con la estructura evaluativa de la
--   asignatura. El coordinador configura los componentes y sus pesos por
--   asignatura (criterio 1); el profesor del curso registra la nota de cada
--   componente y el sistema guarda la nota ponderada en la inscripcion
--   (criterio 2). La escala 1,0 a 7,0 con un decimal sigue sin confirmar con el
--   cliente (criterio 3, SUPUESTO S-78).
--   La nota calculada va en una columna propia (nota_calculada) y no en
--   nota_final: nota_final sigue siendo la nota de cierre que pone el
--   coordinador, y la base exige que este vacia mientras la inscripcion esta
--   EN_CURSO (ck_inscripcion_nota_cierre).
--
-- Cambios:
--   1. Tabla componente_nota (asignatura, nombre, peso en %, orden).
--   2. Tabla nota_componente (inscripcion, componente, nota 1,0 a 7,0).
--   3. Columna inscripcion.nota_calculada.
--   4. Estructura evaluativa real de Practica Pedagogica (8 componentes, 100 %),
--      tomada de las orientaciones 2026-2 (RESUMEN.md, seccion 3.5).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/012_nota_final_ponderada.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes):
-- los nombres con tilde del paso 4 se escriben con CHAR(... USING utf8mb4).
-- =============================================================================

USE isw_practicas;

CREATE TABLE componente_nota (
    id            INT          NOT NULL AUTO_INCREMENT,
    asignatura_id INT          NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    peso          DECIMAL(5,2) NOT NULL COMMENT 'Porcentaje de la nota final (los de una asignatura suman 100)',
    orden         INT          NOT NULL,
    CONSTRAINT pk_componente_nota PRIMARY KEY (id),
    CONSTRAINT uq_componente_nota_nombre UNIQUE (asignatura_id, nombre),
    CONSTRAINT fk_componente_nota_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT ck_componente_nota_peso CHECK (peso > 0 AND peso <= 100),
    CONSTRAINT ck_componente_nota_orden CHECK (orden >= 1)
) ENGINE = InnoDB COMMENT = 'Estructura evaluativa de cada asignatura: componentes y pesos (HU-09)';

CREATE TABLE nota_componente (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id BIGINT       NOT NULL,
    componente_id  INT          NOT NULL,
    nota           DECIMAL(2,1) NOT NULL,
    registrada_por BIGINT       NOT NULL COMMENT 'Profesor del curso que la registro',
    fecha_registro DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_nota_componente PRIMARY KEY (id),
    CONSTRAINT uq_nota_componente UNIQUE (inscripcion_id, componente_id),
    CONSTRAINT fk_nota_componente_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_nota_componente_componente FOREIGN KEY (componente_id) REFERENCES componente_nota (id),
    CONSTRAINT fk_nota_componente_usuario FOREIGN KEY (registrada_por) REFERENCES usuario (id),
    CONSTRAINT ck_nota_componente_nota CHECK (nota BETWEEN 1.0 AND 7.0)
) ENGINE = InnoDB COMMENT = 'Nota de cada componente de la estructura evaluativa en una inscripcion (HU-09)';

ALTER TABLE inscripcion
    ADD COLUMN nota_calculada DECIMAL(3,1) NULL
        COMMENT 'Nota ponderada con los componentes de la asignatura; nula si falta alguno (HU-09, migracion 012)'
        AFTER nota_final,
    ADD CONSTRAINT ck_inscripcion_nota_calculada CHECK (nota_calculada IS NULL OR nota_calculada BETWEEN 1.0 AND 7.0);

-- Practica Pedagogica (asignatura 5). CHAR(0xC3B3) = o con tilde, CHAR(0xC2BA) = simbolo de ordinal.
INSERT INTO componente_nota (asignatura_id, nombre, peso, orden) VALUES
    (5, 'Especialidad UBB: Producto 1 Expectativas', 10.00, 1),
    (5, 'Especialidad UBB: Producto 2 Contexto', 17.00, 2),
    (5, CONCAT('Colaborador Especialidad: pauta 1.', CHAR(0xC2BA USING utf8mb4), ' observaci', CHAR(0xC3B3 USING utf8mb4), 'n'), 5.00, 3),
    (5, CONCAT('Colaborador Especialidad: pauta 2.', CHAR(0xC2BA USING utf8mb4), ' observaci', CHAR(0xC3B3 USING utf8mb4), 'n'), 13.00, 4),
    (5, CONCAT('Colaborador Orientaci', CHAR(0xC3B3 USING utf8mb4), 'n: pauta 1.', CHAR(0xC2BA USING utf8mb4), ' observaci', CHAR(0xC3B3 USING utf8mb4), 'n'), 3.00, 5),
    (5, CONCAT('Colaborador Orientaci', CHAR(0xC3B3 USING utf8mb4), 'n: pauta 2.', CHAR(0xC2BA USING utf8mb4), ' observaci', CHAR(0xC3B3 USING utf8mb4), 'n'), 5.00, 6),
    (5, CONCAT('Integraci', CHAR(0xC3B3 USING utf8mb4), 'n curricular: TANEP'), 20.00, 7),
    (5, CONCAT('Integraci', CHAR(0xC3B3 USING utf8mb4), 'n curricular: Informe final (Producto 3)'), 27.00, 8);
