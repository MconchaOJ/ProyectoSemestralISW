-- =============================================================================
-- Migracion 001 - modalidad GRUPO y "la asignatura usa planificaciones"
--
-- Motivo (respuestas de un estudiante de PEM, 2026-09-20; pendiente de confirmar con el cliente):
--   * Aproximacion al Sistema Escolar se hace en GRUPOS DE 4 y no entrega planificaciones
--     (solo trabajos hechos en clase).
--   * Ambientes de Aprendizaje se hace en PAREJAS y SI entrega planificaciones.
--   * Ninguna de las dos va a un colegio: no existe profesor colaborador (guia).
--
-- Cambios:
--   1. Nueva modalidad GRUPO (el CHECK solo admitia INDIVIDUAL y DUPLA).
--   2. Nueva columna asignatura_practica.requiere_planificacion (TRUE salvo Aproximacion).
--   3. Datos: P1 pasa a GRUPO sin planificaciones; P2 pasa a DUPLA.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/001_grupo_y_planificacion_por_asignatura.sql
-- Para una base nueva NO hace falta: db/schema.sql y db/seed.sql ya incluyen estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

ALTER TABLE asignatura_practica DROP CHECK ck_asignatura_modalidad;

ALTER TABLE asignatura_practica
    MODIFY COLUMN modalidad VARCHAR(20) NOT NULL COMMENT 'INDIVIDUAL, DUPLA, GRUPO';

ALTER TABLE asignatura_practica
    ADD CONSTRAINT ck_asignatura_modalidad CHECK (modalidad IN ('INDIVIDUAL', 'DUPLA', 'GRUPO'));

ALTER TABLE asignatura_practica
    ADD COLUMN requiere_planificacion BOOLEAN NOT NULL DEFAULT TRUE
        COMMENT 'FALSE si la asignatura no entrega planificaciones de clase (Aproximacion al Sistema Escolar)'
        AFTER chatbot_habilitado;

UPDATE asignatura_practica SET modalidad = 'GRUPO', requiere_planificacion = FALSE WHERE codigo = 'PEM-P1';
UPDATE asignatura_practica SET modalidad = 'DUPLA' WHERE codigo = 'PEM-P2';
