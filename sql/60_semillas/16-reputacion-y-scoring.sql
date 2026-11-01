-- Modelo de reputación versionado: pesos por factor, impacto de cada evento e insignias con criterio publicado. El puntaje no se escribe a mano en ningún lado: se recalcula a partir de estas filas y de eventos que ya ocurrieron.
-- GENERADO desde seeders/minimos/16-reputacion-y-scoring.json — no editar a mano.

INSERT INTO modelo_scoring (version, descripcion, puntaje_base, puntaje_minimo, puntaje_maximo, factor_decaimiento_mensual, ventana_historica_meses, min_eventos_para_score, vigente_desde, vigente_hasta, es_produccion) VALUES
  ('v1', 'Modelo inicial de reputación de participante — escala 0 a 1000, base 500', 500.0, 0.0, 1000.0, 0.98, 36, 3, '2026-01-01T00:00:00-04:00', NULL, TRUE)
ON CONFLICT (version) DO NOTHING;

-- Los pesos suman 1,0000. El tope por factor impide que un solo comportamiento domine el puntaje entero.
INSERT INTO peso_factor (modelo_id, codigo_factor, descripcion, peso, tope_aporte_al_score, es_penalizador) VALUES
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'PUNTUALIDAD_DE_APORTE', 'Proporción de aportes pagados antes del vencimiento', 0.3, 250.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'CICLOS_COMPLETADOS', 'Ciclos de pasanaku terminados sin incumplir', 0.15, 120.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'ANTIGUEDAD_EN_PLATAFORMA', 'Meses de permanencia con la cuenta activa y verificada', 0.1, 80.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'COMPORTAMIENTO_COMO_ORGANIZADOR', 'Desempeño en los grupos administrados, si administró alguno', 0.05, 50.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'MORA_ACUMULADA', 'Días de mora acumulados en la ventana histórica', 0.2, 200.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'INCUMPLIMIENTOS_DECLARADOS', 'Incumplimientos con resolución firme, después del descargo', 0.2, 250.0, TRUE)
ON CONFLICT (modelo_id, codigo_factor) DO NOTHING;

-- Todo lo que mueve el puntaje pasa por acá. `requiere_confirmacion` marca los eventos que no se aplican solos: primero hay resolución firme, después impacto.
INSERT INTO regla_impacto_evento (modelo_id, tipo_evento, codigo_factor, impacto_base, multiplicador_por_reincidencia, impacto_maximo, requiere_confirmacion) VALUES
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'APORTE_PUNTUAL', 'PUNTUALIDAD_DE_APORTE', 5.0, 1.0, 250.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'APORTE_ANTICIPADO', 'PUNTUALIDAD_DE_APORTE', 7.0, 1.0, 250.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'APORTE_TARDIO', 'MORA_ACUMULADA', -10.0, 1.25, 200.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'MORA_MAYOR_15_DIAS', 'MORA_ACUMULADA', -25.0, 1.5, 200.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'COBERTURA_APLICADA', 'INCUMPLIMIENTOS_DECLARADOS', -60.0, 1.5, 250.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'INCUMPLIMIENTO_FIRME', 'INCUMPLIMIENTOS_DECLARADOS', -90.0, 1.5, 250.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'SANCION_FIRME', 'INCUMPLIMIENTOS_DECLARADOS', -80.0, 1.5, 250.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'EXPULSION_DEL_GRUPO', 'INCUMPLIMIENTOS_DECLARADOS', -200.0, 1.0, 250.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'DEUDA_CASTIGADA', 'INCUMPLIMIENTOS_DECLARADOS', -150.0, 1.0, 250.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'PROMESA_INCUMPLIDA', 'MORA_ACUMULADA', -15.0, 1.25, 200.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'DEUDA_REGULARIZADA', 'INCUMPLIMIENTOS_DECLARADOS', 30.0, 1.0, 250.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'PLAN_REGULARIZACION_CUMPLIDO', 'INCUMPLIMIENTOS_DECLARADOS', 25.0, 1.0, 250.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'CICLO_COMPLETADO', 'CICLOS_COMPLETADOS', 40.0, 1.0, 120.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'MES_DE_ANTIGUEDAD', 'ANTIGUEDAD_EN_PLATAFORMA', 2.0, 1.0, 80.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'GRUPO_ADMINISTRADO_SIN_INCIDENCIAS', 'COMPORTAMIENTO_COMO_ORGANIZADOR', 15.0, 1.0, 50.0, FALSE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'SANCION_ORGANIZADOR_FIRME', 'COMPORTAMIENTO_COMO_ORGANIZADOR', -50.0, 1.5, 50.0, TRUE),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'RETIRO_ACORDADO', 'INCUMPLIMIENTOS_DECLARADOS', 0.0, 1.0, 0.0, FALSE)
ON CONFLICT (modelo_id, tipo_evento) DO NOTHING;

-- El criterio se publica junto con la insignia: si no se puede explicar cómo se gana, no se otorga.
INSERT INTO insignia_logro (codigo, nombre, descripcion, criterio, icono_url) VALUES
  ('PRIMER_PASANAKU', 'Primer pasanaku', 'Completaste tu primer ciclo de principio a fin', 'Un ciclo con todos los aportes acreditados y la entrega cobrada, sin cobertura del fondo de garantía en el medio.', 'https://cdn.pasanaku.bo/insignias/primer-pasanaku.svg'),
  ('PAGADOR_PUNTUAL', 'Pagador puntual', 'Doce aportes seguidos antes del vencimiento', '12 obligaciones consecutivas acreditadas en fecha o antes, sin ningún día de mora entre medio.', 'https://cdn.pasanaku.bo/insignias/pagador-puntual.svg'),
  ('SIN_MORA_12_MESES', 'Doce meses sin mora', 'Un año calendario sin un solo día de atraso', '365 días corridos sin ninguna obligación en estado vencido y sin recargo por mora aplicado.', 'https://cdn.pasanaku.bo/insignias/sin-mora.svg'),
  ('TRES_CICLOS', 'Tres ciclos completos', 'Tres pasanakus terminados', '3 ciclos con estado COMPLETADO en los que participaste desde el inicio hasta el cierre.', 'https://cdn.pasanaku.bo/insignias/tres-ciclos.svg'),
  ('REGULARIZADOR', 'Palabra cumplida', 'Regularizaste una deuda dentro del plazo comprometido', 'Un plan de regularización cumplido en su totalidad dentro de las fechas acordadas, o una deuda cancelada antes del vencimiento de la promesa de pago.', 'https://cdn.pasanaku.bo/insignias/regularizador.svg'),
  ('ORGANIZADOR_CONFIABLE', 'Organizador confiable', 'Administraste grupos sin una sola incidencia', '2 grupos cerrados como organizador, sin sanción firme, sin entrega fuera de plazo y con evaluación de desempeño igual o mayor a 80 puntos.', 'https://cdn.pasanaku.bo/insignias/organizador-confiable.svg'),
  ('REFERENTE', 'Referente', 'Invitaste a personas que después cumplieron', '5 invitaciones aceptadas cuyos titulares completaron al menos un ciclo sin incumplimientos declarados.', 'https://cdn.pasanaku.bo/insignias/referente.svg'),
  ('VETERANO', 'Veterano', 'Dos años en la plataforma con la cuenta al día', '24 meses corridos con la cuenta activa, la verificación de identidad vigente y sin deuda castigada.', 'https://cdn.pasanaku.bo/insignias/veterano.svg')
ON CONFLICT (codigo) DO NOTHING;

-- Etapas del modelo v1 — catálogo INTERNO (no oficial), decidido por Pablo 2026-10-30 al reemplazar el catálogo anterior («que no suene demasiado fantasioso»); códigos y textos del PLAN.md del trabajo del 2026-10-30 en docs/trabajo (§H1.S6, renombre a etapas). Diseño de producto propio, sin fuente externa ni licencia que declarar. Tramos [desde, hasta) sobre la escala 0–1000, los mismos del catálogo anterior; la última incluye su hasta. `nombre` es el texto que muestra la app tal cual. Quien recién llega (base 500) arranca en Etapa 1 · Inicial; En recuperación es el tramo por debajo de la base. Un cambio de nombre, orden o tramo se corrige acá y `actualizar` lo aplica sobre una base ya sembrada; el id de la etapa no cambia.
INSERT INTO etapa (modelo_id, codigo, nombre, orden, desde, hasta) VALUES
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'RECUPERACION', 'En recuperación', 0, '0.00', '500.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'INICIAL', 'Etapa 1 · Inicial', 1, '500.00', '550.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'EN_MARCHA', 'Etapa 2 · En marcha', 2, '550.00', '600.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'CONSTANTE', 'Etapa 3 · Constante', 3, '600.00', '650.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'PUNTUAL', 'Etapa 4 · Puntual', 4, '650.00', '700.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'COMPROMETIDO', 'Etapa 5 · Comprometido', 5, '700.00', '760.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'SOLIDO', 'Etapa 6 · Sólido', 6, '760.00', '820.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'DESTACADO', 'Etapa 7 · Destacado', 7, '820.00', '880.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'EXPERTO', 'Etapa 8 · Experto', 8, '880.00', '940.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'EJEMPLAR', 'Etapa 9 · Ejemplar', 9, '940.00', '980.00'),
  ((SELECT id FROM modelo_scoring WHERE version = 'v1'), 'PILAR', 'Etapa 10 · Pilar', 10, '980.00', '1000.00')
ON CONFLICT (modelo_id, codigo) DO UPDATE SET nombre = EXCLUDED.nombre, orden = EXCLUDED.orden, desde = EXCLUDED.desde, hasta = EXCLUDED.hasta
  WHERE etapa.nombre IS DISTINCT FROM EXCLUDED.nombre OR etapa.orden IS DISTINCT FROM EXCLUDED.orden OR etapa.desde IS DISTINCT FROM EXCLUDED.desde OR etapa.hasta IS DISTINCT FROM EXCLUDED.hasta;

-- Desbloqueos por etapa — valores SINTÉTICOS PROPUESTOS por Claude 2026-10-30, sin fuente ni confirmación de negocio ni de cumplimiento (regla 97.5.4). Se siembran PROPUESTO: se muestran y NO limitan. Pasar a VIGENTE exige la confirmación de Pablo y de cumplimiento, y se hace en este seeder, no en la base: `actualizar` vuelve a escribir cupos, aporte, moneda y estado sobre una base ya sembrada, y solo si cambiaron.
INSERT INTO desbloqueo_etapa (etapa_id, cupos_max, aporte_max, moneda, estado) VALUES
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'RECUPERACION'), 5, '100.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'INICIAL'), 6, '200.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'EN_MARCHA'), 8, '300.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'CONSTANTE'), 10, '500.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'PUNTUAL'), 12, '700.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'COMPROMETIDO'), 15, '1000.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'SOLIDO'), 18, '1500.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'DESTACADO'), 20, '2000.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'EXPERTO'), 24, '3000.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'EJEMPLAR'), 30, '4000.00', 'BOB', 'PROPUESTO'),
  ((SELECT id FROM etapa WHERE modelo_id = (SELECT id FROM modelo_scoring WHERE version = 'v1') AND codigo = 'PILAR'), 40, '5000.00', 'BOB', 'PROPUESTO')
ON CONFLICT (etapa_id) DO UPDATE SET cupos_max = EXCLUDED.cupos_max, aporte_max = EXCLUDED.aporte_max, moneda = EXCLUDED.moneda, estado = EXCLUDED.estado
  WHERE desbloqueo_etapa.cupos_max IS DISTINCT FROM EXCLUDED.cupos_max OR desbloqueo_etapa.aporte_max IS DISTINCT FROM EXCLUDED.aporte_max OR desbloqueo_etapa.moneda IS DISTINCT FROM EXCLUDED.moneda OR desbloqueo_etapa.estado IS DISTINCT FROM EXCLUDED.estado;
