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

-- HU-07 (2026-11-21): las otras 10 pautas de Especialidad y Orientación de las dos prácticas finales. Fuentes:
-- "Pautas de evaluación Práctica Profesional 2025-2" (carpetas PROFESOR(A) COLABORADOR(A) y TUTOR(A)) y "Pautas
-- Práctica Pedagógica 2025-2" (carpetas Especialidad y Orientación, solo colaborador: en Pedagógica no hay tutor).
-- Textos copiados de los .docx. Se corrigieron 3 errores de tipeo del original (restos de un reemplazo mal hecho en
-- el Word), con el texto de las pautas de Práctica Profesional: "PEDAGÓGICAes" -> "profesionales", "desempeño
-- PEDAGÓGICA" -> "desempeño profesional" y "las los estudiantes" -> "las/os estudiantes". Escala 2 a 7; Especialidad con dimensiones A a D y
-- Orientación con A a E (así vienen impresas). Indicadores numerados corridos en toda la pauta (S-56). El puntaje
-- máximo coincide con el "PUNTAJE TOTAL" impreso de cada pauta: 1.ª Especialidad 133, 2.ª Especialidad 161,
-- 1.ª Orientación 126, 2.ª Orientación 161.
INSERT INTO pauta (id, asignatura_id, nombre, tipo_evaluador, ambito, numero_observacion, escala_min, escala_max, activa) VALUES
    (4, 6, 'Pauta de Especialidad, 2.ª observación - Profesor/a colaborador/a (Práctica Profesional 2025-2)',
        'COLABORADOR', 'ESPECIALIDAD', 2, 2, 7, TRUE),
    (5, 6, 'Pauta de Especialidad, 2.ª observación - Tutor/a (Práctica Profesional 2025-2)',
        'TUTOR', 'ESPECIALIDAD', 2, 2, 7, TRUE),
    (6, 6, 'Pauta de Orientación, 1.ª observación - Profesor/a colaborador/a (Práctica Profesional 2025-2)',
        'COLABORADOR', 'ORIENTACION', 1, 2, 7, TRUE),
    (7, 6, 'Pauta de Orientación, 1.ª observación - Tutor/a (Práctica Profesional 2025-2)',
        'TUTOR', 'ORIENTACION', 1, 2, 7, TRUE),
    (8, 6, 'Pauta de Orientación, 2.ª observación - Profesor/a colaborador/a (Práctica Profesional 2025-2)',
        'COLABORADOR', 'ORIENTACION', 2, 2, 7, TRUE),
    (9, 6, 'Pauta de Orientación, 2.ª observación - Tutor/a (Práctica Profesional 2025-2)',
        'TUTOR', 'ORIENTACION', 2, 2, 7, TRUE),
    (10, 5, 'Pauta de Especialidad, 1.ª observación - Profesor/a colaborador/a (Práctica Pedagógica 2025-2)',
        'COLABORADOR', 'ESPECIALIDAD', 1, 2, 7, TRUE),
    (11, 5, 'Pauta de Especialidad, 2.ª observación - Profesor/a colaborador/a (Práctica Pedagógica 2025-2)',
        'COLABORADOR', 'ESPECIALIDAD', 2, 2, 7, TRUE),
    (12, 5, 'Pauta de Orientación, 1.ª observación - Profesor/a colaborador/a (Práctica Pedagógica 2025-2)',
        'COLABORADOR', 'ORIENTACION', 1, 2, 7, TRUE),
    (13, 5, 'Pauta de Orientación, 2.ª observación - Profesor/a colaborador/a (Práctica Pedagógica 2025-2)',
        'COLABORADOR', 'ORIENTACION', 2, 2, 7, TRUE);

INSERT INTO dimension_pauta (id, pauta_id, nombre, orden) VALUES
    (12,  4, 'A. Conocimiento y preparación de la enseñanza', 1),
    (13,  4, 'B. Convivencia al interior del aula', 2),
    (14,  4, 'C. Transposición didáctica', 3),
    (15,  4, 'D. Responsabilidad y ética profesional', 4),
    (16,  5, 'A. Conocimiento y preparación de la enseñanza', 1),
    (17,  5, 'B. Convivencia al interior del aula', 2),
    (18,  5, 'C. Transposición didáctica', 3),
    (19,  5, 'D. Responsabilidad y ética profesional', 4),
    (20,  6, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (21,  6, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (22,  6, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (23,  6, 'D. Evaluación de los aprendizajes', 4),
    (24,  6, 'E. Evaluación de su desempeño', 5),
    (25,  7, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (26,  7, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (27,  7, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (28,  7, 'D. Evaluación de los aprendizajes', 4),
    (29,  7, 'E. Evaluación de su desempeño', 5),
    (30,  8, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (31,  8, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (32,  8, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (33,  8, 'D. Evaluación de los aprendizajes', 4),
    (34,  8, 'E. Evaluación de su desempeño', 5),
    (35,  9, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (36,  9, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (37,  9, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (38,  9, 'D. Evaluación de los aprendizajes', 4),
    (39,  9, 'E. Evaluación de su desempeño', 5),
    (40, 10, 'A. Conocimiento y preparación de la enseñanza', 1),
    (41, 10, 'B. Convivencia al interior del aula', 2),
    (42, 10, 'C. Transposición didáctica', 3),
    (43, 10, 'D. Responsabilidad y ética profesional', 4),
    (44, 11, 'A. Conocimiento y preparación de la enseñanza', 1),
    (45, 11, 'B. Convivencia al interior del aula', 2),
    (46, 11, 'C. Transposición didáctica', 3),
    (47, 11, 'D. Responsabilidad y ética pedagógica', 4),
    (48, 12, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (49, 12, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (50, 12, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (51, 12, 'D. Evaluación de los aprendizajes', 4),
    (52, 12, 'E. Evaluación de su desempeño', 5),
    (53, 13, 'A. Diseño de la clase - Planificación de la enseñanza', 1),
    (54, 13, 'B. Generación de ambiente propicio para el aprendizaje', 2),
    (55, 13, 'C. Uso de metodologías y didáctica para la enseñanza del/de la profesor/a en formación', 3),
    (56, 13, 'D. Evaluación de los aprendizajes', 4),
    (57, 13, 'E. Evaluación de su desempeño', 5);

INSERT INTO indicador (id, dimension_id, numero, descripcion) VALUES
    ( 59, 12,  1, 'Demuestra conocimiento la relación de los contenidos que enseña y los vincula a la realidad personal de las y los estudiantes.'),
    ( 60, 12,  2, 'Domina los principios de las bases curriculares y logra alcanzar los objetivos de aprendizaje propuestos durante sus clases.'),
    ( 61, 12,  3, 'Identifica las fortalezas y debilidades de las y los estudiantes, respecto de los contenidos que se ha propuesto enseñar.'),
    ( 62, 12,  4, 'Distingue los distintos estilos y ritmos de aprendizaje de las y los estudiantes.'),
    ( 63, 12,  5, 'Selecciona distintos recursos de aprendizajes relacionados con los contenidos curriculares, considerando las características de las y los estudiantes.'),
    ( 64, 12,  6, 'Establece estrategias de evaluación que permitan oportunidades equitativas a los estudiantes, en relación al aprendizaje.'),
    ( 65, 13,  7, 'Demuestra actitudes de compromiso, responsabilidad y solidaridad con las y los estudiantes durante las clases.'),
    ( 66, 13,  8, 'Demuestra respeto por la diversidad cultural, étnica, de género y socioeconómica del estudiantado.'),
    ( 67, 13,  9, 'Favorece el desarrollo de la autonomía de las y los estudiantes en las diferentes situaciones de aprendizaje que les propone.'),
    ( 68, 13, 10, 'Promueve el esfuerzo y perseverancia en las y los estudiantes para alcanzar el aprendizaje.'),
    ( 69, 13, 11, 'Modela estrategias de diálogo y resolución de conflictos al interior del aula.'),
    ( 70, 14, 12, 'Socializa con las y los estudiantes los criterios que los orientarán tanto para autoevaluarse como para ser evaluados/as.'),
    ( 71, 14, 13, 'Propone actividades que involucran el desarrollo cognitivo y emocional de las y los estudiantes.'),
    ( 72, 14, 14, 'Desarrolla los contenidos curriculares en forma clara, precisa y adecuada al nivel y a las condiciones personales de las y los estudiantes.'),
    ( 73, 14, 15, 'Utiliza lenguaje preciso y pertinente de manera gradual.'),
    ( 74, 14, 16, 'Utiliza estrategias de retroalimentación que permiten a las/los estudiantes tomar conciencia de sus logros de aprendizaje.'),
    ( 75, 14, 17, 'Adapta las actividades de enseñanza de acuerdo a las evidencias que recoge sobre los aprendizajes de las y los estudiantes.'),
    ( 76, 14, 18, 'Utiliza recursos didácticos coherentes con las actividades de aprendizaje, facilitando los objetivos de aprendizaje de las y los estudiantes.'),
    ( 77, 15, 19, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    ( 78, 15, 20, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    ( 79, 15, 21, 'Cumple con las normas y responsabilidades profesionales establecidas en la institución educativa donde trabaja, para la realización de las tareas comprometidas.'),
    ( 80, 15, 22, 'Demuestra un trabajo asertivo frente a las dificultades que se presentan en el aula.'),
    ( 81, 15, 23, 'Presenta anticipadamente la planificación de su clase.'),
    ( 82, 16,  1, 'Demuestra conocimiento la relación de los contenidos que enseña y los vincula a la realidad personal de las y los estudiantes.'),
    ( 83, 16,  2, 'Domina los principios de las bases curriculares y logra alcanzar los objetivos de aprendizaje propuestos durante sus clases.'),
    ( 84, 16,  3, 'Identifica las fortalezas y debilidades de las y los estudiantes, respecto de los contenidos que se ha propuesto enseñar.'),
    ( 85, 16,  4, 'Distingue los distintos estilos y ritmos de aprendizaje de las y los estudiantes.'),
    ( 86, 16,  5, 'Selecciona distintos recursos de aprendizajes relacionados con los contenidos curriculares, considerando las características de las y los estudiantes.'),
    ( 87, 16,  6, 'Establece estrategias de evaluación que permitan oportunidades equitativas a los estudiantes, en relación al aprendizaje.'),
    ( 88, 17,  7, 'Demuestra actitudes de compromiso, responsabilidad y solidaridad con las y los estudiantes durante las clases.'),
    ( 89, 17,  8, 'Demuestra respeto por la diversidad cultural, étnica, de género y socioeconómica del estudiantado.'),
    ( 90, 17,  9, 'Favorece el desarrollo de la autonomía de las y los estudiantes en las diferentes situaciones de aprendizaje que les propone.'),
    ( 91, 17, 10, 'Promueve el esfuerzo y perseverancia en las y los estudiantes para alcanzar el aprendizaje.'),
    ( 92, 17, 11, 'Modela estrategias de diálogo y resolución de conflictos al interior del aula.'),
    ( 93, 18, 12, 'Socializa con las y los estudiantes los criterios que los orientarán tanto para autoevaluarse como para ser evaluados/as.'),
    ( 94, 18, 13, 'Propone actividades que involucran el desarrollo cognitivo y emocional de las y los estudiantes.'),
    ( 95, 18, 14, 'Desarrolla los contenidos curriculares en forma clara, precisa y adecuada al nivel y a las condiciones personales de las y los estudiantes.'),
    ( 96, 18, 15, 'Utiliza lenguaje preciso y pertinente de manera gradual.'),
    ( 97, 18, 16, 'Utiliza estrategias de retroalimentación que permiten a las/los estudiantes tomar conciencia de sus logros de aprendizaje.'),
    ( 98, 18, 17, 'Adapta las actividades de enseñanza de acuerdo a las evidencias que recoge sobre los aprendizajes de las y los estudiantes.'),
    ( 99, 18, 18, 'Utiliza recursos didácticos coherentes con las actividades de aprendizaje, facilitando los objetivos de aprendizaje de las y los estudiantes.'),
    (100, 19, 19, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (101, 19, 20, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (102, 19, 21, 'Cumple con las normas y responsabilidades profesionales establecidas en la institución educativa donde trabaja, para la realización de las tareas comprometidas.'),
    (103, 19, 22, 'Demuestra un trabajo asertivo frente a las dificultades que se presentan en el aula.'),
    (104, 19, 23, 'Presenta anticipadamente la planificación de su clase.'),
    (105, 20,  1, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (106, 20,  2, 'Utiliza el tiempo disponible para la enseñanza en función de los objetivos de la clase.'),
    (107, 20,  3, 'Especifica en las unidades pedagógicas de orientación, los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (108, 21,  4, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (109, 21,  5, 'Posee la capacidad de empatía, de atención personalizada propiciada por la clase y escucha activamente a los y las estudiantes.'),
    (110, 21,  6, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (111, 21,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (112, 22,  8, 'Utiliza diferentes metodologías pedagógicas, que motiven el interés de los y las estudiantes durante las clases.'),
    (113, 22,  9, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (114, 22, 10, 'Demuestra conocer variadas estrategias de enseñanza y actividades congruentes con el proceso de enseñanza y aprendizaje.'),
    (115, 23, 11, 'Emplea instrumentos de evaluación, acorde a las temáticas desarrolladas en el grupo curso.'),
    (116, 23, 12, 'Utiliza criterios de evaluación coherentes con los objetivos de aprendizaje propuestos.'),
    (117, 23, 13, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (118, 24, 14, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (119, 24, 15, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (120, 24, 16, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (121, 24, 17, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (122, 24, 18, 'Presenta anticipadamente la planificación de la clase, de acuerdo a la unidad didáctica, para su debida retroalimentación.'),
    (123, 25,  1, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (124, 25,  2, 'Utiliza el tiempo disponible para la enseñanza en función de los objetivos de la clase.'),
    (125, 25,  3, 'Especifica en las unidades pedagógicas de orientación, los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (126, 26,  4, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (127, 26,  5, 'Posee la capacidad de empatía, de atención personalizada propiciada por la clase y escucha activamente a los y las estudiantes.'),
    (128, 26,  6, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (129, 26,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (130, 27,  8, 'Utiliza diferentes metodologías pedagógicas, que motiven el interés de los y las estudiantes durante las clases.'),
    (131, 27,  9, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (132, 27, 10, 'Demuestra conocer variadas estrategias de enseñanza y actividades congruentes con el proceso de enseñanza y aprendizaje.'),
    (133, 28, 11, 'Emplea instrumentos de evaluación, acorde a las temáticas desarrolladas en el grupo curso.'),
    (134, 28, 12, 'Utiliza criterios de evaluación coherentes con los objetivos de aprendizaje propuestos.'),
    (135, 28, 13, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (136, 29, 14, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (137, 29, 15, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (138, 29, 16, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (139, 29, 17, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (140, 29, 18, 'Presenta anticipadamente la planificación de la clase, de acuerdo a la unidad didáctica, para su debida retroalimentación.'),
    (141, 30,  1, 'Contextualiza el diseño de unidades didácticas coherentes a temáticas a desarrollar según la base curricular y el programa de estudio.'),
    (142, 30,  2, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (143, 30,  3, 'Desarrolla los contenidos de la clase especificando los aprendizajes que se desean alcanzar.'),
    (144, 30,  4, 'Utiliza un lenguaje y conceptos de manera precisa y comprensible para sus estudiantes.'),
    (145, 30,  5, 'Estructura las situaciones de aprendizajes considerando los saberes, intereses y experiencia de los y las estudiantes.'),
    (146, 30,  6, 'Planifica Unidad Pedagógica, a partir de los Conocimientos, Habilidades y Actitudes, en función de las experiencias previas, y el aporte reflexivo como desafío personal.'),
    (147, 30,  7, 'Especifica en las unidades pedagógicas de orientación: los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (148, 31,  8, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (149, 31,  9, 'Proporciona a los y las estudiantes oportunidades de participación.'),
    (150, 31, 10, 'Estimula la sensibilización y socialización en la interacción durante la clase.'),
    (151, 31, 11, 'Demuestra capacidades para crear y mantener un clima grato durante el desarrollo de la clase.'),
    (152, 32, 12, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (153, 32, 13, 'Aplica estrategias metodológicas activas y desafiantes para el grupo curso, involucrando dimensiones cognitivas, procedimentales y emocionales, que capten el interés y la motivación de todos los/as estudiantes.'),
    (154, 32, 14, 'Considera en el proceso didáctico, el respeto por las diferencias de géneros, culturas, y etnias.'),
    (155, 32, 15, 'Utiliza estrategias metodologías de enseñanza para generar aprendizajes significativos.'),
    (156, 33, 16, 'Los procedimientos de evaluación son coherentes con la complejidad de los contenidos, incluidos en pautas.'),
    (157, 33, 17, 'Emplea la retroalimentación que permite a las/os estudiantes tomar conciencia de sus logros de aprendizaje.'),
    (158, 33, 18, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (159, 34, 19, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (160, 34, 20, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (161, 34, 21, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (162, 34, 22, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (163, 34, 23, 'Presenta anticipadamente la planificación de la clase, de acuerdo a unidad didáctica, para su debida retroalimentación.'),
    (164, 35,  1, 'Contextualiza el diseño de unidades didácticas coherentes a temáticas a desarrollar según la base curricular y el programa de estudio.'),
    (165, 35,  2, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (166, 35,  3, 'Desarrolla los contenidos de la clase especificando los aprendizajes que se desean alcanzar.'),
    (167, 35,  4, 'Utiliza un lenguaje y conceptos de manera precisa y comprensible para sus estudiantes.'),
    (168, 35,  5, 'Estructura las situaciones de aprendizajes considerando los saberes, intereses y experiencia de los y las estudiantes.'),
    (169, 35,  6, 'Planifica Unidad Pedagógica, a partir de los Conocimientos, Habilidades y Actitudes, en función de las experiencias previas, y el aporte reflexivo como desafío personal.'),
    (170, 35,  7, 'Especifica en las unidades pedagógicas de orientación: los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (171, 36,  8, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (172, 36,  9, 'Proporciona a los y las estudiantes oportunidades de participación.'),
    (173, 36, 10, 'Estimula la sensibilización y socialización en la interacción durante la clase.'),
    (174, 36, 11, 'Demuestra capacidades para crear y mantener un clima grato durante el desarrollo de la clase.'),
    (175, 37, 12, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (176, 37, 13, 'Aplica estrategias metodológicas activas y desafiantes para el grupo curso, involucrando dimensiones cognitivas, procedimentales y emocionales, que capten el interés y la motivación de todos los/as estudiantes.'),
    (177, 37, 14, 'Considera en el proceso didáctico, el respeto por las diferencias de géneros, culturas, y etnias.'),
    (178, 37, 15, 'Utiliza estrategias metodologías de enseñanza para generar aprendizajes significativos.'),
    (179, 38, 16, 'Los procedimientos de evaluación son coherentes con la complejidad de los contenidos, incluidos en pautas.'),
    (180, 38, 17, 'Emplea la retroalimentación que permite a las/os estudiantes tomar conciencia de sus logros de aprendizaje.'),
    (181, 38, 18, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (182, 39, 19, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (183, 39, 20, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (184, 39, 21, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (185, 39, 22, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (186, 39, 23, 'Presenta anticipadamente la planificación de la clase, de acuerdo a unidad didáctica, para su debida retroalimentación.'),
    (187, 40,  1, 'Comprende las bases curriculares y su respectivo programa de estudio de la disciplina que enseña.'),
    (188, 40,  2, 'Conoce las características del desarrollo correspondiente a las edades de sus estudiantes y sus necesidades educativas.'),
    (189, 40,  3, 'Incorpora en sus planificaciones variadas estrategias de enseñanza.'),
    (190, 40,  4, 'Elabora secuencias de contenidos coherentes con los objetivos de aprendizaje del marco curricular.'),
    (191, 41,  5, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (192, 41,  6, 'Presenta situaciones didácticas que motivan la participación de sus estudiantes.'),
    (193, 41,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (194, 41,  8, 'Utiliza estrategias para crear y mantener un ambiente sano y seguro al interior del aula.'),
    (195, 42,  9, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (196, 42, 10, 'Desarrolla los contenidos a través de una estrategia de enseñanza clara y definida.'),
    (197, 42, 11, 'Desarrolla los contenidos en forma clara, precisa y adecuada al nivel de las/os estudiantes.'),
    (198, 42, 12, 'Desarrolla los contenidos con una secuencia adecuada a la comprensión de las/os estudiantes.'),
    (199, 42, 13, 'Formula preguntas, problemas, y concede el tiempo necesario para resolverlos.'),
    (200, 42, 14, 'Utiliza estrategias pertinentes para evaluar el logro de los objetivos de aprendizaje definidos para sus clases.'),
    (201, 43, 15, 'Evalúa el grado en que los estudiantes alcanzaron los aprendizajes esperados.'),
    (202, 43, 16, 'Dialoga con su profesor/a colaborador/a sobre aspectos pedagógicos y didácticos del proceso enseñanza - aprendizaje.'),
    (203, 43, 17, 'Identifica las fortalezas de sus estudiantes y procura potenciarlas.'),
    (204, 43, 18, 'Demuestra sentido de responsabilidad frente a las tareas asignadas asociadas a su práctica profesional.'),
    (205, 43, 19, 'Presenta anticipadamente la planificación de su clase.'),
    (206, 44,  1, 'Demuestra conocimiento la relación de los contenidos que enseña y los vincula a la realidad personal de las y los estudiantes.'),
    (207, 44,  2, 'Domina los principios de las bases curriculares y logra alcanzar los objetivos de aprendizaje propuestos durante sus clases.'),
    (208, 44,  3, 'Identifica las fortalezas y debilidades de las y los estudiantes, respecto de los contenidos que se ha propuesto enseñar.'),
    (209, 44,  4, 'Distingue los distintos estilos y ritmos de aprendizaje de las y los estudiantes.'),
    (210, 44,  5, 'Selecciona distintos recursos de aprendizajes relacionados con los contenidos curriculares, considerando las características de las y los estudiantes.'),
    (211, 44,  6, 'Establece estrategias de evaluación que permitan oportunidades equitativas a los estudiantes, en relación al aprendizaje.'),
    (212, 45,  7, 'Demuestra actitudes de compromiso, responsabilidad y solidaridad con las y los estudiantes durante las clases.'),
    (213, 45,  8, 'Demuestra respeto por la diversidad cultural, étnica, de género y socioeconómica del estudiantado.'),
    (214, 45,  9, 'Favorece el desarrollo de la autonomía de las y los estudiantes en las diferentes situaciones de aprendizaje que les propone.'),
    (215, 45, 10, 'Promueve el esfuerzo y perseverancia en las y los estudiantes para alcanzar el aprendizaje.'),
    (216, 45, 11, 'Modela estrategias de diálogo y resolución de conflictos al interior del aula.'),
    (217, 46, 12, 'Socializa con las y los estudiantes los criterios que los orientarán tanto para autoevaluarse como para ser evaluados/as.'),
    (218, 46, 13, 'Propone actividades que involucran el desarrollo cognitivo y emocional de las y los estudiantes.'),
    (219, 46, 14, 'Desarrolla los contenidos curriculares en forma clara, precisa y adecuada al nivel y a las condiciones personales de las y los estudiantes.'),
    (220, 46, 15, 'Utiliza lenguaje preciso y pertinente de manera gradual.'),
    (221, 46, 16, 'Utiliza estrategias de retroalimentación que permiten a las/los estudiantes tomar conciencia de sus logros de aprendizaje.'),
    (222, 46, 17, 'Adapta las actividades de enseñanza de acuerdo a las evidencias que recoge sobre los aprendizajes de las y los estudiantes.'),
    (223, 46, 18, 'Utiliza recursos didácticos coherentes con las actividades de aprendizaje, facilitando los objetivos de aprendizaje de las y los estudiantes.'),
    (224, 47, 19, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (225, 47, 20, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (226, 47, 21, 'Cumple con las normas y responsabilidades profesionales establecidas en la institución educativa donde trabaja, para la realización de las tareas comprometidas.'),
    (227, 47, 22, 'Demuestra un trabajo asertivo frente a las dificultades que se presentan en el aula.'),
    (228, 47, 23, 'Presenta anticipadamente la planificación de su clase.'),
    (229, 48,  1, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (230, 48,  2, 'Utiliza el tiempo disponible para la enseñanza en función de los objetivos de la clase.'),
    (231, 48,  3, 'Especifica en las unidades pedagógicas de orientación, los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (232, 49,  4, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (233, 49,  5, 'Posee la capacidad de empatía, de atención personalizada propiciada por la clase y escucha activamente a los y las estudiantes.'),
    (234, 49,  6, 'Establece un clima de relaciones interpersonales respetuosas y empáticas con sus estudiantes.'),
    (235, 49,  7, 'Establece normas de comportamiento que son conocidas y comprensibles para sus estudiantes.'),
    (236, 50,  8, 'Utiliza diferentes metodologías pedagógicas, que motiven el interés de los y las estudiantes durante las clases.'),
    (237, 50,  9, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (238, 50, 10, 'Demuestra conocer variadas estrategias de enseñanza y actividades congruentes con el proceso de enseñanza y aprendizaje.'),
    (239, 51, 11, 'Emplea instrumentos de evaluación, acorde a las temáticas desarrolladas en el grupo curso.'),
    (240, 51, 12, 'Utiliza criterios de evaluación coherentes con los objetivos de aprendizaje propuestos.'),
    (241, 51, 13, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (242, 52, 14, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (243, 52, 15, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (244, 52, 16, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (245, 52, 17, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (246, 52, 18, 'Presenta anticipadamente la planificación de la clase, de acuerdo a la unidad didáctica, para su debida retroalimentación.'),
    (247, 53,  1, 'Contextualiza el diseño de unidades didácticas coherentes a temáticas a desarrollar según la base curricular y el programa de estudio.'),
    (248, 53,  2, 'Comunica a las/os estudiantes los propósitos de la clase y los aprendizajes a lograr.'),
    (249, 53,  3, 'Desarrolla los contenidos de la clase especificando los aprendizajes que se desean alcanzar.'),
    (250, 53,  4, 'Utiliza un lenguaje y conceptos de manera precisa y comprensible para sus estudiantes.'),
    (251, 53,  5, 'Estructura las situaciones de aprendizajes considerando los saberes, intereses y experiencia de los y las estudiantes.'),
    (252, 53,  6, 'Planifica Unidad Pedagógica, a partir de los Conocimientos, Habilidades y Actitudes, en función de las experiencias previas, y el aporte reflexivo como desafío personal.'),
    (253, 53,  7, 'Especifica en las unidades pedagógicas de orientación: los tres momentos de la clase: inicio, desarrollo y cierre.'),
    (254, 54,  8, 'Formula preguntas y concede el tiempo necesario para resolverlos.'),
    (255, 54,  9, 'Proporciona a los y las estudiantes oportunidades de participación.'),
    (256, 54, 10, 'Estimula la sensibilización y socialización en la interacción durante la clase.'),
    (257, 54, 11, 'Demuestra capacidades para crear y mantener un clima grato durante el desarrollo de la clase.'),
    (258, 55, 12, 'Ofrece oportunidades y desafíos de participación activas a los y las estudiantes del grupo curso, considerando su diversidad.'),
    (259, 55, 13, 'Aplica estrategias metodológicas activas y desafiantes para el grupo curso, involucrando dimensiones cognitivas, procedimentales y emocionales, que capten el interés y la motivación de todos los/as estudiantes.'),
    (260, 55, 14, 'Considera en el proceso didáctico, el respeto por las diferencias de géneros, culturas, y etnias.'),
    (261, 55, 15, 'Utiliza estrategias metodologías de enseñanza para generar aprendizajes significativos.'),
    (262, 56, 16, 'Los procedimientos de evaluación son coherentes con la complejidad de los contenidos, incluidos en pautas.'),
    (263, 56, 17, 'Emplea la retroalimentación que permite a las/os estudiantes tomar conciencia de sus logros de aprendizaje.'),
    (264, 56, 18, 'Evalúa el grado en que las/os estudiantes alcanzaron los aprendizajes esperados.'),
    (265, 57, 19, 'Reflexiona sobre la propia práctica pedagógica, considerando la capacidad de preparar y gestionar curricularmente sus clases en el área de orientación, considerando la potenciación de sus fortalezas y la mejora de sus debilidades docentes.'),
    (266, 57, 20, 'Evalúa el nivel de logro de los objetivos de aprendizaje planteados, valorando los aciertos, analizando los errores y planteándose metas para su desempeño profesional futuro.'),
    (267, 57, 21, 'Evidencia el cumplimiento de los compromisos contraídos, presentación personal, asistencia, puntualidad en los talleres de reflexión y en la práctica en general.'),
    (268, 57, 22, 'Analiza críticamente su desempeño durante las clases, a partir de los resultados de aprendizaje de las y los estudiantes.'),
    (269, 57, 23, 'Presenta anticipadamente la planificación de la clase, de acuerdo a unidad didáctica, para su debida retroalimentación.');

-- Coordinador inicial: datos y hash vienen de las variables definidas al instalar.
-- El CHECK ck_usuario_password_bcrypt de la tabla rechaza cualquier cosa que no sea un hash BCrypt.
INSERT INTO usuario (rol_id, nombre, apellidos, email, password_hash, activo) VALUES
    (4, TRIM(@coordinador_nombre), TRIM(@coordinador_apellidos), LOWER(TRIM(@coordinador_email)), @coordinador_hash, TRUE);

COMMIT;

-- Resumen de lo cargado
SELECT (SELECT COUNT(*) FROM rol)                 AS roles,
       (SELECT COUNT(*) FROM asignatura_practica) AS asignaturas,
       (SELECT COUNT(*) FROM indicador)           AS indicadores_pauta,
       (SELECT COUNT(*) FROM usuario)             AS usuarios;
