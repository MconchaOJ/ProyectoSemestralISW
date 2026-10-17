-- =============================================================================
-- Migracion 004 - segundo rol opcional para cuentas especiales ("birol")
--
-- Motivo (Sprint 2, 2026-10-11, CONFIRMADO POR EL CLIENTE): la misma persona que
-- es jefe de carrera tambien evalua practicas y acompana estudiantes como
-- profesor guia. Su cuenta vive con el RUT y la clave de la universidad, asi que
-- no puede tener dos cuentas separadas. Deja sin efecto el supuesto S-11 ("un
-- solo rol por usuario") para estas cuentas especiales.
--
-- A proposito NO es un sistema general de N roles: un segundo rol como maximo.
-- Los permisos de la cuenta son la SUMA de lo que da cada uno de sus dos roles.
--
-- Cambios:
--   1. Columna nueva usuario.rol_secundario_id (nula = cuenta normal, un rol).
--   2. FK al catalogo rol y CHECK: el segundo rol no puede repetir el principal.
--   3. Demo: el jefe de carrera (id 32) pasa a tener PROFESOR_GUIA (rol 2) como
--      segundo rol y queda como guia de la inscripcion 40 (Trabajo
--      Colaborativo 2026-2, antes del guia 13). La evaluacion FINALIZADA y la
--      revision que ya hizo ese guia quedan a su nombre (historico).
--      Este punto 3 es SOLO para la base de desarrollo/demo: en el servidor no
--      se ejecuta (ahi el coordinador asigna guias desde la aplicacion).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/004_rol_secundario.sql
-- Para una base nueva NO hace falta: db/schema.sql y db/seed.sql ya incluyen estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

ALTER TABLE usuario
    ADD COLUMN rol_secundario_id INT NULL
        COMMENT 'Segundo rol opcional (cuentas especiales, ej. jefe de carrera que tambien es guia). Nulo = un solo rol'
        AFTER rol_id,
    ADD CONSTRAINT fk_usuario_rol_secundario FOREIGN KEY (rol_secundario_id) REFERENCES rol (id),
    ADD CONSTRAINT ck_usuario_rol_secundario_distinto CHECK (rol_secundario_id IS NULL OR rol_secundario_id <> rol_id);

UPDATE rol
   SET descripcion = 'Jefe de carrera: lectura de todo (como el coordinador, sin modificar) y cartola de practicas (acreditacion)'
 WHERE nombre = 'JEFE_CARRERA';

-- 3. Solo demo (ver arriba)
UPDATE usuario SET rol_secundario_id = 2 WHERE id = 32 AND email = 'jefe.carrera@demo.example';
UPDATE inscripcion SET profesor_guia_id = 32 WHERE id = 40
   AND EXISTS (SELECT 1 FROM (SELECT id FROM usuario WHERE id = 32 AND email = 'jefe.carrera@demo.example') u);
