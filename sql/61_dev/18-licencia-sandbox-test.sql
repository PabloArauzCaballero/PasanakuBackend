-- Entorno de prueba SINTETICO (sandbox) que habilita solo GRUPO_PASANAKU en DEV y TEST, para poder crear grupos de prueba. NO es una licencia: la licencia sigue EN_TRAMITE.
-- GENERADO desde seeders/dev/18-licencia-sandbox-test.json — no editar a mano.

-- SINTETICO · apto_produccion=false · «sandbox de TEST: no es una licencia ASFI». Cuelga de la licencia sembrada EN_TRAMITE (08-gobierno-y-licencia.json), que no se modifica. Ventana: desde ayer hasta 90 dias; pasada la ventana el servicio vuelve a denegarse solo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO entorno_prueba_regulado (id, licencia_regulatoria_id, servicio_en_prueba, alcance, limite_usuarios, limite_monto_operacion, garantia_constituida, fecha_inicio, fecha_fin, estado, informes_remitidos) VALUES
      ('e2e00000-0000-4000-8000-0000000000d1', (SELECT id FROM licencia_regulatoria WHERE organismo = 'ASFI' AND tipo = 'CERTIFICADO_ADECUACION'), 'GRUPO_PASANAKU', '["SANDBOX-TEST", "sandbox de TEST: no es una licencia ASFI", "sintetico: apto_produccion=false"]'::jsonb, 50, 50000.0, NULL, current_date - 1, (current_date + interval '+90 days'), 'ACTIVO', 0)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;
