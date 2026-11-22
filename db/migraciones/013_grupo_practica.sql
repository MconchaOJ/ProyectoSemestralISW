-- =============================================================================
-- Migracion 013 - Duplas y grupos de practica (HU-10)
--
-- Motivo (Sprint 2, 2026-11-21):
--   HU-10: Ambientes de Aprendizaje, Trabajo Colaborativo e Investigacion en el Aula
--   se hacen en DUPLA y Aproximacion al Sistema Escolar en GRUPO de 4
--   (asignatura_practica.modalidad). Hasta ahora cada inscripcion era individual.
--   Un grupo junta inscripciones de la MISMA asignatura y periodo; cada integrante
--   conserva su inscripcion, su pauta (S-11) y sus planificaciones (SUPUESTO S-77:
--   el grupo solo agrupa, no hay planificacion compartida). En las practicas que van
--   a un colegio, el grupo guarda el establecimiento y el curso que comparte (criterio 1).
--
-- Cambios:
--   1. Tabla grupo_practica (asignatura, periodo, establecimiento y curso).
--   2. Columna inscripcion.grupo_id (nula = sin grupo). La clave foranea es compuesta
--      (grupo_id, asignatura_id, periodo) -> grupo_practica (id, asignatura_id, periodo):
--      la base impide que una inscripcion entre a un grupo de otra asignatura o periodo.
--   Las demas reglas (dupla = 2, grupo = 2 a 4, mismo guia, solo EN_CURSO, no en una
--   practica INDIVIDUAL) las aplica la aplicacion (S-78).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/013_grupo_practica.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 1. Grupos (duplas y grupos de 4)
CREATE TABLE grupo_practica (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    asignatura_id   INT          NOT NULL,
    periodo         VARCHAR(6)   NOT NULL COMMENT 'Ej. 2026-2',
    establecimiento VARCHAR(150) NULL     COMMENT 'Colegio que comparten (solo practicas que van a un colegio)',
    curso           VARCHAR(50)  NULL     COMMENT 'Curso del colegio que comparten',
    fecha_creacion  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_grupo_practica PRIMARY KEY (id),
    -- Destino de la clave foranea compuesta de inscripcion
    CONSTRAINT uq_grupo_practica_asignatura_periodo UNIQUE (id, asignatura_id, periodo),
    CONSTRAINT fk_grupo_practica_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT ck_grupo_practica_periodo CHECK (periodo REGEXP '^[0-9]{4}-[12]$'),
    INDEX ix_grupo_practica_asignatura_periodo (asignatura_id, periodo)
) ENGINE = InnoDB COMMENT = 'Dupla o grupo de practica: junta inscripciones de la misma asignatura y periodo (HU-10)';

-- 2. Integrantes: la inscripcion apunta a su grupo
ALTER TABLE inscripcion
    ADD COLUMN grupo_id BIGINT NULL COMMENT 'Dupla o grupo (HU-10, migracion 013); nulo = sin grupo' AFTER tutor_id,
    ADD CONSTRAINT fk_inscripcion_grupo FOREIGN KEY (grupo_id, asignatura_id, periodo)
        REFERENCES grupo_practica (id, asignatura_id, periodo);
