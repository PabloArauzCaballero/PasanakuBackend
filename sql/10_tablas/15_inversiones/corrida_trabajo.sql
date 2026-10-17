-- corrida_trabajo · módulo 15 — Inversiones voluntarias
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.corrida_trabajo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  operacion                          VARCHAR(30) NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  hash_solicitud                     CHAR(64) NOT NULL,
  origen                             VARCHAR(10) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  resultado                          VARCHAR(400),
  error                              VARCHAR(120),
  iniciada_en                        TIMESTAMPTZ NOT NULL,
  terminada_en                       TIMESTAMPTZ,
  CONSTRAINT pk_corrida_trabajo PRIMARY KEY (id),
  CONSTRAINT ck_corrida_trabajo_operacion CHECK (operacion IN ('SINCRONIZAR_CATALOGO', 'SINCRONIZAR_VALORES', 'DEVENGAR', 'REINTENTAR_INSTRUCCIONES')),
  CONSTRAINT ck_corrida_trabajo_origen CHECK (origen IN ('API', 'PROGRAMADO')),
  CONSTRAINT ck_corrida_trabajo_estado CHECK (estado IN ('EN_CURSO', 'OK', 'PARCIAL', 'FALLIDA'))
);

COMMENT ON TABLE inversiones.corrida_trabajo IS 'Módulo 15 — Inversiones voluntarias. Cada corrida de una operación de inversiones: sincronizar el catálogo o los valores, devengar o reintentar instrucciones, disparada por API o por el programador.';
COMMENT ON COLUMN inversiones.corrida_trabajo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN inversiones.corrida_trabajo.operacion IS 'CK: SINCRONIZAR_CATALOGO | SINCRONIZAR_VALORES | DEVENGAR | REINTENTAR_INSTRUCCIONES, IDX';
COMMENT ON COLUMN inversiones.corrida_trabajo.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN inversiones.corrida_trabajo.origen IS 'CK: API | PROGRAMADO';
COMMENT ON COLUMN inversiones.corrida_trabajo.estado IS 'CK: EN_CURSO | OK | PARCIAL | FALLIDA, IDX';
COMMENT ON COLUMN inversiones.corrida_trabajo.resultado IS 'NULL';
COMMENT ON COLUMN inversiones.corrida_trabajo.error IS 'NULL';
COMMENT ON COLUMN inversiones.corrida_trabajo.terminada_en IS 'NULL';
