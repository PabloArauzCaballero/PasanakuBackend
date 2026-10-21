-- Borrador de onboarding incompleto (servicio identidad).
-- Generado por scripts/generar_ddl.py — no editar a mano. Fuente: contrato BorradorOnboarding +
-- BorradorOnboardingRepositorio (ver BORRADOR_COLUMNAS en el generador).

CREATE TABLE IF NOT EXISTS identidad.borrador_onboarding (
  correo                 VARCHAR(150) PRIMARY KEY,
  telefono_e164          VARCHAR(20),
  nombres                VARCHAR(80),
  apellidos              VARCHAR(80),
  fecha_nacimiento       DATE,
  tipo_documento         VARCHAR(25),
  numero_documento       VARCHAR(30),
  lugar_expedicion       VARCHAR(5),
  fecha_expiracion       DATE,
  actividad_economica    VARCHAR(10),
  detalle_actividad      VARCHAR(100),
  origen_de_fondos       VARCHAR(20),
  detalle_del_origen     VARCHAR(100),
  monto_mensual_estimado TEXT,
  paso_pendiente         VARCHAR(10) NOT NULL,
  actualizado_en         TIMESTAMPTZ NOT NULL,
  expira_en              TIMESTAMPTZ NOT NULL,
  CONSTRAINT ck_identidad_borrador_tipo_documento CHECK (tipo_documento IN ('CI', 'PASAPORTE', 'CARNET_EXTRANJERO')),
  CONSTRAINT ck_identidad_borrador_paso_pendiente CHECK (paso_pendiente IN ('cuenta', 'correo', 'documento', 'datos', 'actividad', 'contrato'))
);
COMMENT ON TABLE identidad.borrador_onboarding IS 'Datos de un alta a medio llenar para retomarla; se borra al completar el alta.';
COMMENT ON COLUMN identidad.borrador_onboarding.correo IS 'Correo ya confirmado de quien está dándose de alta; identifica el borrador.';
COMMENT ON COLUMN identidad.borrador_onboarding.telefono_e164 IS 'Teléfono en formato E.164 que la persona cargó.';
COMMENT ON COLUMN identidad.borrador_onboarding.nombres IS 'Nombres tal como los escribió la persona.';
COMMENT ON COLUMN identidad.borrador_onboarding.apellidos IS 'Apellidos tal como los escribió la persona.';
COMMENT ON COLUMN identidad.borrador_onboarding.fecha_nacimiento IS 'Fecha de nacimiento declarada.';
COMMENT ON COLUMN identidad.borrador_onboarding.tipo_documento IS 'Tipo de documento elegido.';
COMMENT ON COLUMN identidad.borrador_onboarding.numero_documento IS 'Número de documento que la persona cargó.';
COMMENT ON COLUMN identidad.borrador_onboarding.lugar_expedicion IS 'Lugar de expedición del documento.';
COMMENT ON COLUMN identidad.borrador_onboarding.fecha_expiracion IS 'Vencimiento del documento.';
COMMENT ON COLUMN identidad.borrador_onboarding.actividad_economica IS 'Código de actividad económica elegido.';
COMMENT ON COLUMN identidad.borrador_onboarding.detalle_actividad IS 'Detalle libre de la actividad.';
COMMENT ON COLUMN identidad.borrador_onboarding.origen_de_fondos IS 'Origen de fondos elegido.';
COMMENT ON COLUMN identidad.borrador_onboarding.detalle_del_origen IS 'Detalle libre del origen de fondos.';
COMMENT ON COLUMN identidad.borrador_onboarding.monto_mensual_estimado IS 'Monto mensual estimado tal como llegó en el contrato (texto, sin tope declarado).';
COMMENT ON COLUMN identidad.borrador_onboarding.paso_pendiente IS 'Paso del alta donde la persona retoma.';
COMMENT ON COLUMN identidad.borrador_onboarding.actualizado_en IS 'Último guardado del borrador.';
COMMENT ON COLUMN identidad.borrador_onboarding.expira_en IS 'Desde aquí el borrador ya no se ofrece para retomar (siete días desde el último guardado).';
-- El repositorio lo borra al completar el alta (BorradorOnboardingRepositorio.eliminar); el GRANT general
-- de 03_permisos solo da SELECT, INSERT y UPDATE.
GRANT DELETE ON identidad.borrador_onboarding TO svc_identidad;
