/// Los textos de la pantalla de estado de la verificación (CU-02), uno a uno
/// iguales a los de Atlas: pendiente, en revisión, aprobada o rechazada.
class TextosDeVerificacion {
  TextosDeVerificacion._();

  static const pendienteTitulo = 'Estamos revisando tu documento';
  static const pendienteDetalle = 'No cierres la app.';
  static const enRevisionTitulo = 'Recibimos tus documentos';
  static const enRevisionDetalle =
      'Un especialista está verificando tu identidad. Mientras tanto ya podés '
      'entrar y conocer AportaYa.';
  static const cuentaEnVerificacionTitulo = 'Estamos verificando tu identidad';
  static const cuentaEnVerificacionDetalle =
      'Ya podés usar la app. La billetera y los grupos se activan apenas '
      'confirmemos tu identidad.';
  static const accesoAunNoHabilitado =
      'Tu identidad ya está verificada. Tocá «Activar mi cuenta» para empezar a usar '
      'la billetera y los grupos.';
  static const aprobadaTitulo = 'Identidad verificada';
  static const pasoRecibido = 'Recibido';
  static const pasoEnRevision = 'En revisión';
  static const pasoListo = 'Listo';
  static const pasoRechazado = 'Rechazado';
  static const rechazadaTitulo = 'No pudimos validar tu documento';
  static const intentarDeNuevo = 'Intentar de nuevo';
  static const seguirConElRegistro = 'Entrar a AportaYa';
  static const irALaApp = 'Ir a la app';
  static const activarCuenta = 'Activar mi cuenta';
  // Verificación diferida (2026-10-17): la cuenta se crea sin carnet y la
  // verificación se pide al mover dinero o entrar a un pasanaku.
  static const avisoSinVerificar =
      'Verificá tu identidad para mover dinero y participar en pasanakus.';
  static const requeridaTitulo = 'Verificá tu identidad';
  static const requeridaDetalle =
      'Para mover dinero y participar en pasanakus necesitamos confirmar que sos '
      'vos. Te pedimos tu carnet, unas fotos y tu actividad: toma unos minutos.';
  static const requeridaVerificar = 'Verificar mi identidad';
  static const requeridaAhoraNo = 'Ahora no';
  static const requeridaEnRevisionDetalle =
      'Recibimos tus documentos. Apenas una persona los confirme vas a poder '
      'hacer esta operación.';
  static const requeridaVerEstado = 'Ver el estado';
  static const requeridaVerificadaTitulo = 'Tu identidad ya está verificada';
  static const requeridaVerificadaDetalle =
      'Activá tu cuenta para seguir con esta operación.';
  static const requeridaNoDisponible =
      'Esta operación no está disponible para tu cuenta. Escribinos desde Ayuda '
      'y lo revisamos.';
  static const volver = 'Volver';
  static const verificarTitulo = 'Verificar mi identidad';
  static const enviandoVerificacion = 'Enviando tus datos…';
  static const enviarVerificacion = 'Enviar mis datos';
  static const enviarVerificacionDetalle =
      'Vamos a enviar tu carnet y tus fotos para que una persona los revise. '
      'Mientras tanto podés seguir usando la app.';
  static const cuentaActivada =
      'Tu identidad fue verificada. Ya podés usar la billetera y los grupos.';
  static const avisoVerificando =
      'Estamos verificando tu identidad. La billetera y los grupos se activan al terminar.';
  static const avisoAprobada =
      'Tu identidad está verificada. Tocá para activar tu cuenta.';
  static const avisoRechazada =
      'No pudimos verificar tu identidad. Tocá para ver qué pasó.';
  static const entrar = 'Entrar';
  // «Consultar estado» (2026-10-19): la persona pregunta si ya la aprobaron sin salir
  // de donde está.
  static const consultarEstado = 'Consultar estado';
  static const consultando = 'Consultando…';
  static const aprobadaConsultar =
      'Tu identidad ya está verificada. Tocá «Consultar estado» para activar la '
      'billetera y los grupos.';
  static const sigueEnRevision =
      'Tu identidad sigue en revisión. Te avisamos apenas la aprueben.';
  static const noSePudoConsultar =
      'No pudimos consultar el estado. Revisá tu conexión y probá de nuevo.';
  static const enRevisionGrupos =
      'Mientras revisamos tu identidad podés ver tus grupos e invitaciones. Para '
      'crear un grupo o unirte a uno necesitamos terminar la revisión.';
  static const actualizarEstado = 'Actualizar estado';
  static const salirDeCuenta = 'Salir de esta cuenta';
  static const continuarRegistro = 'Continuar registro / verificación';
  static const actualizarAcceso = 'Cerrar sesión para actualizar el acceso';
  static const noSePudoActivar =
      'No pudimos activar tu cuenta todavía. Probá de nuevo en un momento.';
  static String caso(String verificacionId) =>
      'Caso ${verificacionId.length > 8 ? verificacionId.substring(0, 8) : verificacionId}';
}

/// Los motivos de rechazo que el motor devuelve, traducidos — mismo catálogo que
/// Atlas.
String motivoDeRechazo(String? codigo) => switch (codigo) {
  'DOCUMENTO_NO_VALIDO' => 'El documento no se pudo validar.',
  'IDENTIDAD_NO_COINCIDE' => 'La cara no coincide con el documento.',
  'SOSPECHA_DE_FRAUDE' => 'El documento quedó marcado para revisión.',
  'REQUIERE_REVISION' => 'Necesita una revisión manual.',
  null => 'No pudimos confirmar el motivo.',
  _ => codigo,
};
