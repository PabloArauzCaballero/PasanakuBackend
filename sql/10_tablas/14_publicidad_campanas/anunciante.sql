-- anunciante · módulo 14 — Publicidad y Campañas
-- clase de dominio: Anunciante
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS publicidad.anunciante (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tipo                               VARCHAR(15) NOT NULL,
  organizador_id                     UUID,
  socio_comercial_id                 UUID,
  razon_social_facturacion           VARCHAR(150) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  creado_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_anunciante PRIMARY KEY (id),
  CONSTRAINT ck_anunciante_tipo CHECK (tipo IN ('ORGANIZADOR', 'SOCIO_COMERCIAL')),
  CONSTRAINT ck_anunciante_estado CHECK (estado IN ('ACTIVO', 'DADO_DE_BAJA', 'SUSPENDIDO'))
);

COMMENT ON TABLE publicidad.anunciante IS 'Módulo 14 — Publicidad y Campañas. la entidad que efectivamente contrata: tipo decide si el anunciante es un organizador existente (M07) o un socio_comercial nuevo, con un CHECK que exige exactamente una de las dos referencias según el tipo. Todo lo demás del módulo (cuenta publicitaria, campaña, factura) cuelga de anunciante, no de organizador ni de socio_comercial directamente — así el resto del modelo no necesita saber si detrás hay un organizador o un tercero.';
COMMENT ON COLUMN publicidad.anunciante.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN publicidad.anunciante.tipo IS 'CK, IDX';
COMMENT ON COLUMN publicidad.anunciante.organizador_id IS 'FK, NULL, IDX, M7';
COMMENT ON COLUMN publicidad.anunciante.socio_comercial_id IS 'FK, NULL, IDX';
COMMENT ON COLUMN publicidad.anunciante.estado IS 'CK, IDX';
