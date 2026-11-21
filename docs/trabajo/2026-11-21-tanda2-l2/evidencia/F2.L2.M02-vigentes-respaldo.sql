-- F2.L2.M02 · precondicion de despliegue en TEST: reservas, coberturas y contingencias VIGENTES del respaldo.
-- Si todo da 0, no hay nada que compensar. Si algo da > 0, cada fila necesita su contra-asiento en el mayor
-- (nunca UPDATE: regla 91.1.3) ANTES de retirar el respaldo; P-14 dice que no hubo dinero real.
SELECT 'reservas VIGENTES' AS que, count(*) AS filas, COALESCE(sum(monto_reservado - monto_aplicado - monto_liberado), 0) AS disponible
  FROM garantia.reserva_respaldo WHERE estado = 'VIGENTE'
UNION ALL
SELECT 'coberturas vivas (APLICADA o RECUPERADA_PARCIAL)', count(*), COALESCE(sum(monto_faltante - monto_recuperado), 0)
  FROM garantia.cobertura_respaldo WHERE estado IN ('APLICADA', 'RECUPERADA_PARCIAL')
UNION ALL
SELECT 'planes de contingencia vivos (PROPUESTO o APROBADO)', count(*), NULL
  FROM garantia.plan_contingencia WHERE estado IN ('PROPUESTO', 'APROBADO');
