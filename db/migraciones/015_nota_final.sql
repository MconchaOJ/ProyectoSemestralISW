-- =============================================================================
-- Migracion 015 - Nota de cada evaluacion y estructura evaluativa (HU-09)
--
-- Motivo (Sprint 2, 2026-11-22):
--   HU-09: el sistema calcula la nota final de la asignatura con su estructura
--   evaluativa ("Estructura evaluativa de la Practica Pedagogica", documento de la
--   carrera). Solo una parte de la nota sale de las pautas del sistema (en Practica
--   Pedagogica el 26 %); el resto son componentes externos (productos, informe
--   final, taller TANEP) que califica otro docente y el profesor del curso escribe.
--
-- Cambios:
--   1. evaluacion.nota: nota de 1,0 a 7,0 al 60 % de exigencia sobre el puntaje
--      maximo de la pauta (SUPUESTO S-81), calculada al finalizar y guardada en la
--      evaluacion para que no cambie si la pauta tiene una version nueva despues.
--      Se calcula aqui para las evaluaciones ya finalizadas. ck_evaluacion_nota.
--   2. pauta: indice unico (id, asignatura_id), para la clave foranea compuesta
--      del punto 3 (el componente y su pauta son de la misma asignatura).
--   3. Tabla componente_evaluativo: la estructura de cada asignatura. Tipo PAUTA
--      (con pauta_id) o EXTERNO (sin pauta). La suma de los porcentajes = 100 la
--      comprueba la aplicacion (una CHECK no mira otras filas).
--   4. Tabla nota_componente_externo: la nota de un componente EXTERNO para una
--      inscripcion (una por componente e inscripcion).
--   5. La estructura oficial de Practica Pedagogica (asignatura 5, pautas 10 a 13).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/015_nota_final.sql
-- Para una base nueva NO hace falta: db/schema.sql ya crea todo y db/seed.sql
-- (o db/datos-iniciales.sql en el servidor) carga la estructura.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 1. Nota de cada evaluacion -------------------------------------------------------
ALTER TABLE evaluacion
    ADD COLUMN nota DECIMAL(3,1) NULL COMMENT 'Nota 1,0 a 7,0 al 60 % de exigencia, calculada al finalizar (HU-09, S-81)'
    AFTER promedio;

-- Puntaje maximo = cantidad de indicadores de la pauta x escala_max. Con 60 % de exigencia
-- el 4,0 esta en el 60 % del maximo: bajo eso, 1 + 3 * p / (0,6 * max); desde ahi, 4 + 3 * (p - 0,6 * max) / (0,4 * max).
UPDATE evaluacion e
  JOIN pauta p ON p.id = e.pauta_id
  JOIN (SELECT d.pauta_id, COUNT(*) AS n
          FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id
         GROUP BY d.pauta_id) c ON c.pauta_id = e.pauta_id
   SET e.nota = ROUND(CASE
           WHEN e.puntaje_total < 0.6 * c.n * p.escala_max
               THEN 1 + 3 * e.puntaje_total / (0.6 * c.n * p.escala_max)
           ELSE 4 + 3 * (e.puntaje_total - 0.6 * c.n * p.escala_max) / (0.4 * c.n * p.escala_max)
       END, 1)
 WHERE e.estado = 'FINALIZADA';

ALTER TABLE evaluacion
    ADD CONSTRAINT ck_evaluacion_nota CHECK (
        (estado = 'BORRADOR' AND nota IS NULL)
     OR (estado = 'FINALIZADA' AND nota IS NOT NULL AND nota BETWEEN 1.0 AND 7.0));

-- 2. Indice para la clave foranea compuesta ------------------------------------------
ALTER TABLE pauta ADD CONSTRAINT uq_pauta_id_asignatura UNIQUE (id, asignatura_id);

-- 3. Estructura evaluativa -----------------------------------------------------------
CREATE TABLE componente_evaluativo (
    id             INT          NOT NULL AUTO_INCREMENT,
    asignatura_id  INT          NOT NULL,
    tipo           VARCHAR(10)  NOT NULL COMMENT 'PAUTA (nota de las evaluaciones con esa pauta) o EXTERNO (la escribe el profesor del curso)',
    nombre         VARCHAR(150) NOT NULL,
    pauta_id       INT          NULL     COMMENT 'Solo en los de tipo PAUTA: la version vigente de la pauta',
    porcentaje     INT          NOT NULL COMMENT 'Peso en la nota final; los de una asignatura suman 100 (lo comprueba la aplicacion)',
    orden          INT          NOT NULL,
    CONSTRAINT pk_componente_evaluativo PRIMARY KEY (id),
    CONSTRAINT uq_componente_asignatura_pauta UNIQUE (asignatura_id, pauta_id),
    CONSTRAINT fk_componente_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT fk_componente_pauta FOREIGN KEY (pauta_id, asignatura_id) REFERENCES pauta (id, asignatura_id),
    CONSTRAINT ck_componente_tipo CHECK (tipo IN ('PAUTA', 'EXTERNO')),
    CONSTRAINT ck_componente_pauta CHECK ((tipo = 'PAUTA') = (pauta_id IS NOT NULL)),
    CONSTRAINT ck_componente_porcentaje CHECK (porcentaje BETWEEN 1 AND 100)
) ENGINE = InnoDB COMMENT = 'Estructura evaluativa de una asignatura: componentes y su peso (HU-09)';

-- 4. Notas de los componentes externos ----------------------------------------------
CREATE TABLE nota_componente_externo (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id  BIGINT       NOT NULL,
    componente_id   INT          NOT NULL,
    nota            DECIMAL(3,1) NOT NULL,
    registrada_por  BIGINT       NOT NULL COMMENT 'Profesor del curso o coordinador que la escribio',
    fecha_registro  DATETIME     NOT NULL,
    CONSTRAINT pk_nota_componente_externo PRIMARY KEY (id),
    CONSTRAINT uq_nota_externa_inscripcion_componente UNIQUE (inscripcion_id, componente_id),
    CONSTRAINT fk_nota_externa_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_nota_externa_componente FOREIGN KEY (componente_id) REFERENCES componente_evaluativo (id),
    CONSTRAINT fk_nota_externa_usuario FOREIGN KEY (registrada_por) REFERENCES usuario (id),
    CONSTRAINT ck_nota_externa_rango CHECK (nota BETWEEN 1.0 AND 7.0)
) ENGINE = InnoDB COMMENT = 'Nota de un componente externo de la estructura para una inscripcion (HU-09)';

-- 5. Estructura oficial de Practica Pedagogica (asignatura 5) -------------------------
-- Las tildes van como literales hexadecimales UTF-8 para que el archivo siga en ASCII.
INSERT INTO componente_evaluativo (asignatura_id, tipo, nombre, pauta_id, porcentaje, orden) VALUES
    (5, 'EXTERNO', 'Producto 1: Texto Expectativas', NULL, 10, 1),
    (5, 'EXTERNO', 'Producto 2: Informe escrito Contexto', NULL, 17, 2),
    (5, 'PAUTA', CONCAT('Pauta de Evaluaci', _utf8mb4 X'C3B3', 'n 1 Especialidad'), 10, 5, 3),
    (5, 'PAUTA', CONCAT('Pauta de Evaluaci', _utf8mb4 X'C3B3', 'n 2 Especialidad'), 11, 13, 4),
    (5, 'PAUTA', CONCAT('Pauta de Evaluaci', _utf8mb4 X'C3B3', 'n 1 Orientaci', _utf8mb4 X'C3B3', 'n'), 12, 3, 5),
    (5, 'PAUTA', CONCAT('Pauta de Evaluaci', _utf8mb4 X'C3B3', 'n 2 Orientaci', _utf8mb4 X'C3B3', 'n'), 13, 5, 6),
    (5, 'EXTERNO', CONCAT('Informe de An', _utf8mb4 X'C3A1', 'lisis de Experiencias Pedag', _utf8mb4 X'C3B3', 'gicas y Pauta del Taller (TANEP)'), NULL, 20, 7),
    (5, 'EXTERNO', CONCAT('Producto 3: Informe Final de Pr', _utf8mb4 X'C3A1', 'cticas'), NULL, 27, 8);

-- Si en esta base las pautas 10 a 13 no son las de Practica Pedagogica, la clave foranea
-- compuesta hace fallar el INSERT: revisar los ids con
--   SELECT id, asignatura_id, nombre FROM pauta WHERE asignatura_id = 5;
