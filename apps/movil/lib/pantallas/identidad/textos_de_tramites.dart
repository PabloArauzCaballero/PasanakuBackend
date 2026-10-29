/// La entrada de las pantallas de trámite del perfil (`IntroDePantalla`, ADR-053) y el
/// motivo de cada botón apagado. Cada texto describe lo que la pantalla ya hace: no
/// promete nada que el servidor no confirme.
abstract final class TextosDeTramites {
  static const contrasenaTitulo = 'Elegí una contraseña nueva';
  static const contrasenaExplicacion =
      'Primero confirmá la que usás hoy. La nueva lleva 8 caracteres o más, al menos un número y ningún espacio.';
  static const contrasenaBloqueo =
      'Escribí tu contraseña actual y una nueva que cumpla las reglas.';

  static const bajaTitulo = 'Antes de dar de baja tu cuenta';
  static const bajaExplicacion =
      'Leé qué pasa con tu cuenta. Te pedimos que lo confirmes una vez más antes de hacerlo.';

  static const dispositivosTitulo = 'Este dispositivo';
  static const dispositivosExplicacion =
      'Marcalo solo si este teléfono es tuyo y no lo usa nadie más.';

  static const pepTitulo = 'Una pregunta de cumplimiento';
  static const pepExplicacion =
      'Se la hacemos a todas las cuentas. Respondé lo que corresponda a tu caso.';
  static const pepBloqueo = 'Elegí «Sí» o «No» para seguir.';
}
