-- DESTRABE SINTÉTICO DE DESARROLLO — solo la base local desechable. NO se commitea como seeder.
-- Motivo: seeders/README.md:68-76 prohíbe encender la licencia con un seeder, y el UPDATE documentado
-- (08-gobierno-y-licencia.json:5) apunta a tipo='LICENCIA_FUNCIONAMIENTO', que no existe en la semilla.
-- Se usa la segunda vía de fn_lic_servicio_habilitado(): un entorno de prueba regulado ACTIVO.
INSERT INTO cumplimiento.entorno_prueba_regulado
  (id, licencia_regulatoria_id, servicio_en_prueba, alcance, limite_usuarios, limite_monto_operacion, fecha_inicio, fecha_fin, estado, informes_remitidos)
SELECT gen_random_uuid(), l.id, s, '["DESARROLLO_LOCAL_SINTETICO"]'::jsonb, 100, 10000,
       current_date - 1, current_date + 30, 'ACTIVO', 0
FROM catalogo.licencia_regulatoria l,
     unnest(ARRAY['BILLETERA','RECARGA','RETIRO','TRANSFERENCIA_P2P','GRUPO_PASANAKU']) AS s
WHERE l.tipo = 'CERTIFICADO_ADECUACION';
