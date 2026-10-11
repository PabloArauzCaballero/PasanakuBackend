-- ENSAYO EN SECO (grupos): el SQL de AplicarCesionDeDerecho contra el esquema REAL del stack y el ROL REAL svc_grupos. ROLLBACK al final.
BEGIN;
SET LOCAL ROLE svc_grupos;
SELECT set_config('app.rol', 'sistema', true), set_config('app.usuario_id', '00000000-0000-0000-0000-0000000000f3', true) \gset
\set cupo 'd86d1d1f-b83b-4da9-9396-e8b1dfe27864'
\set grupo '5e0e4a93-f201-46b8-bccb-79f05486b8ee'
\set vendedor '44f161f5-561a-49a5-97fb-b55b4c15f86a'
\set comprador '5381c2fd-0ba1-4030-89dc-7497523eba24'
SELECT gen_random_uuid() AS evento \gset
SELECT 'antes: titular del cupo es el vendedor', participante_id = :'vendedor'::uuid FROM grupos.cupo WHERE id = :'cupo';
-- bloqueo del cupo (bloquearCupo) y la marca de consumo en la misma transaccion
SELECT 'cupo bloqueado', estado, participante_id = :'vendedor'::uuid FROM grupos.cupo WHERE id = :'cupo' FOR UPDATE;
INSERT INTO grupos.evento_consumido (id_evento, consumidor, consumido_en) VALUES (:'evento', 'grupos.cesion-de-derecho', now()) ON CONFLICT DO NOTHING;
INSERT INTO grupos.traspaso_cupo (id, cupo_id, participante_origen_id, participante_destino_id, motivo, deuda_transferida, derecho_cobro_transferido, aprobado_por_acuerdo_id, fecha)
VALUES (gen_random_uuid(), :'cupo', :'vendedor', :'comprador', 'VENTA', 0, true, NULL, now());
UPDATE grupos.cupo SET participante_id = :'comprador' WHERE id = :'cupo';
SELECT 'despues: titular del cupo es el comprador', participante_id = :'comprador'::uuid FROM grupos.cupo WHERE id = :'cupo';
SELECT 'despues: UN traspaso VENTA', count(*), min(motivo), min(deuda_transferida) FROM grupos.traspaso_cupo WHERE cupo_id = :'cupo' AND fecha >= now() - interval '1 minute';
SELECT 'estados de los participantes intactos', (SELECT estado FROM grupos.participante WHERE id = :'vendedor'), (SELECT estado FROM grupos.participante WHERE id = :'comprador');
-- la marca repetida no inserta (idempotencia por evento)
INSERT INTO grupos.evento_consumido (id_evento, consumidor, consumido_en) VALUES (:'evento', 'grupos.cesion-de-derecho', now()) ON CONFLICT DO NOTHING;
SELECT 'marcas del evento tras el segundo intento (debe ser 1)', count(*) FROM grupos.evento_consumido WHERE id_evento = :'evento';
ROLLBACK;
