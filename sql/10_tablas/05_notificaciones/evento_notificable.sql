-- evento_notificable · módulo 05 — Notificaciones y Comunicaciones
-- clase de dominio: EventoNotificable
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS notificaciones.evento_notificable (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tipo                               VARCHAR(40) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  categoria                          VARCHAR(20) NOT NULL,
  es_obligatorio                     BOOLEAN DEFAULT FALSE NOT NULL,
  prioridad                          VARCHAR(10) NOT NULL,
  es_transaccional                   BOOLEAN DEFAULT FALSE NOT NULL,
  permite_agrupacion                 BOOLEAN DEFAULT FALSE NOT NULL,
  ventana_deduplicacion_min          SMALLINT NOT NULL,
  canales_permitidos                 VARCHAR(120) NOT NULL,
  cadena_respaldo                    VARCHAR(120) NOT NULL,
  activo                             BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_evento_notificable PRIMARY KEY (id),
  CONSTRAINT ck_evento_notificable_categoria CHECK (categoria IN ('COBRANZA', 'COMERCIAL', 'REGULATORIA', 'SEGURIDAD', 'SOPORTE', 'TRANSACCIONAL')),
  CONSTRAINT ck_evento_notificable_prioridad CHECK (prioridad IN ('ALTA', 'BAJA', 'CRITICA', 'NORMAL'))
);

COMMENT ON TABLE notificaciones.evento_notificable IS 'Módulo 05 — Notificaciones y Comunicaciones. El catálogo de los 20 hechos del negocio que ameritan avisarle a alguien, cada uno con su política de comunicación.';
COMMENT ON COLUMN notificaciones.evento_notificable.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN notificaciones.evento_notificable.tipo IS 'UQ';
COMMENT ON COLUMN notificaciones.evento_notificable.categoria IS 'CK, IDX';
COMMENT ON COLUMN notificaciones.evento_notificable.prioridad IS 'un código de verificación es CRÍTICO (bloquea al usuario si no llega); un resumen mensual es BAJO.';
COMMENT ON COLUMN notificaciones.evento_notificable.es_transaccional IS 'si es transaccional, ignora el horario de no molestar. "Tu entrega se acreditó" o "tu pago fue rechazado" se avisan a la hora que sea. Un recordatorio de aporte, no.';
COMMENT ON COLUMN notificaciones.evento_notificable.permite_agrupacion IS 'evita mandar cinco mensajes cuando se pueden juntar en uno.';
COMMENT ON COLUMN notificaciones.evento_notificable.ventana_deduplicacion_min IS 'cuánto tiempo tiene que pasar para que el mismo aviso se considere nuevo y no repetido.';
COMMENT ON COLUMN notificaciones.evento_notificable.cadena_respaldo IS 'el orden de canales a intentar. WhatsApp → SMS → correo.';
