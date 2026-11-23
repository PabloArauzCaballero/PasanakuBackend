-- F2.L2.M04 · consulta de uso en TEST de las tablas del aval, para la decision de Pablo (contraccion destructiva).
-- El codigo del aval ya no existe (CU26EjecutarAval retirado; ejecutarAval responde 410 AP-CU26-08). Nada las escribe.
-- Referencias (pg_constraint en un esquema generado por sql/aplicar.sql):
--   garantia.ejecucion_aval.fk_ejecucion_aval_aval_id -> garantia.aval_participante   (unica FK hacia el aval)
-- Columnas de otros servicios ligadas al aval, a contraer con sus duenos:
--   identidad.referencia_personal.acepta_ser_avalista (L4) · grupos.configuracion_grupo.requiere_avalista (grupos)
SELECT 'aval_participante por estado' AS que, estado, count(*) AS filas, COALESCE(sum(monto_maximo_avalado), 0) AS monto
  FROM garantia.aval_participante GROUP BY estado
UNION ALL
SELECT 'ejecucion_aval por estado', estado, count(*), COALESCE(sum(monto_ejecutado), 0)
  FROM garantia.ejecucion_aval GROUP BY estado
UNION ALL
SELECT 'grupos que exigen avalista', NULL, count(*), NULL
  FROM grupos.configuracion_grupo WHERE requiere_avalista
UNION ALL
SELECT 'referencias que aceptan ser avalistas', NULL, count(*), NULL
  FROM identidad.referencia_personal WHERE acepta_ser_avalista
ORDER BY 1, 2;
-- FK que apuntan a las tablas (por si alguien agrego una):
SELECT conrelid::regclass AS tabla, conname, confrelid::regclass AS apunta_a
  FROM pg_constraint
 WHERE contype = 'f' AND confrelid IN ('garantia.aval_participante'::regclass, 'garantia.ejecucion_aval'::regclass);
