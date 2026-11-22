-- =============================================================================
-- Migracion 017 - La exigencia de la escala de notas es un parametro (HU-27, S-81)
--
-- Motivo (Sprint 2, 2026-11-22):
--   La nota de cada evaluacion (HU-09) se calcula con 60 % de exigencia, el valor
--   de la escala impresa en las pautas oficiales. Pasa a ser un parametro del
--   sistema para poder ajustarlo sin tocar el codigo. Lo editan el coordinador y
--   (solo este parametro) el jefe de carrera; eso lo comprueba la aplicacion.
--   Rango permitido 50 a 70 % (SUPUESTO S-92). Un cambio rige para las
--   evaluaciones que se finalicen despues: la nota de una evaluacion ya
--   finalizada esta guardada en evaluacion.nota y no cambia.
--
-- Cambios: solo datos. Una fila nueva en parametro_sistema con el valor de hoy (60).
--
-- Se ejecuta UNA sola vez y como root, despues de la 014:
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/017_exigencia_parametro.sql
-- Para una base nueva NO hace falta: db/seed.sql (o db/datos-iniciales.sql) ya lo carga.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes): la descripcion
-- queda sin tildes en una base migrada.
-- =============================================================================

USE isw_practicas;

INSERT INTO parametro_sistema (clave, valor, minimo, maximo, unidad, descripcion) VALUES
    ('evaluacion.exigencia_pct', 60, 50, 70, '%',
        'Exigencia de la escala de notas: porcentaje del puntaje maximo de la pauta que corresponde al 4,0 (S-81)');
