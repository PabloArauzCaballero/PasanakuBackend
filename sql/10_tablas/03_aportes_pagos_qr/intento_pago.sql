-- intento_pago · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: IntentoPago
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.intento_pago (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  orden_cobro_id                     UUID NOT NULL,
  numero_intento                     SMALLINT NOT NULL,
  canal                              VARCHAR(30) NOT NULL,
  iniciado_en                        TIMESTAMPTZ NOT NULL,
  finalizado_en                      TIMESTAMPTZ,
  estado                             VARCHAR(15) NOT NULL,
  codigo_error                       VARCHAR(40),
  mensaje_proveedor                  VARCHAR(255),
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  CONSTRAINT pk_intento_pago PRIMARY KEY (id),
  CONSTRAINT ck_intento_pago_canal CHECK (canal IN ('BILLETERA_MOVIL', 'COMPENSACION_INTERNA', 'EFECTIVO_AL_ORGANIZADOR', 'FONDO_GARANTIA', 'QR_INTEROPERABLE', 'TARJETA', 'TRANSFERENCIA_BANCARIA')),
  CONSTRAINT ck_intento_pago_estado CHECK (estado IN ('APROBADA', 'DESCONOCIDA', 'EXPIRADA', 'PENDIENTE', 'RECHAZADA'))
);

COMMENT ON TABLE aportes.intento_pago IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. Cada vez que se trata de ejecutar el cobro de una orden, con su resultado.';
COMMENT ON COLUMN aportes.intento_pago.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.intento_pago.orden_cobro_id IS 'FK, IDX';
COMMENT ON COLUMN aportes.intento_pago.canal IS 'CK';
COMMENT ON COLUMN aportes.intento_pago.finalizado_en IS 'NULL';
COMMENT ON COLUMN aportes.intento_pago.estado IS 'CK';
COMMENT ON COLUMN aportes.intento_pago.codigo_error IS 'NULL';
COMMENT ON COLUMN aportes.intento_pago.mensaje_proveedor IS 'NULL';
COMMENT ON COLUMN aportes.intento_pago.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
