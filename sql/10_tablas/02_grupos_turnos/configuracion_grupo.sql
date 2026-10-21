-- configuracion_grupo · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: ConfiguracionGrupo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.configuracion_grupo (
  grupo_id                           UUID DEFAULT gen_random_uuid() NOT NULL,
  permite_cupos_multiples            BOOLEAN DEFAULT FALSE NOT NULL,
  max_cupos_por_persona              SMALLINT NOT NULL,
  permite_permuta_turnos             BOOLEAN DEFAULT FALSE NOT NULL,
  requiere_avalista                  BOOLEAN DEFAULT FALSE NOT NULL,
  permite_ingreso_tardio             BOOLEAN DEFAULT FALSE NOT NULL,
  hora_limite_pago                   TIME NOT NULL,
  tolerancia_monto_parcial           NUMERIC(5,2) NOT NULL,
  politica_mora_id                   UUID,
  politica_sancion_id                UUID,
  CONSTRAINT pk_configuracion_grupo PRIMARY KEY (grupo_id)
);

COMMENT ON TABLE grupos.configuracion_grupo IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. Los parámetros operativos finos del grupo, separados de su definición económica.';
COMMENT ON COLUMN grupos.configuracion_grupo.grupo_id IS 'PK, FK';
COMMENT ON COLUMN grupos.configuracion_grupo.permite_cupos_multiples IS '¿se permite que alguien tenga dos manos? Es práctica habitual y a la vez es riesgo concentrado — si esa persona falla, el grupo pierde el doble.';
COMMENT ON COLUMN grupos.configuracion_grupo.max_cupos_por_persona IS '¿se permite que alguien tenga dos manos? Es práctica habitual y a la vez es riesgo concentrado — si esa persona falla, el grupo pierde el doble.';
COMMENT ON COLUMN grupos.configuracion_grupo.permite_permuta_turnos IS '¿pueden dos participantes intercambiar posiciones? Permitirlo da flexibilidad real (a alguien se le enfermó un familiar y necesita cobrar antes); prohibirlo evita el mercado gris de compra-venta de turnos.';
COMMENT ON COLUMN grupos.configuracion_grupo.requiere_avalista IS 'exige que cada participante tenga quién responda por él (M8). Sube la fricción de entrada y baja drásticamente la morosidad.';
COMMENT ON COLUMN grupos.configuracion_grupo.permite_ingreso_tardio IS '¿puede entrar alguien con el grupo ya empezado? Tiene implicaciones económicas serias (debe ponerse al día con los períodos ya transcurridos).';
COMMENT ON COLUMN grupos.configuracion_grupo.hora_limite_pago IS '¿un pago a las 23:50 del día de vencimiento cuenta como puntual? ¿Bs 499 sobre Bs 500 cuenta como pagado? Son las dos preguntas que generan más discusiones reales, y por eso se responden por configuración y no por criterio de quien mire.';
COMMENT ON COLUMN grupos.configuracion_grupo.tolerancia_monto_parcial IS '¿un pago a las 23:50 del día de vencimiento cuenta como puntual? ¿Bs 499 sobre Bs 500 cuenta como pagado? Son las dos preguntas que generan más discusiones reales, y por eso se responden por configuración y no por criterio de quien mire.';
COMMENT ON COLUMN grupos.configuracion_grupo.politica_mora_id IS 'FK, NULL';
COMMENT ON COLUMN grupos.configuracion_grupo.politica_sancion_id IS 'FK, NULL';
