/// Los textos de «Completá tu verificación» (Perfil), en voz de marca (ADR-053). Antes
/// el título decía «KYC» y el error de carga era un enlace suelto.
class TextoCompletarKyc {
  TextoCompletarKyc._();
  static const titulo = 'Completá tu verificación';
  static const introTitulo = 'Mandá lo que falta';
  static const introExplicacion =
      'Después, una persona revisa tu documento y decide. Cómo va lo ves en tu perfil.';
  static const cargando = 'Buscando qué te falta';
  static const fotosQueFaltan = 'Fotos que faltan';
  static const perfilIzquierdo = 'Foto de perfil izquierdo';
  static const perfilDerecho = 'Foto de perfil derecho';
  // La misma indicación que el marco de la captura en el alta (`textos_de_captura`).
  static const consejoIzquierdo =
      'Girá la cabeza a tu izquierda, no el teléfono.';
  static const consejoDerecho = 'Girá la cabeza a tu derecha, no el teléfono.';
  static const tomarFoto = 'Tomar foto';
  static const tuDocumento = 'Tu carnet';
  static const vencimiento = 'Fecha de vencimiento';
  static const ayudaVencimiento = 'Copiala tal como figura en tu carnet.';
  static const guardarFecha = 'Guardar fecha';
  static const guardandoFecha = 'Guardando fecha…';
  static const elegiFecha = 'Elegí día, mes y año para poder guardarla.';
  static const errorFecha =
      'No pudimos guardar la fecha del documento. Probá de nuevo en un momento.';
  static const errorFoto =
      'No pudimos subir la foto. Probá de nuevo en un momento.';
  static const revisionTerminadaTitulo = 'La revisión ya terminó';
  static const revisionTerminada =
      'Tu verificación ya tiene resultado y estos datos no se cambian desde acá. Lo ves en tu perfil.';
  static const todoEnviado = 'Ya mandaste todo lo que faltaba';
  static const todoEnviadoDetalle =
      'Una persona revisa tu documento. El resultado aparece en tu perfil.';
}
