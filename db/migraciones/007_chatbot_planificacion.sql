-- =============================================================================
-- Migracion 007 - planificacion revisada en cada mensaje del chatbot (HU-15)
--
-- Motivo (Sprint 2, 2026-10-26): la tabla mensaje_chatbot existe desde el
-- Sprint 1 (vacia, sin API). Con HU-15 el estudiante elige en el chat cual de
-- SUS planificaciones quiere que el chatbot retroalimente (el chatbot la lee
-- desde el formulario y los documentos adjuntos, HU-21). Guardar ese dato en
-- el mensaje permite:
--   * que la conversacion siga hablando de la misma planificacion en los
--     mensajes siguientes, sin volver a elegirla;
--   * que el profesor guia (HU-16) vea sobre que planificacion se converso.
--
-- Cambios:
--   1. mensaje_chatbot.planificacion_id (nulo = mensaje sin planificacion
--      adjunta), con FK a planificacion e indice.
--   Que la planificacion sea de la MISMA inscripcion que el mensaje lo
--   comprueba la aplicacion (ChatbotService) y db/verificacion.sql (bloque 24).
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede alterar tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/007_chatbot_planificacion.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye este cambio.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

ALTER TABLE mensaje_chatbot
    ADD COLUMN planificacion_id BIGINT NULL
        COMMENT 'Planificacion que el estudiante adjunto a este mensaje (HU-15); nulo = sin planificacion'
        AFTER inscripcion_id,
    ADD CONSTRAINT fk_mensaje_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    ADD INDEX ix_mensaje_planificacion (planificacion_id);
