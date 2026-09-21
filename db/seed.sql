-- =============================================================================
-- seed.sql - Datos iniciales y de demostración (MySQL 8.x)
--
-- Se carga DESPUÉS de schema.sql. Tiene dos partes:
--   PARTE 1 - CATÁLOGO: datos reales que el sistema necesita siempre
--             (roles, las 6 asignaturas, la pauta de observación de clase).
--   PARTE 2 - DEMO: datos FICTICIOS para la presentación (S-15). Ninguna
--             persona, RUT, correo ni colegio es real: los correos usan el
--             dominio reservado demo.example y los RUT son de prueba.
--
-- CLAVE DE LOS USUARIOS FICTICIOS DE DEMOSTRACIÓN:  Demo2026!
--   * Es la contraseña de los usuarios ficticios de demostración (correos
--     @demo.example). Ninguna persona real la usa.
--   * Es SOLO para desarrollo y demostración: NO debe usarse en ningún sistema real.
--   * NUNCA debe aparecer como valor por defecto en el código de la aplicación.
--   * En la base de datos solo se guarda su hash BCrypt. La clave de MySQL NO está aquí.
-- =============================================================================

USE isw_practicas;
SET NAMES utf8mb4;

-- =============================================================================
-- PARTE 1 - CATÁLOGO
-- =============================================================================

-- Roles (HU-01)
INSERT INTO rol (id, nombre, descripcion) VALUES
    (1, 'ESTUDIANTE',     'Estudiante de Pedagogía en Educación Matemática que cursa las prácticas'),
    (2, 'PROFESOR_GUIA',  'Profesor colaborador del colegio que guía al estudiante y evalúa sus clases'),
    (3, 'PROFESOR_CURSO', 'Académico de la universidad que autoriza las clases (supuesto S-05)'),
    (4, 'COORDINADOR',    'Coordinador de prácticas: registra estudiantes, profesores e inscripciones');

-- Las 6 asignaturas de práctica (HU-02).
-- Códigos internos del proyecto, no oficiales de la UBB. El chatbot aplica solo a las dos finales.
-- Según un estudiante de PEM (reportado, pendiente de confirmar con el cliente; ver S-16):
--   * Aproximación al Sistema Escolar: grupos de 4 y NO entrega planificaciones (solo trabajos en clase).
--   * Ambientes de Aprendizaje: parejas y SÍ entrega planificaciones.
--   * Ninguna de las dos va a un colegio, por eso no tienen profesor guía (colaborador).
INSERT INTO asignatura_practica
    (id, codigo, nombre, orden, ciclo, modalidad, requiere_centro_educativo, chatbot_habilitado, requiere_planificacion) VALUES
    (1, 'PEM-P1', 'Aproximación al Sistema Escolar',    1, 'INICIAL',    'GRUPO',      FALSE, FALSE, FALSE),
    (2, 'PEM-P2', 'Ambientes de Aprendizaje',           2, 'INICIAL',    'DUPLA',      FALSE, FALSE, TRUE),
    (3, 'PEM-P3', 'Trabajo Colaborativo en el Aula',    3, 'INTERMEDIO', 'DUPLA',      TRUE,  FALSE, TRUE),
    (4, 'PEM-P4', 'Investigación en el Aula',           4, 'INTERMEDIO', 'DUPLA',      TRUE,  FALSE, TRUE),
    (5, 'PEM-P5', 'Práctica Pedagógica',                5, 'AVANZADO',   'INDIVIDUAL', TRUE,  TRUE,  TRUE),
    (6, 'PEM-P6', 'Práctica Profesional',               6, 'AVANZADO',   'INDIVIDUAL', TRUE,  TRUE,  TRUE);

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

-- =============================================================================
-- PARTE 2 - DEMO (datos ficticios, S-15)
-- =============================================================================

-- Hash BCrypt de la clave de los usuarios ficticios de demostración (costo 10).
-- Solo para desarrollo: no usar en ningún sistema real (ver el encabezado).
SET @pw_demo = '$2a$10$oYiGClqHFshZpKW/kdQIXeUU.LlcoFD47bh.VoUQwhvyRUqRtyxLO';

-- Usuarios: 1 coordinadora, 2 profesores guía, 2 profesores del curso, 5 estudiantes
INSERT INTO usuario (id, rol_id, nombre, apellidos, email, password_hash, activo, fecha_creacion) VALUES
    ( 1, 4, 'Andrea',    'Fuentes Lagos',    'coordinador@demo.example',        @pw_demo, TRUE, '2026-09-01 09:00:00'),
    ( 2, 2, 'Marcos',    'Bravo Tapia',      'guia1@demo.example',              @pw_demo, TRUE, '2026-09-01 09:05:00'),
    ( 3, 2, 'Lorena',    'Vidal Pizarro',    'guia2@demo.example',              @pw_demo, TRUE, '2026-09-01 09:06:00'),
    ( 4, 3, 'Ricardo',   'Núñez Salas',      'curso1@demo.example',             @pw_demo, TRUE, '2026-09-01 09:10:00'),
    ( 5, 3, 'Patricia',  'Contreras Mora',   'curso2@demo.example',             @pw_demo, TRUE, '2026-09-01 09:11:00'),
    ( 6, 1, 'Camila',    'Rojas Soto',       'camila.rojas@demo.example',       @pw_demo, TRUE, '2026-09-02 10:00:00'),
    ( 7, 1, 'Diego',     'Muñoz Araya',      'diego.munoz@demo.example',        @pw_demo, TRUE, '2026-09-02 10:01:00'),
    ( 8, 1, 'Valentina', 'Pérez Cortés',     'valentina.perez@demo.example',    @pw_demo, TRUE, '2026-09-02 10:02:00'),
    ( 9, 1, 'Sebastián', 'Vargas Leiva',     'sebastian.vargas@demo.example',   @pw_demo, TRUE, '2026-09-02 10:03:00'),
    (10, 1, 'Javiera',   'Herrera Ibáñez',   'javiera.herrera@demo.example',    @pw_demo, TRUE, '2026-09-02 10:04:00');

-- Estudiantes (RUT de prueba, formalmente válidos). Semestres coherentes con el periodo 2026-2.
INSERT INTO estudiante (id, usuario_id, rut, semestre_actual, anio_ingreso) VALUES
    (1,  6, '11111111-1',  6, 2024),   -- Camila:    cursa Trabajo Colaborativo
    (2,  7, '22222222-2',  6, 2024),   -- Diego:     cursa Trabajo Colaborativo
    (3,  8, '33333333-3',  8, 2023),   -- Valentina: cursa Práctica Pedagógica
    (4,  9, '44444444-4', 10, 2022),   -- Sebastián: cursa Práctica Profesional (recorrido completo)
    (5, 10, '55555555-5',  4, 2025);   -- Javiera:   recién inicia Aproximación al Sistema Escolar

-- Inscripciones. Guía nulo en el ciclo inicial (no va al colegio).
--   guía 2 = Marcos Bravo, guía 3 = Lorena Vidal; curso 4 = Ricardo Núñez, curso 5 = Patricia Contreras
INSERT INTO inscripcion (id, estudiante_id, asignatura_id, periodo, profesor_guia_id, profesor_curso_id, estado, nota_final) VALUES
    -- Camila
    ( 1, 1, 1, '2025-2', NULL, 4, 'APROBADA', 5.8),
    ( 2, 1, 2, '2026-1', NULL, 4, 'APROBADA', 6.1),
    ( 3, 1, 3, '2026-2', 2,    4, 'EN_CURSO', NULL),
    -- Diego
    ( 4, 2, 1, '2025-2', NULL, 5, 'APROBADA', 5.2),
    ( 5, 2, 2, '2026-1', NULL, 5, 'APROBADA', 5.6),
    ( 6, 2, 3, '2026-2', 3,    4, 'EN_CURSO', NULL),
    -- Valentina
    ( 7, 3, 1, '2024-2', NULL, 5, 'APROBADA', 6.0),
    ( 8, 3, 2, '2025-1', NULL, 5, 'APROBADA', 6.2),
    ( 9, 3, 3, '2025-2', 2,    4, 'APROBADA', 6.4),
    (10, 3, 4, '2026-1', 2,    4, 'APROBADA', 6.5),
    (11, 3, 5, '2026-2', 3,    5, 'EN_CURSO', NULL),
    -- Sebastián (recorrido completo de las 6 prácticas)
    (12, 4, 1, '2023-2', NULL, 4, 'APROBADA', 5.5),
    (13, 4, 2, '2024-1', NULL, 4, 'APROBADA', 5.9),
    (14, 4, 3, '2024-2', 2,    4, 'APROBADA', 6.0),
    (15, 4, 4, '2025-1', 3,    5, 'APROBADA', 6.3),
    (16, 4, 5, '2025-2', 3,    5, 'APROBADA', 6.6),
    (17, 4, 6, '2026-2', 3,    5, 'EN_CURSO', NULL),
    -- Javiera
    (18, 5, 1, '2026-2', NULL, 5, 'EN_CURSO', NULL);

-- -----------------------------------------------------------------------------
-- Planificaciones: hay una en cada estado del flujo para poder mostrarlas todas
--   1 y 2: históricas (AUTORIZADAS) enlazadas a evaluaciones pasadas
--   3 AUTORIZADA, 4 CON_OBSERVACIONES, 5 BORRADOR incompleto      (Camila)
--   6 EN_REVISION_GUIA, 7 EN_AUTORIZACION                          (Diego)
--   8 AUTORIZADA con ida y vuelta                                  (Valentina)
--   9 EN_REVISION_GUIA                                             (Sebastián)
-- -----------------------------------------------------------------------------
INSERT INTO planificacion
    (id, inscripcion_id, titulo, fecha_clase, objetivo, recursos, tiempos, duracion_minutos,
     evaluacion_aprendizaje, interaccion_participantes, estado, fecha_creacion, fecha_actualizacion) VALUES
    (1, 14, 'Ecuaciones lineales con una incógnita', '2024-10-16',
        'Resolver ecuaciones lineales con una incógnita aplicando las propiedades de la igualdad.',
        'Pizarra, guía de trabajo impresa y una balanza dibujada para representar la igualdad.',
        'Inicio 10 min (activación de conocimientos previos); desarrollo 60 min (ejemplos guiados y trabajo en parejas); cierre 20 min (síntesis y ticket de salida).',
        90,
        'Ticket de salida con dos ecuaciones y revisión del trabajo en parejas durante el desarrollo.',
        'El profesor modela en la pizarra; los estudiantes resuelven en parejas con la guía impresa y explican su procedimiento al curso.',
        'AUTORIZADA', '2024-10-07 16:00:00', '2024-10-11 12:00:00'),
    (2, 9, 'Razones y proporciones', '2025-10-08',
        'Reconocer y calcular razones y proporciones en situaciones cotidianas.',
        'Recetas de cocina impresas, cinta métrica y presentación de diapositivas.',
        'Inicio 15 min (situación de las recetas); desarrollo 55 min (trabajo en grupos); cierre 20 min (puesta en común y síntesis).',
        90,
        'Rúbrica breve de trabajo grupal y pregunta de cierre sobre cómo se comprueba una proporción.',
        'Los estudiantes miden y comparan cantidades en grupos de cuatro; el profesor circula planteando preguntas y los grupos exponen sus resultados.',
        'AUTORIZADA', '2025-09-29 15:30:00', '2025-10-06 11:00:00'),
    (3, 3, 'Ecuaciones de primer grado en contextos cotidianos', '2026-09-23',
        'Plantear y resolver ecuaciones de primer grado a partir de situaciones cotidianas.',
        'Guía de problemas contextualizados, tarjetas con situaciones y pizarra.',
        'Inicio 10 min (problema motivador); desarrollo 60 min (planteo y resolución en parejas); cierre 20 min (síntesis y ticket de salida).',
        90,
        'Ticket de salida con un problema por resolver y observación del trabajo en parejas.',
        'Las parejas leen las tarjetas, plantean la ecuación y validan su respuesta con otra pareja; el profesor retroalimenta con preguntas.',
        'AUTORIZADA', '2026-09-14 17:00:00', '2026-09-18 10:30:00'),
    (4, 3, 'Proporcionalidad directa', '2026-09-30',
        'Identificar relaciones de proporcionalidad directa y representarlas en una tabla y en un gráfico.',
        'Tablas de valores, papel cuadriculado y proyector.',
        'Inicio 10 min; desarrollo 60 min; cierre 20 min.',
        90,
        'Pregunta de cierre sobre cómo reconocer la proporcionalidad directa.',
        'Los estudiantes completan tablas en parejas y luego construyen el gráfico de forma individual.',
        'CON_OBSERVACIONES', '2026-09-15 18:00:00', '2026-09-18 15:00:00'),
    (5, 3, 'Funciones lineales', '2026-10-07',
        'Reconocer la función lineal como modelo de una relación de proporcionalidad.',
        NULL, NULL, NULL, NULL, NULL,
        'BORRADOR', '2026-09-19 20:00:00', '2026-09-19 20:00:00'),
    (6, 6, 'Teorema de Pitágoras', '2026-09-24',
        'Aplicar el teorema de Pitágoras para calcular longitudes en triángulos rectángulos.',
        'Geoplano, regla, guía de ejercicios y presentación de diapositivas.',
        'Inicio 15 min (verificación con geoplano); desarrollo 55 min (ejercicios guiados); cierre 20 min (síntesis).',
        90,
        'Lista de cotejo durante la práctica y dos ejercicios de cierre.',
        'Los estudiantes construyen triángulos en el geoplano y comparan áreas; luego resuelven ejercicios en parejas.',
        'EN_REVISION_GUIA', '2026-09-17 19:00:00', '2026-09-19 09:00:00'),
    (7, 6, 'Fracciones equivalentes', '2026-10-01',
        'Generar fracciones equivalentes por amplificación y simplificación.',
        'Tiras de fracciones, tarjetas y pizarra.',
        'Inicio 10 min; desarrollo 60 min; cierre 20 min.',
        90,
        'Ticket de salida con tres pares de fracciones por clasificar.',
        'Los estudiantes comparan tiras de fracciones en parejas y explican por qué dos fracciones son equivalentes.',
        'EN_AUTORIZACION', '2026-09-16 16:00:00', '2026-09-19 11:00:00'),
    (8, 11, 'Introducción a las funciones cuadráticas', '2026-10-06',
        'Reconocer la función cuadrática y su gráfica (parábola) a partir de situaciones de movimiento.',
        'Software de geometría dinámica, guía de exploración y proyector.',
        'Inicio 15 min (lanzamiento de un objeto); desarrollo 55 min (exploración de la gráfica); cierre 20 min (conclusiones).',
        90,
        'Informe breve de la exploración y pregunta de cierre sobre el efecto del coeficiente principal.',
        'Los estudiantes exploran la gráfica en el software en parejas, registran sus conjeturas y las contrastan con el curso.',
        'AUTORIZADA', '2026-09-12 18:00:00', '2026-09-18 12:00:00'),
    (9, 17, 'Sistemas de ecuaciones lineales 2x2', '2026-09-29',
        'Resolver sistemas de ecuaciones lineales de 2x2 por sustitución e igualación.',
        'Guía de problemas, pizarra y tarjetas de estrategias.',
        'Inicio 10 min; desarrollo 65 min; cierre 15 min.',
        90,
        'Problema de cierre resuelto por cada estudiante con la estrategia que elija.',
        'Los estudiantes comparan estrategias en grupos y presentan una al curso; el profesor sintetiza las diferencias.',
        'EN_REVISION_GUIA', '2026-09-18 17:00:00', '2026-09-19 08:30:00');

-- Historial de decisiones (el envío inicial del estudiante no genera revisión)
INSERT INTO revision_planificacion (id, planificacion_id, revisor_id, etapa, decision, comentario, fecha) VALUES
    ( 1, 1, 2, 'GUIA',  'APROBADA', 'Planificación clara y coherente con el objetivo.',                                                        '2024-10-09 11:00:00'),
    ( 2, 1, 4, 'CURSO', 'APROBADA', 'Autorizada.',                                                                                              '2024-10-11 12:00:00'),
    ( 3, 2, 2, 'GUIA',  'OBSERVADA','Falta explicitar cómo se verificará el logro del objetivo en el cierre de la clase.',                       '2025-10-01 10:00:00'),
    ( 4, 2, 2, 'GUIA',  'APROBADA', 'Se incorporó la verificación del objetivo en el cierre.',                                                  '2025-10-03 09:30:00'),
    ( 5, 2, 4, 'CURSO', 'APROBADA', 'Autorizada.',                                                                                              '2025-10-06 11:00:00'),
    ( 6, 3, 2, 'GUIA',  'APROBADA', 'Buenos contextos y tiempos realistas.',                                                                    '2026-09-16 12:00:00'),
    ( 7, 3, 4, 'CURSO', 'APROBADA', 'Autorizada.',                                                                                              '2026-09-18 10:30:00'),
    ( 8, 4, 2, 'GUIA',  'OBSERVADA','Incluir una actividad que use los errores de los estudiantes como devolución y precisar la evaluación del cierre.', '2026-09-18 15:00:00'),
    ( 9, 7, 3, 'GUIA',  'APROBADA', 'Buena secuencia; se envía al profesor del curso para su autorización.',                                    '2026-09-19 11:00:00'),
    (10, 8, 3, 'GUIA',  'OBSERVADA','Precisar los tiempos de la exploración y la pregunta de cierre.',                                          '2026-09-14 12:00:00'),
    (11, 8, 3, 'GUIA',  'APROBADA', 'Ajustes incorporados.',                                                                                    '2026-09-16 10:00:00'),
    (12, 8, 5, 'CURSO', 'APROBADA', 'Autorizada.',                                                                                              '2026-09-18 12:00:00');

-- -----------------------------------------------------------------------------
-- Evaluaciones históricas con la pauta digital (Trabajo Colaborativo).
-- Se insertan como BORRADOR, se cargan los 20 puntajes (el trigger valida cada
-- uno) y al final se finalizan calculando puntaje total y promedio.
--   1: Sebastián, 2024-2 (clase con planificación 1)
--   2 y 3: Valentina, 2025-2 (la 2 con planificación 2; la 3 sin planificación
--          enlazada, porque ese vínculo es opcional). Muestran evolución.
-- -----------------------------------------------------------------------------
INSERT INTO evaluacion
    (id, inscripcion_id, pauta_id, evaluador_id, planificacion_id, establecimiento, curso, fecha_clase,
     hora_inicio, hora_termino, objetivo_clase, observaciones_generales, estado) VALUES
    (1, 14, 1, 2, 1,    'Liceo Bicentenario Ejemplo', '1° Medio B', '2024-10-16', '10:15:00', '11:45:00',
        'Resolver ecuaciones lineales con una incógnita aplicando las propiedades de la igualdad.',
        'Fortalezas: buen manejo del contenido y clima de respeto. A mejorar: el cierre no verificó el logro del objetivo y faltó promover el trabajo colaborativo.',
        'BORRADOR'),
    (2, 9, 1, 2, 2,     'Colegio Ejemplo Las Acacias', '2° Medio A', '2025-10-08', '08:30:00', '10:00:00',
        'Reconocer y calcular razones y proporciones en situaciones cotidianas.',
        'Fortalezas: contextualización de las tareas y uso de material concreto. A mejorar: dar más tiempo a la puesta en común de los grupos.',
        'BORRADOR'),
    (3, 9, 1, 2, NULL,  'Colegio Ejemplo Las Acacias', '2° Medio A', '2025-10-22', '08:30:00', '10:00:00',
        'Interpretar la constante de proporcionalidad en tablas y gráficos.',
        'Fortalezas: cierre con verificación del objetivo y trabajo colaborativo bien conducido. Mejora notable respecto de la clase anterior.',
        'BORRADOR');

-- ELT(numero, ...) asigna a cada indicador (1 a 20) el puntaje de su posición.
INSERT INTO respuesta_indicador (evaluacion_id, indicador_id, puntaje)
SELECT 1, i.id, ELT(i.numero, 4,5,4,4,3,3, 5,4,4,3,5,3, 5,5,4,5,6,5,4,4)
  FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id WHERE d.pauta_id = 1;

INSERT INTO respuesta_indicador (evaluacion_id, indicador_id, puntaje)
SELECT 2, i.id, ELT(i.numero, 5,5,5,4,4,4, 5,5,4,4,5,4, 6,5,5,5,6,5,5,5)
  FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id WHERE d.pauta_id = 1;

INSERT INTO respuesta_indicador (evaluacion_id, indicador_id, puntaje)
SELECT 3, i.id, ELT(i.numero, 6,6,6,5,6,5, 6,6,5,5,6,6, 7,6,6,6,7,6,6,6)
  FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id WHERE d.pauta_id = 1;

-- Finalizar: el puntaje total y el promedio se calculan y se guardan (S-08)
UPDATE evaluacion e
  JOIN (SELECT evaluacion_id,
               SUM(puntaje)           AS total,
               ROUND(AVG(puntaje), 2) AS prom
          FROM respuesta_indicador
         GROUP BY evaluacion_id) r ON r.evaluacion_id = e.id
   SET e.estado             = 'FINALIZADA',
       e.puntaje_total      = r.total,
       e.promedio           = r.prom,
       e.fecha_finalizacion = TIMESTAMP(e.fecha_clase, '13:00:00')
 WHERE e.id IN (1, 2, 3);

-- NOTA_VOZ y MENSAJE_CHATBOT quedan vacías: su API llega en sprints posteriores.
