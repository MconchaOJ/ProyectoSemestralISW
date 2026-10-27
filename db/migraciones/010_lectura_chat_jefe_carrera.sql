-- =============================================================================
-- Migracion 010 - registro de las lecturas del jefe de carrera (HU-20)
--
-- Motivo (Sprint 2, 2026-10-26):
--   HU-20, criterio 4: cada vez que el jefe de carrera abre la conversacion de un
--   estudiante con el chatbot (GET /api/inscripciones/{id}/chatbot) queda una
--   fila en la base, no solo en el log de la aplicacion. El estudiante ve en su
--   pantalla que el jefe puede leer y cuando lo hizo por ultima vez (criterio 3).
--   Las lecturas del profesor guia no se registran (HU-16 ya lo cubria).
--   SUPUESTO S-66 (ajustado el 26-oct): sigue sin confirmar con el cliente.
--
-- Cambios:
--   1. Tabla lectura_chat_jefe_carrera (inscripcion, usuario, fecha).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/010_lectura_chat_jefe_carrera.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye estos cambios.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE lectura_chat_jefe_carrera (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    inscripcion_id BIGINT      NOT NULL,
    usuario_id     BIGINT      NOT NULL COMMENT 'Jefe de carrera que leyo la conversacion',
    fecha          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_lectura_chat_jefe_carrera PRIMARY KEY (id),
    CONSTRAINT fk_lectura_chat_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_lectura_chat_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX ix_lectura_chat_inscripcion_fecha (inscripcion_id, fecha)
) ENGINE = InnoDB COMMENT = 'Cada lectura de un chat del chatbot hecha por el jefe de carrera (HU-20)';
