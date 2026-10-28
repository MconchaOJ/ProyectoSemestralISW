-- =============================================================================
-- Migracion 013 - practicas en dupla y en grupo (HU-10)
--
-- Motivo (Sprint 2, 2026-10-28):
--   HU-10: el coordinador agrupa a los estudiantes en duplas (Ambientes de
--   Aprendizaje, Trabajo Colaborativo, Investigacion en el Aula) o en grupos
--   (Aproximacion al Sistema Escolar), segun la modalidad de la asignatura. Una
--   dupla comparte guia, colegio y curso (criterio 1); cada integrante sigue con
--   su propia inscripcion y sus propias evaluaciones (criterio 2, el modelo ya lo
--   cumplia); el historial de cada uno muestra con quien trabajo (criterio 3).
--   El guia sigue en cada inscripcion: el sistema exige que sea el mismo en
--   todos los integrantes al armar el equipo (S-80).
--
-- Cambios:
--   1. Tabla equipo_practica (asignatura, periodo, colegio y curso compartidos).
--   2. Columna inscripcion.equipo_id (nula = trabaja sola o solo).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/013_equipos_dupla_grupo.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE equipo_practica (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    asignatura_id   INT          NOT NULL,
    periodo         VARCHAR(6)   NOT NULL,
    establecimiento VARCHAR(150) NULL COMMENT 'Colegio compartido; nulo si la asignatura no va a un colegio',
    curso           VARCHAR(50)  NULL COMMENT 'Curso compartido, por ejemplo 1 Medio B',
    fecha_creacion  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_equipo_practica PRIMARY KEY (id),
    CONSTRAINT fk_equipo_practica_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT ck_equipo_practica_periodo CHECK (periodo REGEXP '^[0-9]{4}-[12]$'),
    INDEX ix_equipo_practica_asignatura_periodo (asignatura_id, periodo)
) ENGINE = InnoDB COMMENT = 'Dupla o grupo de estudiantes de una misma asignatura y periodo (HU-10)';

ALTER TABLE inscripcion
    ADD COLUMN equipo_id BIGINT NULL
        COMMENT 'Dupla o grupo al que pertenece; nulo = individual (HU-10, migracion 013)'
        AFTER tutor_id,
    ADD CONSTRAINT fk_inscripcion_equipo FOREIGN KEY (equipo_id) REFERENCES equipo_practica (id);
