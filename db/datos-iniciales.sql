-- =============================================================================
-- datos-iniciales.sql - Datos iniciales de PRODUCCIÓN (MySQL 8.x, Sprint 2)
-- -----------------------------------------------------------------------------
-- Es lo que se carga en el servidor real EN LUGAR de seed.sql (seed.sql trae
-- usuarios ficticios con una clave conocida: nunca va al servidor).
-- Contiene solo el catálogo que el sistema necesita para funcionar (roles, las
-- 6 asignaturas, la pauta de observación de clase) y UN coordinador inicial.
-- Desde ese coordinador se registra a todos los demás usuarios por la aplicación.
--
-- La clave del coordinador NO está aquí: se define al instalar. Antes de este
-- script, en la MISMA sesión de mysql, se definen 4 variables:
--
--   SET @coordinador_email     = 'nombre.apellido@ubiobio.cl';
--   SET @coordinador_nombre    = 'Nombre';
--   SET @coordinador_apellidos = 'Apellidos';
--   SET @coordinador_hash      = '<hash BCrypt de la clave>';
--   SOURCE db/datos-iniciales.sql;
--
-- El hash se genera con la herramienta incluida en el jar (pide la clave sin mostrarla):
--   java -cp practicas-0.1.0-SNAPSHOT.jar \
--        -Dloader.main=cl.ubb.pem.practicas.security.GeneradorHashClave \
--        org.springframework.boot.loader.launch.PropertiesLauncher
--
-- Orden en una base nueva: schema.sql (una sola vez, como root) -> este script
-- (como root) -> crear el usuario isw_app con SELECT, INSERT, UPDATE, DELETE.
-- Se ejecuta UNA vez: si la base ya tiene datos falla por claves duplicadas.
--
-- Mantener sincronizado con la PARTE 1 (CATÁLOGO) de seed.sql: mismos ids y textos.
-- =============================================================================

USE isw_practicas;
SET NAMES utf8mb4;

-- Control de versión: MySQL 8.0.16 o superior (se recomienda 8.4 LTS); MariaDB y MySQL 5.7 no sirven.
-- Antes de 8.0.16 los CHECK (incluido el control del coordinador de más abajo) se aceptan pero no se cumplen,
-- por eso aquí no se usa un CHECK: si la versión no alcanza, se intenta asignar un sql_mode inválido y el script
-- se detiene con "Variable 'sql_mode' can't be set to the value of 'ERROR_se_necesita_MySQL_8_0_16_o_superior'"
-- sin haber cargado nada. La misma regla está en db/VersionMysql.ps1; ambas se prueban con db/pruebas-version-mysql.ps1.
SET @version_mysql_ok = (VERSION() NOT LIKE '%MariaDB%'
    AND CAST(SUBSTRING_INDEX(VERSION(), '.', 1) AS UNSIGNED) * 1000000
      + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 2), '.', -1) AS UNSIGNED) * 1000
      + CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(VERSION(), '.', 3), '.', -1) AS UNSIGNED) >= 8000016);
SET SESSION sql_mode = IF(@version_mysql_ok, @@SESSION.sql_mode, 'ERROR_se_necesita_MySQL_8_0_16_o_superior');

-- Control previo: si falta una variable o el correo es de demostración, el script se detiene aquí
-- ("Check constraint 'faltan_variables_del_coordinador' is violated") sin haber cargado nada.
DROP TEMPORARY TABLE IF EXISTS control_datos_iniciales;
CREATE TEMPORARY TABLE control_datos_iniciales (
    ok INT NOT NULL,
    CONSTRAINT faltan_variables_del_coordinador CHECK (ok = 1)
);
INSERT INTO control_datos_iniciales (ok)
SELECT IF(COALESCE(TRIM(@coordinador_email), '') <> ''
          AND COALESCE(TRIM(@coordinador_nombre), '') <> ''
          AND COALESCE(TRIM(@coordinador_apellidos), '') <> ''
          AND COALESCE(@coordinador_hash, '') <> ''
          AND @coordinador_email NOT LIKE '%@demo.example', 1, 0);
DROP TEMPORARY TABLE control_datos_iniciales;

START TRANSACTION;

-- Roles (HU-01, HU-31)
INSERT INTO rol (id, nombre, descripcion) VALUES
    (1, 'ESTUDIANTE',     'Estudiante de Pedagogía en Educación Matemática que cursa las prácticas'),
    (2, 'PROFESOR_GUIA',  'Profesor colaborador del colegio que guía al estudiante y evalúa sus clases'),
    (3, 'PROFESOR_CURSO', 'Académico de la universidad que autoriza las clases planificadas'),
    (4, 'COORDINADOR',    'Coordinador de prácticas: registra estudiantes, profesores e inscripciones'),
    (5, 'JEFE_CARRERA',   'Jefe de carrera: lectura de todo (como el coordinador, sin modificar) y cartola de prácticas (acreditación)'),
    (6, 'TUTOR',          'Tutor de la universidad: observa y evalúa la Práctica Profesional con sus propias pautas');

-- Las 6 asignaturas de práctica (HU-02). Códigos internos del proyecto, no oficiales de la UBB.
-- SUPUESTO (S-16, reportado por un estudiante, pendiente de confirmar): Aproximación es GRUPO y no entrega
-- planificaciones; Ambientes es DUPLA; ninguna de las dos va a un colegio.
INSERT INTO asignatura_practica
    (id, codigo, nombre, orden, ciclo, modalidad, requiere_centro_educativo, chatbot_habilitado, requiere_planificacion, lleva_tutor) VALUES
    (1, 'PEM-P1', 'Aproximación al Sistema Escolar',    1, 'INICIAL',    'GRUPO',      FALSE, FALSE, FALSE, FALSE),
    (2, 'PEM-P2', 'Ambientes de Aprendizaje',           2, 'INICIAL',    'DUPLA',      FALSE, FALSE, TRUE, FALSE),
    (3, 'PEM-P3', 'Trabajo Colaborativo en el Aula',    3, 'INTERMEDIO', 'DUPLA',      TRUE,  FALSE, TRUE, FALSE),
    (4, 'PEM-P4', 'Investigación en el Aula',           4, 'INTERMEDIO', 'DUPLA',      TRUE,  FALSE, TRUE, FALSE),
    (5, 'PEM-P5', 'Práctica Pedagógica',                5, 'AVANZADO',   'INDIVIDUAL', TRUE,  TRUE,  TRUE, FALSE),
    (6, 'PEM-P6', 'Práctica Profesional',               6, 'AVANZADO',   'INDIVIDUAL', TRUE,  TRUE,  TRUE, TRUE);

-- Pauta de observación de clase del profesor colaborador (S-07, HU-03).
-- Fuente: "Pauta de Observación de la clase - Prof Colaborador/a - Trabajo colaborativo 2026".
INSERT INTO pauta (id, asignatura_id, nombre, tipo_evaluador, ambito, escala_min, escala_max, activa) VALUES
    (1, 3, 'Pauta de observación de clase de Matemática - Profesor/a colaborador/a (Trabajo Colaborativo 2026)',
        'COLABORADOR', 'OBSERVACION_CLASE', 1, 7, TRUE);

INSERT INTO dimension_pauta (id, pauta_id, nombre, orden) VALUES
    (1, 1, 'Aspectos de la estructura de la clase',        1),
    (2, 1, 'Tareas matemáticas propuestas',                2),
    (3, 1, 'Ambiente de aula y recursos de aprendizaje',   3);

INSERT INTO indicador (id, dimension_id, numero, descripcion) VALUES
    -- Dimensión 1: estructura de la clase (indicadores 1 a 6)
    ( 1, 1,  1, 'El objetivo de la clase se presenta a los estudiantes. Es claro y pertinente al nivel.'),
    ( 2, 1,  2, 'El profesor/a desarrolla un inicio de la clase generando retroalimentación y/o activación de conocimientos previos.'),
    ( 3, 1,  3, 'El profesor/a realiza un desarrollo de la clase de manera pertinente al objetivo planteado.'),
    ( 4, 1,  4, 'El profesor/a realiza un desarrollo de la clase con tareas matemáticas pertinentes y coherentes con el objetivo planteado.'),
    ( 5, 1,  5, 'El profesor/a realiza un cierre de la clase con actividad(es) clave(s) para la verificación del cumplimiento del objetivo planteado.'),
    ( 6, 1,  6, 'El profesor/a realiza un cierre de la clase integrando y/o sintetizando los aprendizajes de la clase.'),
    -- Dimensión 2: tareas matemáticas propuestas (indicadores 7 a 12)
    ( 7, 2,  7, 'El profesor/a hace uso de los errores y dificultades de los estudiantes como una instancia de devolución.'),
    ( 8, 2,  8, 'El profesor/a propone tareas matemáticas que se relacionen con alguna de las habilidades de: resolución de problemas, representación, modelación y/o argumentación y comunicación.'),
    ( 9, 2,  9, 'Las tareas matemáticas están contextualizadas al entorno de los estudiantes.'),
    (10, 2, 10, 'El profesor/a propone variadas estrategias de resolución de las tareas matemáticas presentadas.'),
    (11, 2, 11, 'El profesor/a explica, comprueba y/o demuestra las definiciones, teoremas, proposiciones o procedimientos de manera clara y adecuadas al nivel educativo.'),
    (12, 2, 12, 'El profesor/a utiliza estrategias de trabajo colaborativo para las/os estudiantes en las actividades de la clase.'),
    -- Dimensión 3: ambiente de aula y recursos de aprendizaje (indicadores 13 a 20)
    (13, 3, 13, 'El profesor/a hace uso de un lenguaje acorde al nivel de enseñanza.'),
    (14, 3, 14, 'El profesor/a genera oportunidades de participación de los estudiantes durante toda la clase.'),
    (15, 3, 15, 'El profesor/a propicia un ambiente para el desarrollo de toda la clase.'),
    (16, 3, 16, 'El profesor/a brinda atención a todos los estudiantes del aula.'),
    (17, 3, 17, 'El profesor/a hace uso de materiales y/o recursos para el desarrollo de los aprendizajes en la clase.'),
    (18, 3, 18, 'El profesor/a promueve la autoestima, evitando el rechazo, fobia o miedo a la matemática.'),
    (19, 3, 19, 'El profesor/a promueve el diálogo y comunicación entre estudiantes favoreciendo la inclusión en la clase.'),
    (20, 3, 20, 'El profesor/a realiza observación sistemática del proceso cognitivo de los estudiantes durante toda la clase.');

-- HU-07 (2026-10-16): pautas de Especialidad de la primera observación de Práctica Profesional, para el profesor
-- colaborador (guía) y para el tutor. Fuente: "Pautas de evaluación Práctica Profesional 2025-2" (PRIMERA PAUTA DE
-- EVALUACIÓN ESPECIALIDAD COLABORADOR-A / TUTOR-A). Escala 2 a 7 sin decimales, dimensiones A a D, 19 indicadores
-- (puntaje total 133, igual que la pauta impresa). La pauta impresa numera de nuevo en cada dimensión (A1..A4, B1..);
-- aquí van corridos del 1 al 19, como los numera el editor de pautas (HU-06; supuesto S-56). Las otras 6 pautas
-- (Orientación y segunda observación) quedan pendientes de cargar.
INSERT INTO pauta (id, asignatura_id, nombre, tipo_evaluador, ambito, numero_observacion, escala_min, escala_max, activa) VALUES
    (2, 6, 'Pauta de Especialidad, 1.ª observación - Profesor/a colaborador/a (Práctica Profesional 2025-2)',
        'COLABORADOR', 'ESPECIALIDAD', 1, 2, 7, TRUE),
    (3, 6, 'Pauta de Especialidad, 1.ª observación - Tutor/a (Práctica Profesional 2025-2)',
        'TUTOR', 'ESPECIALIDAD', 1, 2, 7, TRUE);

INSERT INTO dimension_pauta (id, pauta_id, nombre, orden) VALUES
    ( 4, 2, 'A. Conocimiento y preparación de la enseñanza', 1),
    ( 5, 2, 'B. Convivencia al interior del aula', 2),
    ( 6, 2, 'C. Transposición didáctica', 3),
    ( 7, 2, 'D. Responsabilidad y ética profesional', 4),
    ( 8, 3, 'A. Conocimiento y preparación de la enseñanza', 1),
    ( 9, 3, 'B. Convivencia al interior del aula', 2),
    (10, 3, 'C. Transposición didáctica', 3),
    (11, 3, 'D. Responsabilidad y ética profesional', 4);

INSERT INTO indicador (id, dimension_id, numero, descripcion) VALUES
    (21,  4,  1, 'Comprende las bases curriculares y su respectivo programa de estudio de la disciplina que enseña.'),
    (22,  4,  2, 'Conoce las características del desarrollo correspondiente a las edades de sus estudiantes y sus necesidades educativas.'),
    (23,  4,  3, 'Incorpora en sus planificaciones variadas estrategias de enseñanza.'),
    (24,  4,  4, 'Elabora secuencias de contenidos coherentes con los objetivos de aprendizaje del marco curricular.'),
    (25,  5,  5, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (26,  5,  6, 'Presenta situaciones didácticas que motivan la participación de sus estudiantes.'),
    (27,  5,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (28,  5,  8, 'Utiliza estrategias para crear y mantener un ambiente sano y seguro al interior del aula.'),
    (29,  6,  9, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (30,  6, 10, 'Desarrolla los contenidos a través de una estrategia de enseñanza clara y definida.'),
    (31,  6, 11, 'Desarrolla los contenidos en forma clara, precisa y adecuada al nivel de las/os estudiantes.'),
    (32,  6, 12, 'Desarrolla los contenidos con una secuencia adecuada a la comprensión de las/os estudiantes.'),
    (33,  6, 13, 'Formula preguntas, problemas, y concede el tiempo necesario para resolverlos.'),
    (34,  6, 14, 'Utiliza estrategias pertinentes para evaluar el logro de los objetivos de aprendizaje definidos para sus clases.'),
    (35,  7, 15, 'Evalúa el grado en que los estudiantes alcanzaron los aprendizajes esperados.'),
    (36,  7, 16, 'Dialoga con su profesor/a colaborador/a sobre aspectos pedagógicos y didácticos del proceso enseñanza - aprendizaje.'),
    (37,  7, 17, 'Identifica las fortalezas de sus estudiantes y procura potenciarlas.'),
    (38,  7, 18, 'Demuestra sentido de responsabilidad frente a las tareas asignadas asociadas a su práctica profesional.'),
    (39,  7, 19, 'Presenta anticipadamente la planificación de su clase.'),
    (40,  8,  1, 'Comprende las bases curriculares y su respectivo programa de estudio de la disciplina que enseña.'),
    (41,  8,  2, 'Conoce las características del desarrollo correspondiente a las edades de sus estudiantes y sus necesidades educativas.'),
    (42,  8,  3, 'Incorpora en sus planificaciones variadas estrategias de enseñanza.'),
    (43,  8,  4, 'Elabora secuencias de contenidos coherentes con los objetivos de aprendizaje del marco curricular.'),
    (44,  9,  5, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (45,  9,  6, 'Presenta situaciones didácticas que motivan la participación de sus estudiantes.'),
    (46,  9,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (47,  9,  8, 'Utiliza estrategias para crear y mantener un ambiente sano y seguro al interior del aula.'),
    (48, 10,  9, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (49, 10, 10, 'Desarrolla los contenidos a través de una estrategia de enseñanza clara y definida.'),
    (50, 10, 11, 'Desarrolla los contenidos en forma clara, precisa y adecuada al nivel de las/os estudiantes.'),
    (51, 10, 12, 'Desarrolla los contenidos con una secuencia adecuada a la comprensión de las/os estudiantes.'),
    (52, 10, 13, 'Formula preguntas, problemas, y concede el tiempo necesario para resolverlos.'),
    (53, 10, 14, 'Utiliza estrategias pertinentes para evaluar el logro de los objetivos de aprendizaje definidos para sus clases.'),
    (54, 11, 15, 'Evalúa el grado en que los estudiantes alcanzaron los aprendizajes esperados.'),
    (55, 11, 16, 'Dialoga con su profesor/a colaborador/a sobre aspectos pedagógicos y didácticos del proceso enseñanza - aprendizaje.'),
    (56, 11, 17, 'Identifica las fortalezas de sus estudiantes y procura potenciarlas.'),
    (57, 11, 18, 'Demuestra sentido de responsabilidad frente a las tareas asignadas asociadas a su práctica profesional.'),
    (58, 11, 19, 'Presenta anticipadamente la planificación de su clase.');

-- Estructura evaluativa de Práctica Pedagógica (HU-09, migración 012): 8 componentes que suman 100 %.
-- Fuente: orientaciones de la Práctica Pedagógica 2026-2 (RESUMEN.md, sección 3.5). Las demás asignaturas las
-- configura el coordinador desde la página ("Nota final", SUPUESTO S-78).
INSERT INTO componente_nota (id, asignatura_id, nombre, peso, orden) VALUES
    (1, 5, 'Especialidad UBB: Producto 1 Expectativas',                   10.00, 1),
    (2, 5, 'Especialidad UBB: Producto 2 Contexto',                       17.00, 2),
    (3, 5, 'Colaborador Especialidad: pauta 1.º observación',              5.00, 3),
    (4, 5, 'Colaborador Especialidad: pauta 2.º observación',             13.00, 4),
    (5, 5, 'Colaborador Orientación: pauta 1.º observación',               3.00, 5),
    (6, 5, 'Colaborador Orientación: pauta 2.º observación',               5.00, 6),
    (7, 5, 'Integración curricular: TANEP',                               20.00, 7),
    (8, 5, 'Integración curricular: Informe final (Producto 3)',          27.00, 8);

-- Coordinador inicial: datos y hash vienen de las variables definidas al instalar.
-- El CHECK ck_usuario_password_bcrypt de la tabla rechaza cualquier cosa que no sea un hash BCrypt.
INSERT INTO usuario (rol_id, nombre, apellidos, email, password_hash, activo) VALUES
    (4, TRIM(@coordinador_nombre), TRIM(@coordinador_apellidos), LOWER(TRIM(@coordinador_email)), @coordinador_hash, TRUE);

COMMIT;

-- Resumen de lo cargado
SELECT (SELECT COUNT(*) FROM rol)                 AS roles,
       (SELECT COUNT(*) FROM asignatura_practica) AS asignaturas,
       (SELECT COUNT(*) FROM indicador)           AS indicadores_pauta,
       (SELECT COUNT(*) FROM componente_nota)     AS componentes_nota,
       (SELECT COUNT(*) FROM usuario)             AS usuarios;
