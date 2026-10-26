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
SELECT '1. ESTRUCTURA: cantidad de tablas (esperado: 17)' AS verificacion;
SELECT COUNT(*) AS tablas
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';

SELECT '2. FILAS POR TABLA (esperado: rol 6, usuario 33, estudiante 20, asignatura 6, inscripcion 55, pauta 3, dimension 11, indicador 58, planificacion 19, revision 23, evaluacion 11, respuesta 192, nota_voz 0, mensaje 0, documento_planificacion 0, documento_evaluacion 0, instrucciones_chatbot 1)' AS verificacion;
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
UNION ALL SELECT 'mensaje_chatbot',        COUNT(*) FROM mensaje_chatbot
UNION ALL SELECT 'documento_planificacion', COUNT(*) FROM documento_planificacion
UNION ALL SELECT 'documento_evaluacion',    COUNT(*) FROM documento_evaluacion
UNION ALL SELECT 'instrucciones_chatbot',   COUNT(*) FROM instrucciones_chatbot;

SELECT '3. RESTRICCIONES por tipo (esperado: PRIMARY KEY 17, FOREIGN KEY 29, UNIQUE 14, CHECK 42)' AS verificacion;
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
SELECT '5. CATÁLOGO: las 6 asignaturas (esperado: el chatbot solo en las dos últimas y el tutor solo en Práctica Profesional)' AS verificacion;
SELECT orden, codigo, nombre, ciclo, modalidad, requiere_centro_educativo AS centro, chatbot_habilitado AS chatbot,
       requiere_planificacion AS planifica, lleva_tutor AS tutor
  FROM asignatura_practica
 ORDER BY orden;

SELECT '6. CATÁLOGO: la pauta de observación de clase (id 1) tiene 3 dimensiones y 20 indicadores (esperado: 6, 6 y 8 indicadores)' AS verificacion;
SELECT d.orden, d.nombre AS dimension, COUNT(i.id) AS indicadores,
       MIN(i.numero) AS desde, MAX(i.numero) AS hasta
  FROM dimension_pauta d
  JOIN indicador i ON i.dimension_id = d.id
 WHERE d.pauta_id = 1
 GROUP BY d.id, d.orden, d.nombre
 ORDER BY d.orden;

SELECT '6b. CATÁLOGO HU-07: pautas de Práctica Profesional (esperado: 2 pautas, colaborador y tutor, Especialidad 1.ª observación, escala 2 a 7, 4 dimensiones, 19 indicadores, puntaje máximo 133)' AS verificacion;
SELECT p.id, p.tipo_evaluador, p.ambito, p.numero_observacion AS observacion, p.escala_min, p.escala_max,
       COUNT(DISTINCT d.id) AS dimensiones, COUNT(i.id) AS indicadores, COUNT(i.id) * p.escala_max AS puntaje_maximo
  FROM pauta p
  JOIN dimension_pauta d ON d.pauta_id = p.id
  JOIN indicador i ON i.dimension_id = d.id
 WHERE p.asignatura_id = 6
 GROUP BY p.id, p.tipo_evaluador, p.ambito, p.numero_observacion, p.escala_min, p.escala_max
 ORDER BY p.id;

-- -----------------------------------------------------------------------------
SELECT '7. INTEGRIDAD: reglas del MER que la base debe cumplir (esperado: 0 violaciones en cada fila)' AS verificacion;
-- Desde la migración 004 un usuario puede tener un segundo rol: "tiene el rol" = principal o secundario.
SELECT 'Guía de una inscripción sin rol PROFESOR_GUIA' AS control, COUNT(*) AS violaciones
  FROM inscripcion i JOIN usuario u ON u.id = i.profesor_guia_id
 WHERE (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA') NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
SELECT 'Profesor del curso sin rol PROFESOR_CURSO', COUNT(*)
  FROM inscripcion i JOIN usuario u ON u.id = i.profesor_curso_id
 WHERE (SELECT id FROM rol WHERE nombre = 'PROFESOR_CURSO') NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
SELECT 'Ficha de estudiante cuyo usuario no tiene rol ESTUDIANTE', COUNT(*)
  FROM estudiante e JOIN usuario u ON u.id = e.usuario_id
 WHERE (SELECT id FROM rol WHERE nombre = 'ESTUDIANTE') NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
SELECT 'Inscripción en asignatura con colegio pero sin guía', COUNT(*)
  FROM inscripcion i JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE a.requiere_centro_educativo AND i.profesor_guia_id IS NULL
UNION ALL
SELECT 'Evaluador sin rol PROFESOR_GUIA en pauta de COLABORADOR', COUNT(*)
  FROM evaluacion e JOIN pauta p ON p.id = e.pauta_id JOIN usuario u ON u.id = e.evaluador_id
 WHERE p.tipo_evaluador = 'COLABORADOR'
   AND (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA') NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
-- Tras un cambio de guía (demo: inscripción 40), las evaluaciones FINALIZADAS del guía anterior quedan como
-- registro histórico; un BORRADOR sí sería un problema, porque su autor ya no podría terminarlo.
SELECT 'Evaluación en BORRADOR de un guía que ya no es el guía de esa inscripción', COUNT(*)
  FROM evaluacion e JOIN inscripcion i ON i.id = e.inscripcion_id
 WHERE e.estado = 'BORRADOR' AND e.evaluador_id <> i.profesor_guia_id
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
-- Una revisión es un registro histórico: tras un cambio de guía (demo: inscripción 40) la del guía anterior sigue
-- valiendo. Lo que nunca puede pasar es que la etapa la haya firmado alguien sin el rol que corresponde.
SELECT 'Revisión firmada por alguien sin el rol de su etapa (GUIA: PROFESOR_GUIA; CURSO: PROFESOR_CURSO)', COUNT(*)
  FROM revision_planificacion r
  JOIN usuario u ON u.id = r.revisor_id
 WHERE (SELECT id FROM rol WHERE nombre = IF(r.etapa = 'GUIA', 'PROFESOR_GUIA', 'PROFESOR_CURSO'))
       NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
SELECT 'Revisión de un guía o profesor del curso que ya no lo es, salvo la histórica de la inscripción 40', COUNT(*)
  FROM revision_planificacion r
  JOIN planificacion p ON p.id = r.planificacion_id
  JOIN inscripcion i ON i.id = p.inscripcion_id
 WHERE ((r.etapa = 'GUIA'  AND r.revisor_id <> i.profesor_guia_id)
     OR (r.etapa = 'CURSO' AND r.revisor_id <> i.profesor_curso_id))
   AND i.id <> 40;

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
SELECT '12. HU-04 ESTADOS: planificaciones por estado (esperado: 2 BORRADOR, 2 EN_REVISION_GUIA, 4 CON_OBSERVACIONES, 3 EN_AUTORIZACION, 8 AUTORIZADA)' AS verificacion;
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

-- -----------------------------------------------------------------------------
SELECT '16. GENERACIÓN COMPLETA (S-39): 20 estudiantes y ningún guía con más de 15-20 (esperado: 20 estudiantes, máximo <= 20 por guía)' AS verificacion;
SELECT COUNT(*) AS total_estudiantes FROM estudiante;
SELECT CONCAT(u.nombre, ' ', u.apellidos) AS guia, COUNT(DISTINCT i.estudiante_id) AS estudiantes_asignados
  FROM inscripcion i
  JOIN usuario u ON u.id = i.profesor_guia_id
 GROUP BY u.id, u.nombre, u.apellidos
 ORDER BY estudiantes_asignados DESC;

SELECT '17. DOCUMENTOS ADJUNTOS Y NOTA DE VOZ: tablas nuevas listas para la aplicación (esperado: 0 filas; se cargan por HTTP, no por seed)' AS verificacion;
SELECT 'documento_planificacion' AS tabla, COUNT(*) AS filas FROM documento_planificacion
UNION ALL SELECT 'documento_evaluacion', COUNT(*) FROM documento_evaluacion
UNION ALL SELECT 'nota_voz', COUNT(*) FROM nota_voz;

-- -----------------------------------------------------------------------------
SELECT '18. ACTIVIDAD REAL (Fase F, 2026-09-22): nadie queda con cero registros relacionados (esperado: 0 en cada fila)' AS verificacion;
SELECT 'Estudiante en Trabajo Colaborativo sin ninguna evaluación' AS control, COUNT(*) AS violaciones
  FROM (SELECT DISTINCT i.estudiante_id
          FROM inscripcion i
         WHERE i.asignatura_id = 3
           AND NOT EXISTS (SELECT 1 FROM evaluacion ev WHERE ev.inscripcion_id = i.id)) x
UNION ALL
SELECT 'Estudiante en una asignatura que pide planificación sin ninguna planificación', COUNT(*)
  FROM (SELECT DISTINCT i.estudiante_id
          FROM inscripcion i
          JOIN asignatura_practica a ON a.id = i.asignatura_id
         WHERE a.requiere_planificacion) necesitan
 WHERE NOT EXISTS (SELECT 1 FROM planificacion p
                     JOIN inscripcion i2 ON i2.id = p.inscripcion_id
                    WHERE i2.estudiante_id = necesitan.estudiante_id)
UNION ALL
SELECT 'Profesor guía activo (con estudiantes asignados) sin ninguna evaluación realizada', COUNT(*)
  FROM usuario u
 WHERE u.rol_id = (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA') AND u.activo
   AND EXISTS (SELECT 1 FROM inscripcion i WHERE i.profesor_guia_id = u.id)
   AND NOT EXISTS (SELECT 1 FROM evaluacion e WHERE e.evaluador_id = u.id)
UNION ALL
SELECT 'Profesor guía activo (con estudiantes asignados) sin ninguna revisión de planificación', COUNT(*)
  FROM usuario u
 WHERE u.rol_id = (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA') AND u.activo
   AND EXISTS (SELECT 1 FROM inscripcion i WHERE i.profesor_guia_id = u.id)
   AND NOT EXISTS (SELECT 1 FROM revision_planificacion r WHERE r.revisor_id = u.id AND r.etapa = 'GUIA')
UNION ALL
-- Solo guías de rol principal: el jefe de carrera es guía por su segundo rol y tiene 1 estudiante a propósito
SELECT 'Profesor guía activo con menos de 2 estudiantes distintos asignados', COUNT(*)
  FROM (SELECT i.profesor_guia_id, COUNT(DISTINCT i.estudiante_id) AS n
          FROM inscripcion i JOIN usuario u ON u.id = i.profesor_guia_id
         WHERE u.rol_id = (SELECT id FROM rol WHERE nombre = 'PROFESOR_GUIA')
         GROUP BY i.profesor_guia_id) x
 WHERE x.n < 2
UNION ALL
SELECT 'Profesor guía activo con más de 20 estudiantes distintos asignados', COUNT(*)
  FROM (SELECT i.profesor_guia_id, COUNT(DISTINCT i.estudiante_id) AS n
          FROM inscripcion i WHERE i.profesor_guia_id IS NOT NULL GROUP BY i.profesor_guia_id) x
 WHERE x.n > 20
UNION ALL
SELECT 'Profesor del curso activo sin ninguna inscripción a su cargo', COUNT(*)
  FROM usuario u
 WHERE u.rol_id = (SELECT id FROM rol WHERE nombre = 'PROFESOR_CURSO') AND u.activo
   AND NOT EXISTS (SELECT 1 FROM inscripcion i WHERE i.profesor_curso_id = u.id)
UNION ALL
SELECT 'Profesor del curso activo sin ninguna revisión de planificación', COUNT(*)
  FROM usuario u
 WHERE u.rol_id = (SELECT id FROM rol WHERE nombre = 'PROFESOR_CURSO') AND u.activo
   AND NOT EXISTS (SELECT 1 FROM revision_planificacion r WHERE r.revisor_id = u.id AND r.etapa = 'CURSO');

SELECT '19. ACTIVIDAD POR GUÍA Y POR PROFESOR DEL CURSO (para revisar a ojo, sin resultado único esperado)' AS verificacion;
SELECT CONCAT(u.nombre, ' ', u.apellidos) AS guia,
       COUNT(DISTINCT i.estudiante_id)                                          AS estudiantes,
       (SELECT COUNT(*) FROM evaluacion e WHERE e.evaluador_id = u.id)          AS evaluaciones,
       (SELECT COUNT(*) FROM revision_planificacion r WHERE r.revisor_id = u.id AND r.etapa = 'GUIA') AS revisiones
  FROM usuario u
  JOIN inscripcion i ON i.profesor_guia_id = u.id
 GROUP BY u.id, u.nombre, u.apellidos
 ORDER BY estudiantes DESC;

SELECT CONCAT(u.nombre, ' ', u.apellidos) AS profesor_del_curso,
       COUNT(DISTINCT i.estudiante_id)                                          AS estudiantes,
       (SELECT COUNT(*) FROM revision_planificacion r WHERE r.revisor_id = u.id AND r.etapa = 'CURSO') AS revisiones
  FROM usuario u
  JOIN inscripcion i ON i.profesor_curso_id = u.id
 GROUP BY u.id, u.nombre, u.apellidos
 ORDER BY estudiantes DESC;

-- -----------------------------------------------------------------------------
SELECT '20. CARTOLA DE PRÁCTICAS (HU-31, migración 003; esperado: 0 violaciones en cada fila)' AS verificacion;
SELECT 'Inscripción cerrada (APROBADA o REPROBADA) sin fecha de cierre' AS control, COUNT(*) AS violaciones
  FROM inscripcion WHERE estado <> 'EN_CURSO' AND fecha_cierre IS NULL
UNION ALL
SELECT 'Inscripción EN_CURSO con fecha de cierre', COUNT(*)
  FROM inscripcion WHERE estado = 'EN_CURSO' AND fecha_cierre IS NOT NULL
UNION ALL
SELECT 'Usuario con segundo rol igual a su rol principal', COUNT(*)
  FROM usuario WHERE rol_secundario_id = rol_id;

SELECT '21. CUENTAS CON SEGUNDO ROL (migración 004; esperado: 1, el jefe de carrera con PROFESOR_GUIA, guía de 1 inscripción)' AS verificacion;
SELECT u.email, r1.nombre AS rol_principal, r2.nombre AS rol_secundario,
       (SELECT COUNT(*) FROM inscripcion i WHERE i.profesor_guia_id = u.id) AS inscripciones_como_guia
  FROM usuario u
  JOIN rol r1 ON r1.id = u.rol_id
  JOIN rol r2 ON r2.id = u.rol_secundario_id;

SELECT 'Inscripciones cerradas por año de cierre (para revisar a ojo)' AS verificacion;
SELECT YEAR(fecha_cierre) AS anio, COUNT(*) AS cerradas
  FROM inscripcion WHERE fecha_cierre IS NOT NULL
 GROUP BY YEAR(fecha_cierre) ORDER BY anio;

-- -----------------------------------------------------------------------------
SELECT '22. TUTOR DE PRÁCTICA PROFESIONAL (HU-07, migración 006; esperado: 0 violaciones en cada fila)' AS verificacion;
SELECT 'Tutor de una inscripción sin rol TUTOR' AS control, COUNT(*) AS violaciones
  FROM inscripcion i JOIN usuario u ON u.id = i.tutor_id
 WHERE (SELECT id FROM rol WHERE nombre = 'TUTOR') NOT IN (u.rol_id, COALESCE(u.rol_secundario_id, 0))
UNION ALL
SELECT 'Inscripción con tutor en una asignatura que no lleva tutor', COUNT(*)
  FROM inscripcion i JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE i.tutor_id IS NOT NULL AND NOT a.lleva_tutor
UNION ALL
SELECT 'Evaluación con pauta de TUTOR hecha por alguien que no es el tutor de la inscripción', COUNT(*)
  FROM evaluacion e JOIN pauta p ON p.id = e.pauta_id JOIN inscripcion i ON i.id = e.inscripcion_id
 WHERE p.tipo_evaluador = 'TUTOR' AND NOT (e.evaluador_id <=> i.tutor_id);

SELECT '23. TUTORES y sus inscripciones (esperado en la demo: tutor1@demo.example con la inscripción 17, Sebastián Vargas)' AS verificacion;
SELECT u.email, i.id AS inscripcion, CONCAT(ue.nombre, ' ', ue.apellidos) AS estudiante, a.nombre AS asignatura, i.periodo
  FROM inscripcion i
  JOIN usuario u ON u.id = i.tutor_id
  JOIN estudiante es ON es.id = i.estudiante_id
  JOIN usuario ue ON ue.id = es.usuario_id
  JOIN asignatura_practica a ON a.id = i.asignatura_id;

-- -----------------------------------------------------------------------------
SELECT '24. CHATBOT (HU-15 y HU-16, migración 007; esperado: 0 violaciones en cada fila)' AS verificacion;
SELECT 'Mensaje del chatbot en una asignatura sin chatbot habilitado' AS control, COUNT(*) AS violaciones
  FROM mensaje_chatbot m JOIN inscripcion i ON i.id = m.inscripcion_id
  JOIN asignatura_practica a ON a.id = i.asignatura_id
 WHERE NOT a.chatbot_habilitado
UNION ALL
SELECT 'Mensaje con una planificación de otra inscripción', COUNT(*)
  FROM mensaje_chatbot m JOIN planificacion p ON p.id = m.planificacion_id
 WHERE p.inscripcion_id <> m.inscripcion_id
UNION ALL
SELECT 'Respuesta del chatbot con planificación adjunta (solo el estudiante adjunta)', COUNT(*)
  FROM mensaje_chatbot WHERE autor = 'CHATBOT' AND planificacion_id IS NOT NULL;

-- -----------------------------------------------------------------------------
SELECT '25. INSTRUCCIONES DEL CHATBOT (HU-17, migración 008; esperado: 0 violaciones en cada fila)' AS verificacion;
SELECT 'Instrucciones editadas por alguien que no es coordinador' AS control, COUNT(*) AS violaciones
  FROM instrucciones_chatbot c JOIN usuario u ON u.id = c.editado_por_id
  JOIN rol r ON r.id = u.rol_id
  LEFT JOIN rol r2 ON r2.id = u.rol_secundario_id
 WHERE r.nombre <> 'COORDINADOR' AND NOT (r2.nombre <=> 'COORDINADOR')
UNION ALL
SELECT 'Instrucciones fuera del largo permitido (200 a 12 000 caracteres, S-67)', COUNT(*)
  FROM instrucciones_chatbot WHERE CHAR_LENGTH(TRIM(texto)) NOT BETWEEN 200 AND 12000;
