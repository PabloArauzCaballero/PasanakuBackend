/// La entrada de las pantallas de trámite del perfil (`IntroDePantalla`, ADR-053), el
/// botón principal de cada una y el motivo de cada botón apagado. Cada texto describe
/// lo que la pantalla ya hace: no promete nada que el servidor no confirme.
abstract final class TextosDeTramites {
  static const contrasenaTitulo = 'Elegí una contraseña nueva';
  static const contrasenaExplicacion =
      'Primero escribí la que usás hoy. La nueva necesita 8 caracteres o más, '
      'al menos un número y ningún espacio.';
  static const contrasenaBloqueo =
      'Escribí tu contraseña actual y una nueva que cumpla las reglas.';
  static const contrasenaGuardar = 'Guardar contraseña nueva';

  // La baja se cuenta sin culpa y sin esconder la salida: qué pasa, qué hace falta
  // antes, y un botón que dice exactamente lo que hace. Se confirma una vez más en la
  // hoja, con «Cancelar» al lado.
  static const bajaTitulo = 'Antes de dar de baja tu cuenta';
  static const bajaExplicacion =
      'Revisá qué pasa con tu cuenta. Después lo confirmás una vez más.';
  static const bajaConsecuencia = 'Tu cuenta queda inhabilitada';
  static const bajaCondicion =
      'Si tenés saldo o cupos activos, primero hay que resolverlos.';
  static const bajaBoton = 'Dar de baja mi cuenta';

  static const dispositivosTitulo = '¿Este teléfono es solo tuyo?';
  static const dispositivosExplicacion =
      'Marcá la casilla solo si nadie más lo usa. Si es un teléfono compartido, '
      'dejala sin marcar.';
  static const dispositivosSeguir = 'Ir al inicio';

  static const pepTitulo = 'Una pregunta de cumplimiento';
  static const pepExplicacion =
      'Se la hacemos a todas las cuentas. Respondé lo que corresponda a tu caso.';
  static const pepNo = 'No';
  static const pepSi = 'Sí';
  static const pepBloqueo = 'Elegí «Sí» o «No» para seguir.';
  static const pepConfirmar = 'Confirmar respuesta';
}
