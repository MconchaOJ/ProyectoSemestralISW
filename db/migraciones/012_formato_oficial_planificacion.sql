-- =============================================================================
-- Migracion 012 - Planificacion con el formato oficial de la carrera (S-45)
--
-- Motivo (Sprint 2, 2026-11-21):
--   La carrera entrego el 22-sep "FORMATO DE PLANIFICACION DIDACTICA DIARIA.docx".
--   La tabla planificacion seguia con los elementos de la entrevista (objetivo,
--   recursos, tiempos, evaluacion del aprendizaje, interaccion de los participantes).
--   Pasa a los campos del formato oficial:
--     establecimiento, curso, unidad, objetivo de aprendizaje / indicadores de
--     evaluacion, objetivo de aprendizaje actitudinal, objetivo de la clase, fecha,
--     duracion, recursos, instrumentos de evaluacion, y la clase en tres partes
--     (inicio, desarrollo y cierre), cada una con su relato y sus minutos.
--   No se guardan (salen de la inscripcion): nombre del profesor/a (el estudiante),
--   anio y periodo (inscripcion.periodo). "Asignatura: Matematica" es un texto fijo.
--   SUPUESTO S-76: inicio + desarrollo + cierre (minutos) = duracion de la clase.
--
-- Cambios:
--   0. Control previo: si una planificacion ya enviada (no BORRADOR ni
--      CON_OBSERVACIONES) no tiene sus minutos de inicio, desarrollo y cierre
--      legibles en "tiempos" (por ejemplo "Inicio 10 min; desarrollo 60 min;
--      cierre 20 min") o no suman la duracion, la migracion se DETIENE antes de
--      cambiar nada y nombra sus ids: hay que corregirlas a mano. Nunca inventa minutos.
--   1. Columnas nuevas.
--   2. Renombradas: objetivo -> objetivo_clase,
--                   evaluacion_aprendizaje -> instrumentos_evaluacion.
--   3. Copia de lo que habia: los minutos se leen de "tiempos"; "tiempos" e
--      "interaccion_participantes" pasan al relato del desarrollo; lo que el formato
--      anterior no tenia queda, en las planificaciones ya enviadas, con el texto
--      "(registrada con el formato anterior)". Los borradores quedan incompletos.
--   4. Se eliminan tiempos e interaccion_participantes.
--   5. Restricciones: todos los campos obligatorios fuera de BORRADOR y
--      CON_OBSERVACIONES; minutos > 0; la suma de las tres partes = duracion.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/012_formato_oficial_planificacion.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 0. Control previo (no cambia nada; si falla, la migracion se detiene aqui)
DROP PROCEDURE IF EXISTS migracion_012_control;
DELIMITER //
CREATE PROCEDURE migracion_012_control()
BEGIN
    DECLARE malas TEXT;
    SELECT GROUP_CONCAT(id ORDER BY id) INTO malas
      FROM (SELECT id, duracion_minutos,
                   CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)inicio\\D*\\d+'), '\\d+') AS UNSIGNED) AS i,
                   CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)desarrollo\\D*\\d+'), '\\d+') AS UNSIGNED) AS d,
                   CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)cierre\\D*\\d+'), '\\d+') AS UNSIGNED) AS c
              FROM planificacion
             WHERE estado NOT IN ('BORRADOR', 'CON_OBSERVACIONES')) t
     WHERE i IS NULL OR d IS NULL OR c IS NULL OR i = 0 OR d = 0 OR c = 0 OR i + d + c <> duracion_minutos;
    IF malas IS NOT NULL THEN
        -- MESSAGE_TEXT admite 128 caracteres: primero que hacer, al final los ids
        SET malas = LEFT(CONCAT('Migracion 012 detenida sin cambios. Corregir a mano los minutos de tiempos (deben sumar la duracion) en: ', malas), 128);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = malas;
    END IF;
END //
DELIMITER ;
CALL migracion_012_control();
DROP PROCEDURE migracion_012_control;

-- 1. Columnas nuevas (en el orden del formato oficial)
ALTER TABLE planificacion
    ADD COLUMN establecimiento         VARCHAR(150) NULL AFTER titulo,
    ADD COLUMN curso                   VARCHAR(50)  NULL AFTER establecimiento,
    ADD COLUMN unidad                  VARCHAR(200) NULL AFTER curso,
    ADD COLUMN objetivo_aprendizaje    TEXT         NULL COMMENT 'Objetivo de aprendizaje / indicadores de evaluacion' AFTER unidad,
    ADD COLUMN objetivo_actitudinal    TEXT         NULL COMMENT 'Objetivo de aprendizaje actitudinal' AFTER objetivo_aprendizaje,
    ADD COLUMN inicio                  TEXT         NULL COMMENT 'Relato del inicio de la clase' AFTER interaccion_participantes,
    ADD COLUMN inicio_minutos          INT          NULL AFTER inicio,
    ADD COLUMN desarrollo              TEXT         NULL COMMENT 'Relato del desarrollo de la clase' AFTER inicio_minutos,
    ADD COLUMN desarrollo_minutos      INT          NULL AFTER desarrollo,
    ADD COLUMN cierre                  TEXT         NULL COMMENT 'Relato del cierre de la clase' AFTER desarrollo_minutos,
    ADD COLUMN cierre_minutos          INT          NULL AFTER cierre;

-- 2. Renombradas (la restriccion vieja nombra "objetivo": se quita antes)
ALTER TABLE planificacion DROP CHECK ck_planificacion_completa;
ALTER TABLE planificacion
    RENAME COLUMN objetivo TO objetivo_clase,
    RENAME COLUMN evaluacion_aprendizaje TO instrumentos_evaluacion;
ALTER TABLE planificacion
    MODIFY COLUMN objetivo_clase TEXT NULL AFTER objetivo_actitudinal,
    MODIFY COLUMN duracion_minutos INT NULL AFTER fecha_clase,
    MODIFY COLUMN instrumentos_evaluacion TEXT NULL AFTER recursos;

-- 3. Copia de lo que habia
UPDATE planificacion SET
    inicio_minutos     = CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)inicio\\D*\\d+'), '\\d+') AS UNSIGNED),
    desarrollo_minutos = CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)desarrollo\\D*\\d+'), '\\d+') AS UNSIGNED),
    cierre_minutos     = CAST(REGEXP_SUBSTR(REGEXP_SUBSTR(tiempos, '(?i)cierre\\D*\\d+'), '\\d+') AS UNSIGNED),
    desarrollo         = NULLIF(CONCAT_WS(' ',
                             IF(tiempos IS NULL, NULL, CONCAT('Tiempos (formato anterior): ', tiempos)),
                             IF(interaccion_participantes IS NULL, NULL, CONCAT('Interaccion de los participantes: ', interaccion_participantes))), '')
 WHERE tiempos IS NOT NULL OR interaccion_participantes IS NOT NULL;
-- Un borrador puede no tener minutos legibles: se dejan vacios en vez de 0
UPDATE planificacion SET inicio_minutos = NULLIF(inicio_minutos, 0), desarrollo_minutos = NULLIF(desarrollo_minutos, 0),
                         cierre_minutos = NULLIF(cierre_minutos, 0);
UPDATE planificacion SET
    establecimiento      = COALESCE(establecimiento, '(registrada con el formato anterior)'),
    curso                = COALESCE(curso, '(registrada con el formato anterior)'),
    unidad               = COALESCE(unidad, '(registrada con el formato anterior)'),
    objetivo_aprendizaje = COALESCE(objetivo_aprendizaje, '(registrada con el formato anterior)'),
    objetivo_actitudinal = COALESCE(objetivo_actitudinal, '(registrada con el formato anterior)'),
    inicio               = COALESCE(inicio, '(registrada con el formato anterior: ver el desarrollo)'),
    cierre               = COALESCE(cierre, '(registrada con el formato anterior: ver el desarrollo)')
 WHERE estado NOT IN ('BORRADOR', 'CON_OBSERVACIONES');

-- 4. Columnas que el formato oficial no tiene
ALTER TABLE planificacion
    DROP COLUMN tiempos,
    DROP COLUMN interaccion_participantes;

-- 5. Restricciones
ALTER TABLE planificacion
    ADD CONSTRAINT ck_planificacion_minutos CHECK (
        (inicio_minutos IS NULL OR inicio_minutos > 0)
        AND (desarrollo_minutos IS NULL OR desarrollo_minutos > 0)
        AND (cierre_minutos IS NULL OR cierre_minutos > 0)),
    ADD CONSTRAINT ck_planificacion_tiempos CHECK (
        inicio_minutos IS NULL OR desarrollo_minutos IS NULL OR cierre_minutos IS NULL OR duracion_minutos IS NULL
        OR inicio_minutos + desarrollo_minutos + cierre_minutos = duracion_minutos),
    ADD CONSTRAINT ck_planificacion_completa CHECK (
        estado IN ('BORRADOR', 'CON_OBSERVACIONES')
        OR (establecimiento IS NOT NULL AND curso IS NOT NULL AND unidad IS NOT NULL
            AND objetivo_aprendizaje IS NOT NULL AND objetivo_actitudinal IS NOT NULL AND objetivo_clase IS NOT NULL
            AND fecha_clase IS NOT NULL AND duracion_minutos IS NOT NULL AND recursos IS NOT NULL
            AND instrumentos_evaluacion IS NOT NULL
            AND inicio IS NOT NULL AND inicio_minutos IS NOT NULL
            AND desarrollo IS NOT NULL AND desarrollo_minutos IS NOT NULL
            AND cierre IS NOT NULL AND cierre_minutos IS NOT NULL));

ALTER TABLE planificacion COMMENT = 'Plan escrito de una clase, con el formato oficial de la carrera (S-45, migracion 012)';
