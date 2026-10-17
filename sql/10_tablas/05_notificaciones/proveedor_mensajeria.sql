-- proveedor_mensajeria · módulo 05 — Notificaciones y Comunicaciones
-- clase de dominio: ProveedorMensajeria
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS notificaciones.proveedor_mensajeria (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(80) NOT NULL,
  canales_soportados                 VARCHAR(120) NOT NULL,
  url_base                           VARCHAR(200) NOT NULL,
  referencia_credenciales            VARCHAR(120) NOT NULL,
  costo_por_mensaje                  NUMERIC(10,4) DEFAULT 0 NOT NULL,
  limite_mensajes_por_segundo        SMALLINT NOT NULL,
  prioridad                          SMALLINT NOT NULL,
  activo                             BOOLEAN DEFAULT FALSE NOT NULL,
  salud_porcentaje                   NUMERIC(5,2) NOT NULL,
  CONSTRAINT pk_proveedor_mensajeria PRIMARY KEY (id)
);

COMMENT ON TABLE notificaciones.proveedor_mensajeria IS 'Módulo 05 — Notificaciones y Comunicaciones. Cada proveedor de mensajería con el que se opera, con su costo, su límite de tasa y su salud.';
COMMENT ON COLUMN notificaciones.proveedor_mensajeria.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN notificaciones.proveedor_mensajeria.codigo IS 'UQ';
