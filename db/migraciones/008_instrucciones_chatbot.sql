-- =============================================================================
-- Migracion 008 - instrucciones del chatbot calibradas por el coordinador (HU-17)
--
-- Motivo (Sprint 2, 2026-10-26): las instrucciones del chatbot (reglas del
-- flujo, tono, expresiones a usar y a evitar, respuestas modelo de RetroPlani
-- Mate) estaban fijas en el codigo (InstruccionesChatbot.java). Con HU-17 el
-- coordinador las calibra desde la aplicacion, sin tocar codigo.
--
-- Cambios:
--   1. Tabla instrucciones_chatbot con UNA sola fila (id = 1, CHECK): el texto
--      vigente, quien lo edito por ultima vez y cuando.
--   La tabla queda VACIA a proposito: mientras no haya fila, el chatbot usa el
--   texto base de RetroPlani Mate que trae la aplicacion
--   (backend/src/main/resources/chatbot/instrucciones-base.txt). La primera vez
--   que el coordinador guarda, la aplicacion crea la fila. Asi este archivo
--   sigue siendo solo ASCII y el texto base no queda copiado aqui.
--   db/seed.sql (desarrollo) si carga la fila con el texto base, para la demo y
--   las pruebas automaticas.
--
-- Se ejecuta UNA sola vez y como root (isw_app no puede crear tablas):
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/008_instrucciones_chatbot.sql
-- Para una base nueva NO hace falta: db/schema.sql ya incluye esta tabla.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

CREATE TABLE instrucciones_chatbot (
    id             BIGINT   NOT NULL COMMENT 'Siempre 1: un solo texto para todo el sistema',
    texto          TEXT     NOT NULL COMMENT 'Reglas, tono, expresiones y respuestas modelo del chatbot (HU-17)',
    editado_por_id BIGINT   NULL     COMMENT 'Coordinador que lo edito por ultima vez; nulo = texto base sin editar',
    fecha_edicion  DATETIME NULL     COMMENT 'Ultima edicion; nulo = texto base sin editar',
    CONSTRAINT pk_instrucciones_chatbot PRIMARY KEY (id),
    CONSTRAINT fk_instrucciones_editado_por FOREIGN KEY (editado_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_instrucciones_fila_unica CHECK (id = 1),
    CONSTRAINT ck_instrucciones_edicion CHECK ((editado_por_id IS NULL) = (fecha_edicion IS NULL))
) ENGINE = InnoDB COMMENT = 'Instrucciones del chatbot calibradas por el coordinador (HU-17)';
