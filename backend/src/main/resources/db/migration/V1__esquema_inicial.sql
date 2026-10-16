-- =============================================================================
-- V1__esquema_inicial.sql - Migración inicial Flyway
-- Esquema completo del Sistema de Formación Práctica PEM-UBB (16 tablas)
-- =============================================================================

-- 1. Usuarios y roles
CREATE TABLE rol (
    id          INT          NOT NULL AUTO_INCREMENT,
    nombre      VARCHAR(30)  NOT NULL COMMENT 'ESTUDIANTE, PROFESOR_GUIA, PROFESOR_CURSO, COORDINADOR',
    descripcion VARCHAR(255) NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
) ENGINE = InnoDB COMMENT = 'Catálogo de tipos de usuario';

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    rol_id         INT          NOT NULL,
    nombre         VARCHAR(80)  NOT NULL,
    apellidos      VARCHAR(120) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL COMMENT 'BCrypt (60 caracteres). Nunca texto plano',
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol (id),
    CONSTRAINT ck_usuario_email CHECK (email LIKE '%_@_%._%'),
    CONSTRAINT ck_usuario_password_bcrypt CHECK (password_hash REGEXP '^[$]2[aby][$][0-9]{2}[$].{53}$')
) ENGINE = InnoDB COMMENT = 'Cualquier persona que entra al sistema';

-- 2. Estudiantes, asignaturas de práctica e inscripciones
CREATE TABLE estudiante (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    usuario_id      BIGINT      NOT NULL,
    rut             VARCHAR(10) NOT NULL COMMENT 'Sin puntos y con guion, ej. 12345678-9',
    semestre_actual INT         NOT NULL,
    anio_ingreso    INT         NOT NULL,
    CONSTRAINT pk_estudiante PRIMARY KEY (id),
    CONSTRAINT uq_estudiante_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_estudiante_rut UNIQUE (rut),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_estudiante_rut CHECK (rut REGEXP '^[0-9]{7,8}-[0-9K]$'),
    CONSTRAINT ck_estudiante_semestre CHECK (semestre_actual BETWEEN 1 AND 12),
    CONSTRAINT ck_estudiante_ingreso CHECK (anio_ingreso BETWEEN 2000 AND 2100)
) ENGINE = InnoDB COMMENT = 'Datos propios de un usuario que es estudiante';

CREATE TABLE asignatura_practica (
    id                        INT          NOT NULL AUTO_INCREMENT,
    codigo                    VARCHAR(20)  NOT NULL,
    nombre                    VARCHAR(100) NOT NULL,
    orden                     INT          NOT NULL,
    ciclo                     VARCHAR(20)  NOT NULL,
    modalidad                 VARCHAR(20)  NOT NULL,
    requiere_centro_educativo BOOLEAN      NOT NULL DEFAULT TRUE,
    chatbot_habilitado        BOOLEAN      NOT NULL DEFAULT FALSE,
    requiere_planificacion    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_asignatura PRIMARY KEY (id),
    CONSTRAINT uq_asignatura_codigo UNIQUE (codigo),
    CONSTRAINT uq_asignatura_orden UNIQUE (orden),
    CONSTRAINT ck_asignatura_orden CHECK (orden BETWEEN 1 AND 6),
    CONSTRAINT ck_asignatura_ciclo CHECK (ciclo IN ('INICIAL', 'INTERMEDIO', 'AVANZADO')),
    CONSTRAINT ck_asignatura_modalidad CHECK (modalidad IN ('INDIVIDUAL', 'DUPLA', 'GRUPO')),
    CONSTRAINT ck_asignatura_chatbot CHECK (chatbot_habilitado = FALSE OR ciclo = 'AVANZADO')
) ENGINE = InnoDB COMMENT = 'Las 6 asignaturas de práctica de la carrera';

CREATE TABLE inscripcion (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    estudiante_id     BIGINT       NOT NULL,
    asignatura_id     INT          NOT NULL,
    periodo           VARCHAR(6)   NOT NULL,
    profesor_guia_id  BIGINT       NULL,
    profesor_curso_id BIGINT       NOT NULL,
    estado            VARCHAR(20)  NOT NULL DEFAULT 'EN_CURSO',
    nota_final        DECIMAL(3,1) NULL,
    CONSTRAINT pk_inscripcion PRIMARY KEY (id),
    CONSTRAINT uq_inscripcion_estudiante_asignatura_periodo UNIQUE (estudiante_id, asignatura_id, periodo),
    CONSTRAINT fk_inscripcion_estudiante FOREIGN KEY (estudiante_id) REFERENCES estudiante (id),
    CONSTRAINT fk_inscripcion_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT fk_inscripcion_guia FOREIGN KEY (profesor_guia_id) REFERENCES usuario (id),
    CONSTRAINT fk_inscripcion_curso FOREIGN KEY (profesor_curso_id) REFERENCES usuario (id),
    CONSTRAINT ck_inscripcion_periodo CHECK (periodo REGEXP '^[0-9]{4}-[12]$'),
    CONSTRAINT ck_inscripcion_estado CHECK (estado IN ('EN_CURSO', 'APROBADA', 'REPROBADA')),
    CONSTRAINT ck_inscripcion_nota CHECK (nota_final IS NULL OR nota_final BETWEEN 1.0 AND 7.0),
    CONSTRAINT ck_inscripcion_nota_cierre CHECK (estado <> 'EN_CURSO' OR nota_final IS NULL),
    CONSTRAINT ck_inscripcion_profesores_distintos CHECK (profesor_guia_id IS NULL OR profesor_guia_id <> profesor_curso_id),
    INDEX ix_inscripcion_asignatura_periodo (asignatura_id, periodo)
) ENGINE = InnoDB COMMENT = 'Inscripción de práctica';

-- 3. Pautas de evaluación
CREATE TABLE pauta (
    id             INT          NOT NULL AUTO_INCREMENT,
    asignatura_id  INT          NOT NULL,
    nombre         VARCHAR(200) NOT NULL,
    tipo_evaluador VARCHAR(20)  NOT NULL,
    ambito         VARCHAR(30)  NOT NULL,
    escala_min     INT          NOT NULL,
    escala_max     INT          NOT NULL,
    activa         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_pauta PRIMARY KEY (id),
    CONSTRAINT uq_pauta_asignatura_nombre UNIQUE (asignatura_id, nombre),
    CONSTRAINT fk_pauta_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura_practica (id),
    CONSTRAINT ck_pauta_tipo_evaluador CHECK (tipo_evaluador IN ('COLABORADOR', 'TUTOR', 'ACADEMICO')),
    CONSTRAINT ck_pauta_ambito CHECK (ambito IN ('OBSERVACION_CLASE', 'ESPECIALIDAD', 'ORIENTACION')),
    CONSTRAINT ck_pauta_escala CHECK (escala_min >= 0 AND escala_max > escala_min)
) ENGINE = InnoDB COMMENT = 'Pauta de evaluación';

CREATE TABLE dimension_pauta (
    id       INT          NOT NULL AUTO_INCREMENT,
    pauta_id INT          NOT NULL,
    nombre   VARCHAR(150) NOT NULL,
    orden    INT          NOT NULL,
    CONSTRAINT pk_dimension_pauta PRIMARY KEY (id),
    CONSTRAINT uq_dimension_pauta_orden UNIQUE (pauta_id, orden),
    CONSTRAINT uq_dimension_pauta_nombre UNIQUE (pauta_id, nombre),
    CONSTRAINT fk_dimension_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT ck_dimension_orden CHECK (orden >= 1)
) ENGINE = InnoDB COMMENT = 'Dimensiones de pauta';

CREATE TABLE indicador (
    id           INT  NOT NULL AUTO_INCREMENT,
    dimension_id INT  NOT NULL,
    numero       INT  NOT NULL,
    descripcion  TEXT NOT NULL,
    CONSTRAINT pk_indicador PRIMARY KEY (id),
    CONSTRAINT uq_indicador_dimension_numero UNIQUE (dimension_id, numero),
    CONSTRAINT fk_indicador_dimension FOREIGN KEY (dimension_id) REFERENCES dimension_pauta (id),
    CONSTRAINT ck_indicador_numero CHECK (numero >= 1)
) ENGINE = InnoDB COMMENT = 'Indicadores de evaluación';

-- 4. Planificación de clase y revisión
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
    interaccion_participantes TEXT         NULL,
    estado                    VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR',
    fecha_creacion            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_planificacion PRIMARY KEY (id),
    CONSTRAINT fk_planificacion_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT ck_planificacion_estado CHECK (estado IN
        ('BORRADOR', 'EN_REVISION_GUIA', 'CON_OBSERVACIONES', 'EN_AUTORIZACION', 'AUTORIZADA')),
    CONSTRAINT ck_planificacion_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos > 0),
    INDEX ix_planificacion_inscripcion_estado (inscripcion_id, estado),
    INDEX ix_planificacion_inscripcion_fecha (inscripcion_id, fecha_clase)
) ENGINE = InnoDB COMMENT = 'Planificación de clase';

CREATE TABLE revision_planificacion (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    planificacion_id BIGINT      NOT NULL,
    revisor_id       BIGINT      NOT NULL,
    etapa            VARCHAR(10) NOT NULL,
    decision         VARCHAR(10) NOT NULL,
    comentario       TEXT        NULL,
    fecha            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_revision PRIMARY KEY (id),
    CONSTRAINT fk_revision_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT fk_revision_revisor FOREIGN KEY (revisor_id) REFERENCES usuario (id),
    CONSTRAINT ck_revision_etapa CHECK (etapa IN ('GUIA', 'CURSO')),
    CONSTRAINT ck_revision_decision CHECK (decision IN ('APROBADA', 'OBSERVADA')),
    INDEX ix_revision_planificacion_fecha (planificacion_id, fecha)
) ENGINE = InnoDB COMMENT = 'Historial de revisiones de planificación';

-- 5. Evaluaciones y respuestas de indicadores
CREATE TABLE evaluacion (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    inscripcion_id          BIGINT       NOT NULL,
    pauta_id                INT          NOT NULL,
    evaluador_id            BIGINT       NOT NULL,
    planificacion_id        BIGINT       NULL,
    establecimiento         VARCHAR(150) NULL,
    curso                   VARCHAR(50)  NULL,
    fecha_clase             DATE         NOT NULL,
    hora_inicio             TIME         NULL,
    hora_termino            TIME         NULL,
    objetivo_clase          TEXT         NULL,
    observaciones_generales TEXT         NULL,
    estado                  VARCHAR(12)  NOT NULL DEFAULT 'BORRADOR',
    puntaje_total           INT          NULL,
    promedio                DECIMAL(4,2) NULL,
    fecha_finalizacion      DATETIME     NULL,
    CONSTRAINT pk_evaluacion PRIMARY KEY (id),
    CONSTRAINT fk_evaluacion_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT fk_evaluacion_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT fk_evaluacion_evaluador FOREIGN KEY (evaluador_id) REFERENCES usuario (id),
    CONSTRAINT fk_evaluacion_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT ck_evaluacion_estado CHECK (estado IN ('BORRADOR', 'FINALIZADA')),
    INDEX ix_evaluacion_inscripcion_fecha (inscripcion_id, fecha_clase),
    INDEX ix_evaluacion_evaluador_estado (evaluador_id, estado)
) ENGINE = InnoDB COMMENT = 'Evaluaciones realizadas';

CREATE TABLE respuesta_indicador (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id BIGINT       NOT NULL,
    indicador_id  INT          NOT NULL,
    puntaje       INT          NOT NULL,
    comentario    VARCHAR(500) NULL,
    CONSTRAINT pk_respuesta PRIMARY KEY (id),
    CONSTRAINT uq_respuesta_evaluacion_indicador UNIQUE (evaluacion_id, indicador_id),
    CONSTRAINT fk_respuesta_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_indicador FOREIGN KEY (indicador_id) REFERENCES indicador (id),
    CONSTRAINT ck_respuesta_puntaje CHECK (puntaje >= 0),
    INDEX ix_respuesta_indicador (indicador_id)
) ENGINE = InnoDB COMMENT = 'Respuestas a los indicadores de la pauta';

-- 6. Documentos adjuntos
CREATE TABLE documento_planificacion (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    planificacion_id  BIGINT       NOT NULL,
    nombre_archivo    VARCHAR(255) NOT NULL,
    ruta_archivo      VARCHAR(500) NOT NULL,
    tipo_mime         VARCHAR(150) NOT NULL,
    tamano_bytes      BIGINT       NOT NULL,
    subido_por        BIGINT       NOT NULL,
    fecha_subida      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_planificacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_planificacion_planificacion FOREIGN KEY (planificacion_id) REFERENCES planificacion (id),
    CONSTRAINT fk_documento_planificacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_planificacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_planificacion_planificacion (planificacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos adjuntos a planificación';

CREATE TABLE documento_evaluacion (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id  BIGINT       NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500) NOT NULL,
    tipo_mime      VARCHAR(150) NOT NULL,
    tamano_bytes   BIGINT       NOT NULL,
    subido_por     BIGINT       NOT NULL,
    fecha_subida   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_documento_evaluacion PRIMARY KEY (id),
    CONSTRAINT fk_documento_evaluacion_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id),
    CONSTRAINT fk_documento_evaluacion_usuario FOREIGN KEY (subido_por) REFERENCES usuario (id),
    CONSTRAINT ck_documento_evaluacion_tamano CHECK (tamano_bytes > 0 AND tamano_bytes <= 10485760),
    INDEX ix_documento_evaluacion_evaluacion (evaluacion_id)
) ENGINE = InnoDB COMMENT = 'Documentos adjuntos a evaluación';

-- 7. Notas de voz y mensajes de chatbot
CREATE TABLE nota_voz (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    evaluacion_id           BIGINT       NOT NULL,
    archivo_ruta            VARCHAR(500) NOT NULL,
    duracion_segundos       INT          NOT NULL,
    transcripcion           TEXT         NULL,
    transcripcion_corregida BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_grabacion         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_nota_voz PRIMARY KEY (id),
    CONSTRAINT uq_nota_voz_evaluacion UNIQUE (evaluacion_id),
    CONSTRAINT fk_nota_voz_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluacion (id),
    CONSTRAINT ck_nota_voz_duracion CHECK (duracion_segundos > 0)
) ENGINE = InnoDB COMMENT = 'Notas de voz de retroalimentación';

CREATE TABLE mensaje_chatbot (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    inscripcion_id BIGINT      NOT NULL,
    autor          VARCHAR(12) NOT NULL,
    contenido      TEXT        NOT NULL,
    fecha          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT pk_mensaje_chatbot PRIMARY KEY (id),
    CONSTRAINT fk_mensaje_inscripcion FOREIGN KEY (inscripcion_id) REFERENCES inscripcion (id),
    CONSTRAINT ck_mensaje_autor CHECK (autor IN ('ESTUDIANTE', 'CHATBOT')),
    INDEX ix_mensaje_inscripcion_fecha (inscripcion_id, fecha)
) ENGINE = InnoDB COMMENT = 'Historial de interacción con el chatbot';
