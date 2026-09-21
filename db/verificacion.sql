-- =============================================================================
-- verificacion.sql - Consultas de solo lectura para comprobar la base de datos
--
-- Se ejecuta con:  .\db\bd.ps1 -Accion Verificar
-- o a mano:        mysql -u isw_app -p isw_practicas < db\verificacion.sql
--                  (la clave de isw_app está en la variable de entorno ISW_DB_PASSWORD)
--
-- Cada bloque dice qué resultado se espera. No modifica nada.
-- =============================================================================

SELECT CONCAT('Conectado como ', CURRENT_USER(), ' a la base ', DATABASE(), ' (MySQL ', VERSION(), ')') AS conexion;

-- -----------------------------------------------------------------------------
SELECT '1. ESTRUCTURA: cantidad de tablas (esperado: 14)' AS verificacion;
SELECT COUNT(*) AS tablas
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT '2. FILAS POR TABLA (esperado: rol 4, usuario 10, estudiante 5, asignatura 6, inscripcion 18, pauta 1, dimension 3, indicador 20, planificacion 9, revision 12, evaluacion 3, respuesta 60, nota_voz 0, mensaje 0)' AS verificacion;
SELECT 'rol' AS tabla, COUNT(*) AS filas FROM rol
UNION ALL SELECT 'usuario',                COUNT(*) FROM usuario
UNION ALL SELECT 'estudiante',             COUNT(*) FROM estudiante
UNION ALL SELECT 'asignatura_practica',    COUNT(*) FROM asignatura_practica
UNION ALL SELECT 'inscripcion',            COUNT(*) FROM inscripcion
UNION ALL SELECT 'pauta',                  COUNT(*) FROM pauta
UNION ALL SELECT 'dimension_pauta',        COUNT(*) FROM dimension_pauta
UNION ALL SELECT 'indicador',              COUNT(*) FROM indicador
UNION ALL SELECT 'planificacion',          COUNT(*) FROM planificacion
UNION ALL SELECT 'revision_planificacion', COUNT(*) FROM revision_planificacion
UNION ALL SELECT 'evaluacion',             COUNT(*) FROM evaluacion
UNION ALL SELECT 'respuesta_indicador',    COUNT(*) FROM respuesta_indicador
UNION ALL SELECT 'nota_voz',               COUNT(*) FROM nota_voz
UNION ALL SELECT 'mensaje_chatbot',        COUNT(*) FROM mensaje_chatbot;

SELECT '3. RESTRICCIONES por tipo (esperado: PRIMARY KEY 14, FOREIGN KEY 20, UNIQUE 13, CHECK 32)' AS verificacion;
SELECT constraint_type AS tipo, COUNT(*) AS cantidad
  FROM information_schema.table_constraints
 WHERE table_schema = DATABASE()
 GROUP BY constraint_type
 ORDER BY constraint_type;

SELECT '4. ÍNDICES por tabla (incluye clave primaria, UNIQUE y los que crea cada FOREIGN KEY)' AS verificacion;
SELECT table_name AS tabla,
       COUNT(DISTINCT index_name) AS indices,
       GROUP_CONCAT(DISTINCT index_name ORDER BY index_name SEPARATOR ', ') AS nombres
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
 GROUP BY table_name
 ORDER BY table_name;

-- -----------------------------------------------------------------------------
SELECT '5. CATÁLOGO: las 6 asignaturas (esperado: el chatbot solo en las dos últimas)' AS verificacion;
SELECT orden, codigo, nombre, ciclo, modalidad, requiere_centro_educativo AS centro, chatbot_habilitado AS chatbot,
       requiere_planificacion AS planifica
  FROM asignatura_practica
 ORDER BY orden;

SELECT '6. CATÁLOGO: la pauta tiene 3 dimensiones y 20 indicadores (esperado: 6, 6 y 8 indicadores)' AS verificacion;
SELECT d.orden, d.nombre AS dimension, COUNT(i.id) AS indicadores,
       MIN(i.numero) AS desde, MAX(i.numero) AS hasta
  FROM dimension_pauta d
  JOIN indicador i ON i.dimension_id = d.id
 GROUP BY d.id, d.orden, d.nombre
 ORDER BY d.orden;

-- -----------------------------------------------------------------------------
SELECT '7. INTEGRIDAD: reglas del MER que la base debe cumplir (esperado: 0 violaciones en cada fila)' AS verificacion;
SELECT 'Guía de una inscripción sin rol PROFESOR_GUIA' AS control, COUNT(*) AS violaciones
  FROM inscripcion i JOIN usuario u ON u.id = i.profesor_guia_id
 WHERE u.rol_id <> (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA')
UNION ALL
SELECT 'Profesor del curso sin rol PROFESOR_CURSO', COUNT(*)
  FROM inscripcion i JOIN usuario u ON u.id = i.profesor_curso_id
 WHERE u.rol_id <> (SELECT id FROM rol WHERE nombre = 'PROFESOR_CURSO')
UNION ALL
SELECT 'Ficha de estudiante cuyo usuario no tiene rol ESTUDIANTE', COUNT(*)
  FROM estudiante e JOIN usuario u ON u.id = e.usuario_id
 WHERE u.rol_id <> (SELECT id FROM rol WHERE nombre = 'ESTUDIANTE')
UNION ALL
SELECT 'Inscripción en asignatura con colegio pero sin guía', COUNT(*)
  FROM inscripcion i JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE a.requiere_centro_educativo AND i.profesor_guia_id IS NULL
UNION ALL
SELECT 'Evaluador sin rol PROFESOR_GUIA en pauta de COLABORADOR', COUNT(*)
  FROM evaluacion e JOIN pauta p ON p.id = e.pauta_id JOIN usuario u ON u.id = e.evaluador_id
 WHERE p.tipo_evaluador = 'COLABORADOR' AND u.rol_id <> (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA')
UNION ALL
SELECT 'Evaluación de un guía que no es el guía de esa inscripción', COUNT(*)
  FROM evaluacion e JOIN inscripcion i ON i.id = e.inscripcion_id
 WHERE e.evaluador_id <> i.profesor_guia_id
UNION ALL
SELECT 'Evaluación FINALIZADA con indicadores sin puntuar o total mal calculado', COUNT(*)
  FROM evaluacion e
  LEFT JOIN (SELECT evaluacion_id, SUM(puntaje) AS suma, COUNT(*) AS n
               FROM respuesta_indicador GROUP BY evaluacion_id) r ON r.evaluacion_id = e.id
 WHERE e.estado = 'FINALIZADA'
   AND (r.n IS NULL
        OR r.n <> (SELECT COUNT(*) FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id
                    WHERE d.pauta_id = e.pauta_id)
        OR r.suma <> e.puntaje_total
        OR ABS(e.promedio - r.suma / r.n) > 0.005)
UNION ALL
SELECT 'Planificación AUTORIZADA sin aprobación final del profesor del curso', COUNT(*)
  FROM planificacion p
 WHERE p.estado = 'AUTORIZADA'
   AND NOT EXISTS (SELECT 1 FROM revision_planificacion r
                    WHERE r.planificacion_id = p.id AND r.etapa = 'CURSO' AND r.decision = 'APROBADA')
UNION ALL
SELECT 'Planificación EN_AUTORIZACION sin aprobación del guía (salvo inscripciones sin guía: van directo al profesor del curso)', COUNT(*)
  FROM planificacion p JOIN inscripcion i ON i.id = p.inscripcion_id
 WHERE p.estado = 'EN_AUTORIZACION'
   AND i.profesor_guia_id IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM revision_planificacion r
                    WHERE r.planificacion_id = p.id AND r.etapa = 'GUIA' AND r.decision = 'APROBADA')
UNION ALL
SELECT 'Planificación en una asignatura que no entrega planificaciones', COUNT(*)
  FROM planificacion p
  JOIN inscripcion i ON i.id = p.inscripcion_id
  JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE NOT a.requiere_planificacion
UNION ALL
SELECT 'Revisión hecha por alguien que no es el guía/profesor del curso de la inscripción', COUNT(*)
  FROM revision_planificacion r
  JOIN planificacion p ON p.id = r.planificacion_id
  JOIN inscripcion i ON i.id = p.inscripcion_id
 WHERE (r.etapa = 'GUIA'  AND r.revisor_id <> i.profesor_guia_id)
    OR (r.etapa = 'CURSO' AND r.revisor_id <> i.profesor_curso_id);

-- -----------------------------------------------------------------------------
SELECT '8. HU-05 HISTORIAL: recorrido de Sebastián Vargas (RUT 44444444-4). Esperado: 6 inscripciones en orden cronológico, las 5 primeras APROBADA y la última EN_CURSO' AS verificacion;
SELECT i.periodo, a.orden, a.nombre AS asignatura, a.ciclo, i.estado, i.nota_final,
       COUNT(DISTINCT ev.id) AS evaluaciones_finalizadas,
       COUNT(DISTINCT pl.id) AS planificaciones
  FROM estudiante s
  JOIN inscripcion i ON i.estudiante_id = s.id
  JOIN asignatura_practica a ON a.id = i.asignatura_id
  LEFT JOIN evaluacion ev ON ev.inscripcion_id = i.id AND ev.estado = 'FINALIZADA'
  LEFT JOIN planificacion pl ON pl.inscripcion_id = i.id
 WHERE s.rut = '44444444-4'
 GROUP BY i.id, i.periodo, a.orden, a.nombre, a.ciclo, i.estado, i.nota_final
 ORDER BY i.periodo, a.orden;

SELECT '9. HU-05 EVOLUCIÓN: evaluaciones de Valentina Pérez (RUT 33333333-3). Esperado: 2 clases y el promedio SUBE de la primera a la segunda' AS verificacion;
SELECT ev.fecha_clase, ev.establecimiento, ev.curso, ev.puntaje_total, ev.promedio,
       IF(ev.planificacion_id IS NULL, 'sin planificación enlazada', 'con planificación') AS planificacion
  FROM estudiante s
  JOIN inscripcion i ON i.estudiante_id = s.id
  JOIN evaluacion ev ON ev.inscripcion_id = i.id AND ev.estado = 'FINALIZADA'
 WHERE s.rut = '33333333-3'
 ORDER BY ev.fecha_clase;

SELECT '10. HU-05 DETALLE: puntaje de cada indicador de la evaluación 3 (esperado: 20 filas, números 1 a 20)' AS verificacion;
SELECT d.orden AS dim, i.numero, LEFT(i.descripcion, 70) AS indicador, r.puntaje
  FROM respuesta_indicador r
  JOIN indicador i ON i.id = r.indicador_id
  JOIN dimension_pauta d ON d.id = i.dimension_id
 WHERE r.evaluacion_id = 3
 ORDER BY i.numero;

SELECT '11. CONTEXTO DEL CHATBOT (S-14): semestre y asignaturas aprobadas de Valentina Pérez. Esperado: semestre 8 y 4 asignaturas aprobadas' AS verificacion;
SELECT s.semestre_actual,
       COUNT(DISTINCT a.id) AS aprobadas,
       GROUP_CONCAT(DISTINCT a.nombre ORDER BY a.orden SEPARATOR ' | ') AS asignaturas_aprobadas
  FROM estudiante s
  JOIN inscripcion i ON i.estudiante_id = s.id AND i.estado = 'APROBADA'
  JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE s.rut = '33333333-3'
 GROUP BY s.id, s.semestre_actual;

-- -----------------------------------------------------------------------------
SELECT '12. HU-04 ESTADOS: planificaciones por estado (esperado: 1 BORRADOR, 2 EN_REVISION_GUIA, 1 CON_OBSERVACIONES, 1 EN_AUTORIZACION, 4 AUTORIZADA)' AS verificacion;
SELECT estado, COUNT(*) AS planificaciones
  FROM planificacion
 GROUP BY estado
 ORDER BY FIELD(estado, 'BORRADOR', 'EN_REVISION_GUIA', 'CON_OBSERVACIONES', 'EN_AUTORIZACION', 'AUTORIZADA');

SELECT '13. HU-04 HISTORIAL: decisiones sobre la planificación 8 en orden cronológico (esperado: guía observa, guía aprueba, curso autoriza)' AS verificacion;
SELECT r.fecha, r.etapa, r.decision, CONCAT(u.nombre, ' ', u.apellidos) AS revisor, r.comentario
  FROM revision_planificacion r
  JOIN usuario u ON u.id = r.revisor_id
 WHERE r.planificacion_id = 8
 ORDER BY r.fecha;

SELECT '14. HU-04 BANDEJA del profesor guía Lorena Vidal: planificaciones por revisar (esperado: 2, de Diego y Sebastián)' AS verificacion;
SELECT p.id, CONCAT(u.nombre, ' ', u.apellidos) AS estudiante, a.nombre AS asignatura, p.titulo, p.fecha_clase
  FROM planificacion p
  JOIN inscripcion i ON i.id = p.inscripcion_id
  JOIN estudiante e ON e.id = i.estudiante_id
  JOIN usuario u ON u.id = e.usuario_id
  JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE i.profesor_guia_id = (SELECT id FROM usuario WHERE email = 'guia2@demo.example')
   AND p.estado = 'EN_REVISION_GUIA'
 ORDER BY p.fecha_clase;

-- -----------------------------------------------------------------------------
SELECT '15. ÍNDICES EN USO: la consulta del historial usa el índice UNIQUE de inscripcion (columna key)' AS verificacion;
EXPLAIN SELECT id FROM inscripcion WHERE estudiante_id = 4 ORDER BY periodo;
