/// Textos del perfil (ADR-053). Van aparte porque `textos.dart` ya está en el tope de
/// 200 líneas del gate. Cada texto describe lo que la pantalla hace hoy: no promete
/// plazos ni resultados que decide otra persona o el servidor.
abstract final class TextosPerfil {
  static const saludo = 'Cuenta';
  static const seccionCuenta = 'Tu cuenta';
  static const seccionSeguridad = 'Seguridad y ayuda';
  static const correo = 'Correo';
  static const cerrandoSesion = 'Cerrando sesión…';
  static const ayuda = 'Ayuda';

  // Pistas del lector de pantalla: qué pasa al tocar la fila.
  static const pistaContrasena = 'Abre el cambio de contraseña.';
  static const pistaGuia = 'Vuelve a mostrar la guía de inicio.';
  static const pistaAyuda = 'Abre los tutoriales de ayuda.';
  static const pistaBaja = 'Abre la pantalla para dar de baja tu cuenta.';

  // La tarjeta de la verificación de identidad. Sin «KYC» ni «expediente»: es jerga
  // de cumplimiento, la persona lo llama «verificar mi identidad».
  static const identidadTitulo = 'Tu identidad';
  static const identidadConsultando = 'Consultando cómo va tu verificación…';
  static const identidadErrorTitulo = 'No pudimos ver cómo va tu verificación';
  static const identidadErrorDetalle =
      'Revisá tu conexión y probá de nuevo. Tus datos no cambiaron.';
  static const identidadReintentar = 'Reintentar';
  static const identidadSinSesion =
      'Iniciá sesión para ver cómo va tu verificación.';
  static const identidadCompletar = 'Completar verificación';
  static const identidadCompleta =
      'Ya mandaste todo. Una persona revisa tu documento; el resultado aparece acá.';
  static const identidadRechazada =
      'Comunicate con soporte de AportaYa para saber el motivo y qué hacer ahora.';

  /// «…falta: foto de perfil izquierdo y fecha de vencimiento…». La lista viene del
  /// expediente real; acá solo se arma la oración.
  static String identidadFalta(List<String> faltantes) {
    // Las etiquetas del dominio vienen con mayúscula inicial («Foto de…»); en medio de
    // la oración van en minúscula.
    final items = [
      for (final f in faltantes)
        f.isEmpty ? f : f[0].toLowerCase() + f.substring(1),
    ];
    if (items.length == 1) {
      return 'Para revisar tu identidad falta: ${items.single}.';
    }
    final inicio = items.sublist(0, items.length - 1).join(', ');
    return 'Para revisar tu identidad falta: $inicio y ${items.last}.';
  }
}
