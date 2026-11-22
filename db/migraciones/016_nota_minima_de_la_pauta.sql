-- =============================================================================
-- Migracion 016 - La nota minima de la escala es la de cada pauta (HU-09, S-81)
--
-- Motivo (Sprint 2, 2026-11-22):
--   La migracion 015 calculo la nota de cada evaluacion finalizada al 60 % de
--   exigencia partiendo SIEMPRE de 1,0 con 0 puntos. La pauta impresa de
--   Practica Profesional trae su escala (escaladenotas.cl): nota minima 2,0,
--   aprobacion 4,0, exigencia 60 %, maximo 133. La nota minima es la escala
--   minima de la pauta (2 en las de Practica Pedagogica y Profesional, 1 en la
--   de observacion de clase), acotada entre 1 y 3. Sobre el 4,0 nada cambia; bajo
--   el 4,0 la nota sube (ej. 40 de 133 puntos: antes 2,5, ahora 3,0).
--
-- Cambios: solo datos. Recalcula evaluacion.nota de las evaluaciones FINALIZADAS.
-- No hay cambios de estructura (la columna y ck_evaluacion_nota son de la 015).
--
-- Se ejecuta UNA sola vez y como root, despues de la 015:
--   mysql --protocol=TCP -h 127.0.0.1 -u root -p --default-character-set=utf8mb4 < db/migraciones/016_nota_minima_de_la_pauta.sql
-- Para una base nueva NO hace falta: db/seed.sql ya calcula la nota asi.
-- El archivo es solo ASCII a proposito (los clientes mysql de Windows leen mal las tildes).
-- =============================================================================

USE isw_practicas;

UPDATE evaluacion e
  JOIN pauta p ON p.id = e.pauta_id
  JOIN (SELECT d.pauta_id, COUNT(*) AS n
          FROM indicador i JOIN dimension_pauta d ON d.id = i.dimension_id
         GROUP BY d.pauta_id) c ON c.pauta_id = e.pauta_id
   SET e.nota = ROUND(CASE
           WHEN e.puntaje_total < 0.6 * c.n * p.escala_max
               THEN LEAST(GREATEST(p.escala_min, 1), 3)
                    + (4 - LEAST(GREATEST(p.escala_min, 1), 3)) * e.puntaje_total / (0.6 * c.n * p.escala_max)
           ELSE 4 + 3 * (e.puntaje_total - 0.6 * c.n * p.escala_max) / (0.4 * c.n * p.escala_max)
       END, 1)
 WHERE e.estado = 'FINALIZADA';
