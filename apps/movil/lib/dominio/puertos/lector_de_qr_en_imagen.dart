/// Leer el QR de una imagen que la persona elige de su galería (2026-10-20): el QR del
/// grupo llega como foto por WhatsApp y se guarda en el teléfono. Leerlo no mueve plata
/// ni da entrada a nada: solo lleva a la pantalla donde la persona pide unirse.
abstract interface class LectorDeQrEnImagen {
  /// Abre la galería y lee el primer QR de la imagen elegida.
  Future<LecturaDeImagen> elegirYLeer();
}

/// Lo que salió de elegir una imagen.
sealed class LecturaDeImagen {
  const LecturaDeImagen();
}

/// La persona cerró la galería sin elegir nada.
final class LecturaCancelada extends LecturaDeImagen {
  const LecturaCancelada();
}

/// La imagen no tiene un QR que se pueda leer.
final class LecturaSinQr extends LecturaDeImagen {
  const LecturaSinQr();
}

/// El texto del QR. Todavía no está validado: lo valida quien lo usa.
final class LecturaConQr extends LecturaDeImagen {
  const LecturaConQr(this.texto);
  final String texto;
}
