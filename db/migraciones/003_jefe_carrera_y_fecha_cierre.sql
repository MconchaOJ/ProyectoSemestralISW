-- =============================================================================
-- Migracion 003 - rol JEFE_CARRERA y fecha de cierre de la inscripcion (HU-31)
--
-- Motivo (Sprint 2, 2026-10-10): la cartola de practicas (HU-31) es un reporte
-- en PDF para el jefe de carrera con las inscripciones cerradas en un rango de
-- fechas (evidencia para el criterio 3 de acreditacion). El equipo decidio que
-- el jefe de carrera es un ROL PROPIO y de solo lectura, no el coordinador
-- (resuelve la pregunta abierta en S-38). Hasta ahora la inscripcion no guardaba
-- cuando se cerro, asi que no se podia filtrar por fecha.
--
-- Cambios:
--   1. Rol nuevo JEFE_CARRERA (id 5) en el catalogo rol.
--   2. Columna nueva inscripcion.fecha_cierre (DATE, nula mientras esta EN_CURSO).
--   3. Las inscripciones ya cerradas reciben una fecha de cierre deducida de su
--      periodo. SUPUESTO S-48: el semestre 1 cierra el 31 de julio y el 2 el 31
--      de diciembre (solo para datos anteriores a esta migracion; desde ahora la
--      aplicacion guarda la fecha real del dia en que se cierra).
--   4. CHECK: una inscripcion EN_CURSO no puede tener fecha de cierre.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/003_jefe_carrera_y_fecha_cierre.sql
-- Para una base nueva NO hace falta: db/schema.sql y db/seed.sql ya incluyen estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

INSERT INTO rol (id, nombre, descripcion) VALUES
    (5, 'JEFE_CARRERA', 'Jefe de carrera: solo lectura, consulta la cartola de practicas (evidencia de acreditacion)');

ALTER TABLE inscripcion
    ADD COLUMN fecha_cierre DATE NULL COMMENT 'Dia en que se cerro (APROBADA o REPROBADA); nulo mientras esta EN_CURSO'
        AFTER nota_final;

UPDATE inscripcion
SET fecha_cierre = IF(RIGHT(periodo, 1) = '1',
                      CONCAT(LEFT(periodo, 4), '-07-31'),
                      CONCAT(LEFT(periodo, 4), '-12-31'))
WHERE estado <> 'EN_CURSO' AND fecha_cierre IS NULL;

ALTER TABLE inscripcion
    ADD CONSTRAINT ck_inscripcion_fecha_cierre CHECK (estado <> 'EN_CURSO' OR fecha_cierre IS NULL),
    ADD INDEX ix_inscripcion_fecha_cierre (fecha_cierre);
