-- Generado por scripts/generar_ddl.py — no editar a mano.
-- Borrador del alta incompleta (identidad): lo que la persona ya cargo para
-- retomar el alta en otro momento. Lo usa BorradorOnboardingRepositorio.
-- Una fila por correo; vence a los 7 dias (expira_en) y se borra al terminar
-- el alta. Idempotente: se puede aplicar sobre una base que ya la tiene.
CREATE TABLE IF NOT EXISTS identidad.borrador_onboarding (
  correo                  VARCHAR(254) PRIMARY KEY,
  telefono_e164           VARCHAR(20),
  nombres                 VARCHAR(120),
  apellidos               VARCHAR(120),
  fecha_nacimiento        DATE,
  tipo_documento          VARCHAR(30),
  numero_documento        VARCHAR(40),
  lugar_expedicion        VARCHAR(60),
  fecha_expiracion        DATE,
  actividad_economica     VARCHAR(80),
  detalle_actividad       VARCHAR(500),
  origen_de_fondos        VARCHAR(80),
  detalle_del_origen      VARCHAR(500),
  monto_mensual_estimado  VARCHAR(40),
  paso_pendiente          VARCHAR(30)  NOT NULL DEFAULT 'datos',
  actualizado_en          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expira_en               TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_identidad_borrador_expira
  ON identidad.borrador_onboarding (expira_en);

-- Los permisos por defecto del esquema dan SELECT/INSERT/UPDATE; el borrador
-- ademas se borra al confirmar el alta.
GRANT SELECT, INSERT, UPDATE, DELETE ON identidad.borrador_onboarding TO svc_identidad;
