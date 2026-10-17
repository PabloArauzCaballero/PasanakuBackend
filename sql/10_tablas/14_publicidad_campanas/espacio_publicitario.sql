-- espacio_publicitario · módulo 14 — Publicidad y Campañas
-- clase de dominio: EspacioPublicitario
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS publicidad.espacio_publicitario (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(80) NOT NULL,
  tipo                               VARCHAR(25) NOT NULL,
  capacidad_maxima_simultanea        SMALLINT NOT NULL,
  activo                             BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_espacio_publicitario PRIMARY KEY (id),
  CONSTRAINT ck_espacio_publicitario_tipo CHECK (tipo IN ('BANNER_BILLETERA', 'BANNER_INICIO', 'LISTADO_GRUPOS_DESTACADO', 'PUSH_PATROCINADO'))
);

COMMENT ON TABLE publicidad.espacio_publicitario IS 'Módulo 14 — Publicidad y Campañas. El catálogo de inventario propio de la app: dónde puede aparecer un anuncio.';
COMMENT ON COLUMN publicidad.espacio_publicitario.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN publicidad.espacio_publicitario.codigo IS 'UQ';
COMMENT ON COLUMN publicidad.espacio_publicitario.tipo IS 'CK';
