-- =============================================================================
-- Migracion 006 - tutor de Practica Profesional y numero de observacion (HU-07)
--
-- Motivo (Sprint 2, 2026-10-16): en Practica Profesional, ademas del profesor
-- colaborador (guia), un TUTOR contratado por la universidad observa y evalua
-- al estudiante con sus propias pautas (Especialidad y Orientacion, primera y
-- segunda observacion). Las pautas de la primera y la segunda observacion son
-- instrumentos distintos (133/126 y 161 puntos), por eso el numero de
-- observacion es un dato de la PAUTA (supuesto S-55).
--
-- Cambios:
--   1. Rol nuevo TUTOR (id 6).
--   2. asignatura_practica.lleva_tutor: solo Practica Profesional (PEM-P6).
--   3. inscripcion.tutor_id: tutor de la inscripcion (nulo = sin tutor), con FK,
--      CHECK (distinto del guia y del profesor del curso) e indice.
--   4. pauta.numero_observacion: 1 o 2 en las pautas de Especialidad y
--      Orientacion; nulo en la pauta de observacion de clase (CHECK).
--   Las pautas reales y el tutor de demostracion NO van aqui: estan en
--   db/seed.sql (desarrollo). En el servidor el coordinador registra al tutor.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/006_tutor_y_numero_de_observacion.sql
-- Para una base nueva NO hace falta: db/schema.sql, db/seed.sql y
-- db/datos-iniciales.sql ya incluyen estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

-- 1. Rol
INSERT INTO rol (id, nombre, descripcion) VALUES
    (6, 'TUTOR', 'Tutor de la universidad: observa y evalua la Practica Profesional con sus propias pautas');

-- 2. Asignaturas con tutor
ALTER TABLE asignatura_practica
    ADD COLUMN lleva_tutor BOOLEAN NOT NULL DEFAULT FALSE
        COMMENT 'TRUE si la practica tiene tutor de la universidad (solo Practica Profesional, HU-07)'
        AFTER requiere_planificacion,
    ADD CONSTRAINT ck_asignatura_tutor CHECK (lleva_tutor = FALSE OR requiere_centro_educativo = TRUE);
UPDATE asignatura_practica SET lleva_tutor = TRUE WHERE codigo = 'PEM-P6';

-- 3. Tutor de la inscripcion
ALTER TABLE inscripcion
    ADD COLUMN tutor_id BIGINT NULL COMMENT 'Tutor de la universidad (solo Practica Profesional); nulo = sin tutor'
        AFTER profesor_curso_id,
    ADD CONSTRAINT fk_inscripcion_tutor FOREIGN KEY (tutor_id) REFERENCES usuario (id),
    ADD CONSTRAINT ck_inscripcion_tutor_distinto CHECK (tutor_id IS NULL
        OR (tutor_id <> profesor_curso_id AND (profesor_guia_id IS NULL OR tutor_id <> profesor_guia_id))),
    ADD INDEX ix_inscripcion_tutor (tutor_id);

-- 4. Numero de observacion de la pauta
ALTER TABLE pauta
    ADD COLUMN numero_observacion INT NULL
        COMMENT '1 o 2: primera o segunda observacion (Especialidad y Orientacion, HU-07); nulo en la observacion de clase'
        AFTER ambito;
-- Pautas de Especialidad u Orientacion creadas antes con HU-06 (sin numero): quedan como primera observacion.
-- El coordinador puede corregirlo editandola (crea una version nueva si ya se uso).
UPDATE pauta SET numero_observacion = 1 WHERE ambito <> 'OBSERVACION_CLASE' AND numero_observacion IS NULL;
ALTER TABLE pauta
    ADD CONSTRAINT ck_pauta_numero_observacion CHECK (
        (ambito = 'OBSERVACION_CLASE' AND numero_observacion IS NULL)
     OR (ambito <> 'OBSERVACION_CLASE' AND numero_observacion IS NOT NULL AND numero_observacion IN (1, 2)));
