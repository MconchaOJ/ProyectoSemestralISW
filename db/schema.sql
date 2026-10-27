-- =============================================================================
-- schema.sql - Sistema de Formación Práctica PEM-UBB (MySQL 8.x)
--
-- Implementa el MER de Sprint1/mer.md: 18 tablas con claves, restricciones e
-- índices (14 del Sprint 1, documento_planificacion y documento_evaluacion de
-- la Fase E, instrucciones_chatbot de HU-17 y documento_base_conocimiento de
-- HU-29). No contiene contraseñas (el
-- usuario de la aplicación lo crea db/bd.ps1 con su clave tomada de una
-- variable de entorno).
--
-- ATENCIÓN: es re-ejecutable y, por eso, BORRA y vuelve a crear las tablas.
-- Úsalo solo en desarrollo y demo. Cárgalo con db/bd.ps1 (o `source` en el
-- cliente mysql) y luego carga seed.sql.
--
-- Convenciones:
--   * Motor InnoDB, utf8mb4, collation utf8mb4_0900_ai_ci (sin distinguir
--     mayúsculas ni tildes: "jose" encuentra "José").
--   * Las restricciones llevan nombre (pk_, fk_, uq_, ck_, ix_) para que los
--     mensajes de error digan cuál regla se rompió.
--   * Todas las FK son RESTRICT (no se borra lo que está referenciado), salvo
--     respuesta_indicador -> evaluacion (CASCADE: borrar un borrador borra sus
--     puntajes).
--   * Los valores permitidos de cada estado/tipo se validan con CHECK.
-- =============================================================================

CREATE DATABASE IF NOT EXISTS isw_practicas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE isw_practicas;
SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- Limpieza (orden inverso a las dependencias)
-- -----------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS documento_base_conocimiento;
DROP TABLE IF EXISTS instrucciones_chatbot;
DROP TABLE IF EXISTS mensaje_chatbot;
DROP TABLE IF EXISTS nota_voz;
DROP TABLE IF EXISTS documento_evaluacion;
DROP TABLE IF EXISTS documento_planificacion;
DROP TABLE IF EXISTS respuesta_indicador;
DROP TABLE IF EXISTS revision_planificacion;
DROP TABLE IF EXISTS evaluacion;
DROP TABLE IF EXISTS planificacion;
DROP TABLE IF EXISTS indicador;
DROP TABLE IF EXISTS dimension_pauta;
DROP TABLE IF EXISTS pauta;
DROP TABLE IF EXISTS inscripcion;
DROP TABLE IF EXISTS asignatura_practica;
DROP TABLE IF EXISTS estudiante;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol;
DROP PROCEDURE IF EXISTS sp_validar_respuesta_indicador;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 1. Usuarios y roles                                                    (HU-01)
-- =============================================================================

CREATE TABLE rol (
    id          INT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(30)  NOT NULL COMMENT 'ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR, JEFE_CARRERA, TUTOR',
    descripcion VARCHAR(255) NULL,
    CONSTRAINT pk_rol        PRIMARY KEY (id),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Catálogo de tipos de usuario';

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    rol_id         INT          NOT NULL,
    rol_secundario_id INT       NULL COMMENT 'Segundo rol opcional (cuentas especiales, migración 004). Nulo = un solo rol',
    nombre         VARCHAR(80)  NOT NULL,
    apellidos      VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt (60 caracteres). Nunca texto plano',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario       PRIMARY KEY (id),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol   FOREIGN KEY (rol_id) REFERENCES rol (id),
    -- Cuentas especiales (2026-10-11, confirmado por el cliente): como máximo un segundo rol, distinto del principal
    CONSTRAINT fk_usuario_rol_secundario FOREIGN KEY (rol_secundario_id) REFERENCES rol (id),
    CONSTRAINT ck_usuario_rol_secundario_distinto CHECK (rol_secundario_id IS NULL OR rol_secundario_id <> rol_id),
    CONSTRAINT ck_usuario_email CHECK (email LIKE '%_@_%._%'),
    -- Formato BCrypt: $2a$/$2b$/$2y$ + costo de 2 dígitos + $ + 53 caracteres
    CONSTRAINT ck_usuario_password_bcrypt CHECK (password_hash REGEXP '^[$]2[aby][$][0-9]{2}[$].{53}$')
) ENGINE = InnoDB COMMENT = 'Cualquier persona que entra al sistema';

-- =============================================================================
-- 2. Estudiantes, asignaturas de práctica e inscripciones          (HU-02, HU-05)
-- =============================================================================

CREATE TABLE estudiante (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT      NOT NULL,
    rut             VARCHAR(10) NOT NULL COMMENT 'Sin puntos y con guion, ej. 12345678-9',
    semestre_actual INT         NOT NULL COMMENT 'Parte del contexto del chatbot (S-14)',
    anio_ingreso    INT         NOT NULL,
    validador_chatbot BOOLEAN   NOT NULL DEFAULT FALSE COMMENT 'TRUE = participa en la validación del chatbot (HU-30, migración 009)',
    CONSTRAINT pk_estudiante         PRIMARY KEY (id),
    CONSTRAINT uq_estudiante_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_estudiante_rut     UNIQUE (rut),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_estudiante_rut      CHECK (rut REGEXP '^[0-9]{7,8}-[0-9K]$'),
    CONSTRAINT ck_estudiante_semestre CHECK (semestre_actual BETWEEN 1 AND 12),
    CONSTRAINT ck_estudiante_ingreso  CHECK (anio_ingreso BETWEEN 2000 AND 2100)
) ENGINE = InnoDB COMMENT = 'Datos propios de un usuario que es estudiante';

CREATE TABLE asignatura_practica (
    id                        INT          NOT NULL AUTO_INCREMENT,
    codigo                    VARCHAR(20)  NOT NULL COMMENT 'Código interno del proyecto (no oficial UBB)',
    nombre                    VARCHAR(100) NOT NULL,
    orden                     INT          NOT NULL COMMENT '1 a 6: posición en el recorrido',
    ciclo                     VARCHAR(20)  NOT NULL COMMENT 'INICIAL, INTERMEDIO, AVANZADO',
    modalidad                 VARCHAR(20)  NOT NULL COMMENT 'INDIVIDUAL, DUPLA, GRUPO',
    requiere_centro_educativo BOOLEAN      NOT NULL DEFAULT TRUE,
    chatbot_habilitado        BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'Solo las dos prácticas finales',
    requiere_planificacion    BOOLEAN      NOT NULL DEFAULT TRUE COMMENT 'FALSE si la asignatura no entrega planificaciones de clase (Aproximación al Sistema Escolar)',
    lleva_tutor               BOOLEAN      NOT NULL DEFAULT FALSE COMMENT 'TRUE si la práctica tiene tutor de la universidad (solo Práctica Profesional, HU-07, migración 006)',
    CONSTRAINT pk_asignatura        PRIMARY KEY (id),
    CONSTRAINT uq_asignatura_codigo UNIQUE (codigo),
    CONSTRAINT uq_asignatura_orden  UNIQUE (orden),
    CONSTRAINT ck_asignatura_orden     CHECK (orden BETWEEN 1 AND 6),
    CONSTRAINT ck_asignatura_ciclo     CHECK (ciclo IN ('INICIAL', 'INTERMEDIO', 'AVANZADO')),
    CONSTRAINT ck_asignatura_modalidad CHECK (modalidad IN ('INDIVIDUAL', 'DUPLA', 'GRUPO')),
    -- El chatbot aplica solo a las dos prácticas finales, que son las del ciclo AVANZADO
    CONSTRAINT ck_asignatura_chatbot   CHECK (chatbot_habilitado = FALSE OR ciclo = 'AVANZADO'),
    -- El tutor observa clases en el colegio: solo existe donde se va a un centro educativo
    CONSTRAINT ck_asignatura_tutor     CHECK (lleva_tutor = FALSE OR requiere_centro_educativo = TRUE)
) ENGINE = InnoDB COMMENT = 'Las 6 asignaturas de práctica de la carrera';

CREATE TABLE inscripcion (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    estudiante_id     BIGINT       NOT NULL,
    asignatura_id     INT          NOT NULL,
    periodo           VARCHAR(6)   NOT NULL COMMENT 'Ej. 2026-2',
    profesor_guia_id  BIGINT       NULL     COMMENT 'Nulo en el ciclo inicial (no va al colegio)',
    profesor_curso_id BIGINT       NOT NULL COMMENT 'Quien autoriza las clases (S-05)',
    tutor_id          BIGINT       NULL     COMMENT 'Tutor de la universidad (solo Práctica Profesional); nulo = sin tutor (HU-07, migración 006)',
    estado            VARCHAR(20)  NOT NULL DEFAULT 'EN_CURSO' COMMENT 'EN_CURSO, APROBADA, REPROBADA',
    nota_final        DECIMAL(3,1) NULL     COMMENT 'Nulo hasta el cierre',
    fecha_cierre      DATE         NULL     COMMENT 'Día en que se cerró (APROBADA o REPROBADA); nulo mientras está EN_CURSO (HU-31, migración 003)',
    CONSTRAINT pk_inscripcion PRIMARY KEY (id),
    -- Un estudiante no se inscribe dos veces en la misma asignatura y periodo.
    -- Este índice también sirve para el historial (HU-05): estudiante -> periodo.
    CONSTRAINT uq_inscripcion_estudiante_asignatura_periodo UNIQUE (estudiante_id, asignatura_id, periodo),
    CONSTRAINT fk_inscripcion_estudiante FOREIGN KEY (estudiante_id)     REFERENCES estudiante (id),
    CONSTRAINT fk_inscripcion_asignatura FOREIGN KEY (asignatura_id)     REFERENCES asignatura_practica (id),
    CONSTRAINT fk_inscripcion_guia       FOREIGN KEY (profesor_guia_id)  REFERENCES usuario (id),
    CONSTRAINT fk_inscripcion_curso      FOREIGN KEY (profesor_curso_id) REFERENCES usuario (id),
    CONSTRAINT fk_inscripcion_tutor      FOREIGN KEY (tutor_id)          REFERENCES usuario (id),
    CONSTRAINT ck_inscripcion_periodo CHECK (periodo REGEXP '^[0-9]{4}-[12]$'),
    CONSTRAINT ck_inscripcion_estado  CHECK (estado IN ('EN_CURSO', 'APROBADA', 'REPROBADA')),
    CONSTRAINT ck_inscripcion_nota    CHECK (nota_final IS NULL OR nota_final BETWEEN 1.0 AND 7.0),
    CONSTRAINT ck_inscripcion_nota_cierre CHECK (estado <> 'EN_CURSO' OR nota_final IS NULL),
    CONSTRAINT ck_inscripcion_profesores_distintos CHECK (profesor_guia_id IS NULL OR profesor_guia_id <> profesor_curso_id),
    CONSTRAINT ck_inscripcion_fecha_cierre CHECK (estado <> 'EN_CURSO' OR fecha_cierre IS NULL),
    CONSTRAINT ck_inscripcion_tutor_distinto CHECK (tutor_id IS NULL
        OR (tutor_id <> profesor_curso_id AND (profesor_guia_id IS NULL OR tutor_id <> profesor_guia_id))),
    INDEX ix_inscripcion_tutor (tutor_id),
    INDEX ix_inscripcion_asignatura_periodo (asignatura_id, periodo),
    -- La cartola de prácticas (HU-31) filtra por rango de fecha de cierre.
    INDEX ix_inscripcion_fecha_cierre (fecha_cierre)
) ENGINE = InnoDB COMMENT = 'Un estudiante cursando una asignatura en un periodo (centro del modelo)';

-- =============================================================================
-- 3. Pautas de evaluación (la pauta es un dato, no código)                (HU-03)
-- =============================================================================

CREATE TABLE pauta (
    id             INT          NOT NULL AUTO_INCREMENT,
    asignatura_id  INT          NOT NULL,
    nombre         VARCHAR(200) NOT NULL,
    version        INT          NOT NULL DEFAULT 1 COMMENT 'Versión de la pauta (HU-06, migración 005): 1, 2, 3...',
    version_anterior_id INT     NULL COMMENT 'Versión que esta reemplaza; nula en la versión 1',
    tipo_evaluador VARCHAR(20)  NOT NULL COMMENT 'COLABORADOR, TUTOR, ACADEMICO',
    ambito         VARCHAR(30)  NOT NULL COMMENT 'OBSERVACION_CLASE, ESPECIALIDAD, ORIENTACION',
    numero_observacion INT      NULL COMMENT '1 o 2: primera o segunda observación (Especialidad y Orientación, HU-07, migración 006); nulo en la observación de clase',
    escala_min     INT          NOT NULL,
    escala_max     INT          NOT NULL,
    activa         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_pauta PRIMARY KEY (id),
    -- HU-06: una pauta usada no se edita; se crea otra fila con version + 1 que apunta a la anterior
    CONSTRAINT uq_pauta_asignatura_nombre_version UNIQUE (asignatura_id, nombre, version),
    CONSTRAINT uq_pauta_version_anterior UNIQUE (version_anterior_id),
    CONSTRAINT fk_pauta_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT fk_pauta_version_anterior FOREIGN KEY (version_anterior_id) REFERENCES pauta (id),
    CONSTRAINT ck_pauta_version CHECK (version >= 1 AND ((version = 1) = (version_anterior_id IS NULL))),
    CONSTRAINT ck_pauta_tipo_evaluador CHECK (tipo_evaluador IN ('COLABORADOR', 'TUTOR', 'ACADEMICO')),
    CONSTRAINT ck_pauta_ambito         CHECK (ambito IN ('OBSERVACION_CLASE', 'ESPECIALIDAD', 'ORIENTACION')),
    CONSTRAINT ck_pauta_escala         CHECK (escala_min >= 0 AND escala_max > escala_min),
    -- HU-07: la primera y la segunda observación son pautas distintas (S-55)
    CONSTRAINT ck_pauta_numero_observacion CHECK (
        (ambito = 'OBSERVACION_CLASE' AND numero_observacion IS NULL)
     OR (ambito <> 'OBSERVACION_CLASE' AND numero_observacion IS NOT NULL AND numero_observacion IN (1, 2)))
) ENGINE = InnoDB COMMENT = 'Instrumento de evaluación con su escala y quién lo aplica';

CREATE TABLE dimension_pauta (
    id       INT          NOT NULL AUTO_INCREMENT,
    pauta_id INT          NOT NULL,
    nombre   VARCHAR(150) NOT NULL,
    orden    INT          NOT NULL,
    CONSTRAINT pk_dimension_pauta PRIMARY KEY (id),
    CONSTRAINT uq_dimension_pauta_orden  UNIQUE (pauta_id, orden),
    CONSTRAINT uq_dimension_pauta_nombre UNIQUE (pauta_id, nombre),
    CONSTRAINT fk_dimension_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT ck_dimension_orden CHECK (orden >= 1)
) ENGINE = InnoDB COMMENT = 'Grupo de indicadores de una pauta';

CREATE TABLE indicador (
    id           INT  NOT NULL AUTO_INCREMENT,
    dimension_id INT  NOT NULL,
    numero       INT  NOT NULL COMMENT 'Número del criterio en la pauta impresa',
    descripcion  TEXT NOT NULL,
    CONSTRAINT pk_indicador PRIMARY KEY (id),
    CONSTRAINT uq_indicador_dimension_numero UNIQUE (dimension_id, numero),
    CONSTRAINT fk_indicador_dimension FOREIGN KEY (dimension_id) REFERENCES dimension_pauta (id),
    CONSTRAINT ck_indicador_numero CHECK (numero >= 1)
) ENGINE = InnoDB COMMENT = 'Cada criterio que se puntúa';

-- =============================================================================
-- 4. Planificación de clase y su flujo de aprobación                      (HU-04)
--    (se crea antes que evaluacion, que puede apuntar a una planificación)
-- =============================================================================

CREATE TABLE planificacion (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id            BIGINT       NOT NULL,
    titulo                    VARCHAR(200) NOT NULL,
    fecha_clase               DATE         NULL,
    objetivo                  TEXT         NULL,
    recursos                  TEXT         NULL,
    tiempos                   TEXT         NULL,
    duracion_minutos          INT          NULL,
    evaluacion_aprendizaje    TEXT         NULL,
    interaccion_participantes TEXT         NULL COMMENT 'Cómo interactúa cada participante con los recursos',
    estado                    VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR'
        COMMENT 'BORRADOR, EN_REVISION_GUIA, CON_OBSERVACIONES, EN_AUTORIZACION, AUTORIZADA',
    fecha_creacion            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_planificacion PRIMARY KEY (id),
    CONSTRAINT fk_planificacion_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT ck_planificacion_estado CHECK (estado IN
        ('BORRADOR', 'EN_REVISION_GUIA', 'CON_OBSERVACIONES', 'EN_AUTORIZACION', 'AUTORIZADA')),
    CONSTRAINT ck_planificacion_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos > 0),
    -- Un borrador (o una devuelta que se está corrigiendo) puede estar incompleto;
    -- para estar en revisión, en autorización o autorizada, todos los elementos son obligatorios.
    CONSTRAINT ck_planificacion_completa CHECK (
        estado IN ('BORRADOR', 'CON_OBSERVACIONES')
        OR (fecha_clase IS NOT NULL AND objetivo IS NOT NULL AND recursos IS NOT NULL
            AND tiempos IS NOT NULL AND duracion_minutos IS NOT NULL
            AND evaluacion_aprendizaje IS NOT NULL AND interaccion_participantes IS NOT NULL)),
    -- Planificaciones de un estudiante por estado (bandeja del guía) y por fecha de clase
    INDEX ix_planificacion_inscripcion_estado (inscripcion_id, estado),
    INDEX ix_planificacion_inscripcion_fecha  (inscripcion_id, fecha_clase)
) ENGINE = InnoDB COMMENT = 'Plan escrito de una clase, con el formato de la carrera (S-09)';

CREATE TABLE revision_planificacion (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    planificacion_id BIGINT     NOT NULL,
    revisor_id      BIGINT      NOT NULL,
    etapa           VARCHAR(10) NOT NULL COMMENT 'GUIA, CURSO',
    decision        VARCHAR(10) NOT NULL COMMENT 'APROBADA, OBSERVADA',
    comentario      TEXT        NULL,
    fecha           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_revision PRIMARY KEY (id),
    CONSTRAINT fk_revision_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT fk_revision_revisor       FOREIGN KEY (revisor_id)       REFERENCES usuario (id),
    CONSTRAINT ck_revision_etapa    CHECK (etapa IN ('GUIA', 'CURSO')),
    CONSTRAINT ck_revision_decision CHECK (decision IN ('APROBADA', 'OBSERVADA')),
    -- Devolver con observaciones exige comentario (HU-04, criterios 4 y 6)
    CONSTRAINT ck_revision_comentario CHECK (
        decision = 'APROBADA' OR (comentario IS NOT NULL AND CHAR_LENGTH(TRIM(comentario)) > 0)),
    -- Historial cronológico de una planificación
    INDEX ix_revision_planificacion_fecha (planificacion_id, fecha)
) ENGINE = InnoDB COMMENT = 'Cada decisión del guía o del profesor del curso (historial completo)';

-- =============================================================================
-- 5. Evaluaciones con la pauta digital                                    (HU-03)
-- =============================================================================

CREATE TABLE evaluacion (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id         BIGINT       NOT NULL,
    pauta_id               INT          NOT NULL,
    evaluador_id           BIGINT       NOT NULL,
    planificacion_id       BIGINT       NULL COMMENT 'Opcional: planificación de la clase observada',
    establecimiento        VARCHAR(150) NULL,
    curso                  VARCHAR(50)  NULL,
    fecha_clase            DATE         NOT NULL,
    hora_inicio            TIME         NULL,
    hora_termino           TIME         NULL,
    objetivo_clase         TEXT         NULL,
    observaciones_generales TEXT        NULL,
    estado                 VARCHAR(12)  NOT NULL DEFAULT 'BORRADOR' COMMENT 'BORRADOR, FINALIZADA',
    puntaje_total          INT          NULL COMMENT 'Calculado al finalizar',
    promedio               DECIMAL(4,2) NULL COMMENT 'Calculado al finalizar',
    fecha_finalizacion     DATETIME     NULL,
    CONSTRAINT pk_evaluacion PRIMARY KEY (id),
    CONSTRAINT fk_evaluacion_inscripcion   FOREIGN KEY (inscripcion_id)   REFERENCES inscripcion (id),
    CONSTRAINT fk_evaluacion_pauta         FOREIGN KEY (pauta_id)         REFERENCES pauta (id),
    CONSTRAINT fk_evaluacion_evaluador     FOREIGN KEY (evaluador_id)     REFERENCES usuario (id),
    CONSTRAINT fk_evaluacion_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT ck_evaluacion_estado  CHECK (estado IN ('BORRADOR', 'FINALIZADA')),
    CONSTRAINT ck_evaluacion_horas   CHECK (hora_inicio IS NULL OR hora_termino IS NULL OR hora_termino > hora_inicio),
    CONSTRAINT ck_evaluacion_puntaje CHECK (puntaje_total IS NULL OR puntaje_total >= 0),
    -- Una evaluación finalizada trae su resultado calculado; un borrador no
    CONSTRAINT ck_evaluacion_finalizada CHECK (
        (estado = 'BORRADOR'   AND puntaje_total IS NULL AND promedio IS NULL AND fecha_finalizacion IS NULL)
     OR (estado = 'FINALIZADA' AND puntaje_total IS NOT NULL AND promedio IS NOT NULL AND fecha_finalizacion IS NOT NULL)),
    -- Historial del estudiante (HU-05) y bandeja del evaluador (borradores propios)
    INDEX ix_evaluacion_inscripcion_fecha (inscripcion_id, fecha_clase),
    INDEX ix_evaluacion_evaluador_estado  (evaluador_id, estado)
) ENGINE = InnoDB COMMENT = 'Una clase evaluada con una pauta';

CREATE TABLE respuesta_indicador (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id BIGINT       NOT NULL,
    indicador_id  INT          NOT NULL,
    puntaje       INT          NOT NULL,
    comentario    VARCHAR(500) NULL,
    CONSTRAINT pk_respuesta PRIMARY KEY (id),
    -- Un solo puntaje por indicador en cada evaluación
    CONSTRAINT uq_respuesta_evaluacion_indicador UNIQUE (evaluacion_id, indicador_id),
    CONSTRAINT fk_respuesta_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_indicador  FOREIGN KEY (indicador_id)  REFERENCES indicador (id),
    CONSTRAINT ck_respuesta_puntaje CHECK (puntaje >= 0),
    INDEX ix_respuesta_indicador (indicador_id)
) ENGINE = InnoDB COMMENT = 'Puntaje de un indicador en una evaluación (muchos a muchos)';

-- Reglas entre tablas que un CHECK no puede expresar (HU-03, criterio 3):
--   1. el indicador debe pertenecer a la pauta de la evaluación;
--   2. el puntaje debe estar dentro de la escala (min-max) de esa pauta;
--   3. una evaluación FINALIZADA ya no se modifica.
-- (Validar el rol del evaluador y las transiciones de estado sigue siendo tarea de la aplicación.)
DELIMITER $$

CREATE PROCEDURE sp_validar_respuesta_indicador(
    IN p_evaluacion_id BIGINT,
    IN p_indicador_id  INT,
    IN p_puntaje       INT
)
BEGIN
    DECLARE v_estado      VARCHAR(12);
    DECLARE v_pauta_eval  INT;
    DECLARE v_min         INT;
    DECLARE v_max         INT;
    DECLARE v_pauta_indic INT;

    SELECT e.estado, e.pauta_id, p.escala_min, p.escala_max
      INTO v_estado, v_pauta_eval, v_min, v_max
      FROM evaluacion e
      JOIN pauta p ON p.id = e.pauta_id
     WHERE e.id = p_evaluacion_id;

    SELECT d.pauta_id
      INTO v_pauta_indic
      FROM indicador i
      JOIN dimension_pauta d ON d.id = i.dimension_id
     WHERE i.id = p_indicador_id;

    IF v_estado = 'FINALIZADA' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La evaluacion esta FINALIZADA y no se puede modificar';
    END IF;
    IF v_pauta_indic <> v_pauta_eval THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El indicador no pertenece a la pauta de la evaluacion';
    END IF;
    IF p_puntaje < v_min OR p_puntaje > v_max THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El puntaje esta fuera de la escala de la pauta';
    END IF;
END$$

CREATE TRIGGER trg_respuesta_indicador_bi
BEFORE INSERT ON respuesta_indicador FOR EACH ROW
BEGIN
    CALL sp_validar_respuesta_indicador(NEW.evaluacion_id, NEW.indicador_id, NEW.puntaje);
END$$

CREATE TRIGGER trg_respuesta_indicador_bu
BEFORE UPDATE ON respuesta_indicador FOR EACH ROW
BEGIN
    CALL sp_validar_respuesta_indicador(NEW.evaluacion_id, NEW.indicador_id, NEW.puntaje);
END$$

DELIMITER ;

-- =============================================================================
-- 6. Documentos adjuntos de planificaciones y evaluaciones          (Fase E, migración 002)
-- =============================================================================

CREATE TABLE documento_planificacion (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    planificacion_id  BIGINT       NOT NULL,
    nombre_archivo    VARCHAR(255) NOT NULL,
    ruta_archivo      VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime         VARCHAR(150) NOT NULL,
    tamano_bytes      BIGINT       NOT NULL,
    subido_por        BIGINT       NOT NULL,
    fecha_subida      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_planificacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_planificacion_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT fk_documento_planificacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_planificacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_planificacion_planificacion (planificacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos (Word o PDF, máximo 10 MB) que el estudiante adjunta a su planificación';

CREATE TABLE documento_evaluacion (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id  BIGINT       NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco, fuera del control de versiones',
    tipo_mime      VARCHAR(150) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    subido_por     BIGINT       NOT NULL,
    fecha_subida   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_evaluacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_evaluacion_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id),
    CONSTRAINT fk_documento_evaluacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_evaluacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_evaluacion_evaluacion (evaluacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos (Word, Excel o PDF, máximo 10 MB) con la pauta y evaluación formal del guía';

-- =============================================================================
-- 7. Tablas de sprints posteriores (existen para que el modelo esté completo)
--    NOTA_VOZ: HU-11 y HU-12 (API ya construida en la Fase E). MENSAJE_CHATBOT: HU-15 y HU-16
--    (API construida en el Sprint 2, 2026-10-26; migración 007 agregó planificacion_id).
-- =============================================================================

CREATE TABLE nota_voz (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id          BIGINT       NOT NULL,
    archivo_ruta           VARCHAR(500) NOT NULL,
    duracion_segundos      INT          NOT NULL,
    transcripcion          TEXT         NULL COMMENT 'Nulo hasta transcribir',
    transcripcion_corregida BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_grabacion        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_nota_voz PRIMARY KEY (id),
    -- Como máximo una nota de voz por evaluación
    CONSTRAINT uq_nota_voz_evaluacion UNIQUE (evaluacion_id),
    CONSTRAINT fk_nota_voz_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id),
    CONSTRAINT ck_nota_voz_duracion CHECK (duracion_segundos > 0)
) ENGINE = InnoDB COMMENT = 'Audio del evaluador al terminar la clase, con su transcripción';

CREATE TABLE mensaje_chatbot (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    inscripcion_id   BIGINT      NOT NULL,
    planificacion_id BIGINT      NULL     COMMENT 'Planificación que el estudiante adjuntó a este mensaje (HU-15, migración 007); nulo = sin planificación',
    autor            VARCHAR(12) NOT NULL COMMENT 'ESTUDIANTE, CHATBOT',
    contenido        TEXT        NOT NULL,
    fecha            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_mensaje_chatbot PRIMARY KEY (id),
    CONSTRAINT fk_mensaje_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_mensaje_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT ck_mensaje_autor CHECK (autor IN ('ESTUDIANTE', 'CHATBOT')),
    -- Conversación de una inscripción en orden
    INDEX ix_mensaje_inscripcion_fecha (inscripcion_id, fecha),
    INDEX ix_mensaje_planificacion (planificacion_id)
) ENGINE = InnoDB COMMENT = 'Mensajes entre el estudiante y el chatbot, por inscripción (HU-15; el guía los lee, HU-16)';

-- HU-17 (migración 008): una sola fila con las instrucciones que calibra el coordinador. Sin fila, la aplicación usa
-- el texto base de RetroPlani Mate (backend/src/main/resources/chatbot/instrucciones-base.txt).
CREATE TABLE instrucciones_chatbot (
    id             BIGINT   NOT NULL COMMENT 'Siempre 1: un solo texto para todo el sistema',
    texto          TEXT     NOT NULL COMMENT 'Reglas, tono, expresiones y respuestas modelo del chatbot (HU-17)',
    editado_por_id BIGINT   NULL     COMMENT 'Coordinador que lo editó por última vez; nulo = texto base sin editar',
    fecha_edicion  DATETIME NULL     COMMENT 'Última edición; nulo = texto base sin editar',
    validacion_abierta BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'TRUE = el chatbot solo responde a los validadores (HU-30, migración 009)',
    CONSTRAINT pk_instrucciones_chatbot PRIMARY KEY (id),
    CONSTRAINT fk_instrucciones_editado_por FOREIGN KEY (editado_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_instrucciones_fila_unica CHECK (id = 1),
    CONSTRAINT ck_instrucciones_edicion CHECK ((editado_por_id IS NULL) = (fecha_edicion IS NULL))
) ENGINE = InnoDB COMMENT = 'Configuración del chatbot: instrucciones del coordinador (HU-17) y validación (HU-30)';

-- HU-29 (migración 009): documentos de referencia del chatbot, uno vigente por tipo. El archivo va a disco; la base
-- guarda su ruta y el texto ya extraído (no se convierte un PDF largo en cada mensaje).
CREATE TABLE documento_base_conocimiento (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tipo           VARCHAR(40)  NOT NULL COMMENT 'Uno de los 10 documentos de la propuesta RetroPlani Mate',
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL COMMENT 'Ruta relativa en disco (fuera de git)',
    tipo_mime      VARCHAR(100) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    texto          MEDIUMTEXT   NOT NULL COMMENT 'Texto extraído del Word o PDF al subirlo',
    subido_por_id  BIGINT       NOT NULL,
    fecha_subida   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_base_conocimiento PRIMARY KEY (id),
    CONSTRAINT uq_documento_base_tipo UNIQUE (tipo),
    CONSTRAINT fk_documento_base_subido_por FOREIGN KEY (subido_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_documento_base_tipo CHECK (tipo IN ('MOMENTOS_CLASE', 'RUBRICA_PLANIFICACION',
        'DUA_DIVERSIFICACION', 'BANCO_COMENTARIOS', 'ENLACES_RECURSOS', 'BASES_CURRICULARES_7B_2M',
        'BASES_CURRICULARES_3M_4M', 'ACCIONES_DUA', 'PLANIFICACION_COMENTADA_1', 'PLANIFICACION_COMENTADA_2')),
    CONSTRAINT ck_documento_base_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760)
) ENGINE = InnoDB COMMENT = 'Documentos de referencia del chatbot cargados por el coordinador (HU-29)';
