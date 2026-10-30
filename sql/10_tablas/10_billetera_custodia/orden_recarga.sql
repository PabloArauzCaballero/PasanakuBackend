-- orden_recarga · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: OrdenRecarga
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.orden_recarga (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cuenta_billetera_id                UUID NOT NULL,
  instrumento_fondeo_id              UUID,
  proveedor_id                       UUID,
  pago_id                            UUID,
  transaccion_id                     UUID,
  monto_bruto                        NUMERIC(16,2) DEFAULT 0 NOT NULL,
  costo_proveedor                    NUMERIC(10,2) DEFAULT 0 NOT NULL,
  monto_acreditado                   NUMERIC(16,2) DEFAULT 0 NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  medio                              VARCHAR(30),
  cotizacion_id                      UUID,
  referencia_externa                 VARCHAR(80),
  clave_idempotencia                 VARCHAR(100) NOT NULL,
  solicitada_en                      TIMESTAMPTZ DEFAULT now() NOT NULL,
  acreditada_en                      TIMESTAMPTZ,
  expira_en                          TIMESTAMPTZ,
  qr_contenido                       VARCHAR(512),
  qr_origen                          VARCHAR(10),
  CONSTRAINT pk_orden_recarga PRIMARY KEY (id),
  CONSTRAINT ck_orden_recarga_monto_bruto CHECK (monto_bruto > 0),
  CONSTRAINT ck_orden_recarga_estado CHECK (estado IN ('ACREDITADA', 'EXPIRADA', 'PENDIENTE', 'RECHAZADA', 'REVERSADA')),
  CONSTRAINT ck_orden_recarga_qr_origen CHECK (qr_origen IN ('BANCO', 'PRUEBA'))
);

COMMENT ON TABLE nucleo_financiero.orden_recarga IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. El *cash-in* y el *cash-out*: los dos puntos donde el dinero cruza la frontera entre el mundo bancario y el saldo de la app.';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.cuenta_billetera_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.instrumento_fondeo_id IS 'FK, NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.proveedor_id IS 'FK, NULL, M3';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.pago_id IS 'FK, NULL, M3';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.transaccion_id IS 'FK, NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.monto_bruto IS 'CK: > 0';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.estado IS 'CK, IDX';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.medio IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.cotizacion_id IS 'NULL, polimorfica';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.referencia_externa IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.acreditada_en IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.expira_en IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.qr_contenido IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.orden_recarga.qr_origen IS 'CK, NULL';
