import 'dart:typed_data';
import 'dart:ui' as ui;

/// El chequeo de calidad que Atlas aplica al carnet antes de subirlo (no a las
/// selfies: una selfie no tiene una proporción fija que comprobar).
///
/// Solo mide dos cosas, las mismas que Atlas: que la foto no sea diminuta, y que la
/// proporción se parezca a la de un carnet ID-1. No mira nitidez ni brillo — eso
/// Atlas tampoco lo mide por software; lo que ofrece son los consejos (dibujos) y
/// confía en que la persona los siga.
class DimensionesDeImagen {
  const DimensionesDeImagen(this.ancho, this.alto);
  final int ancho;
  final int alto;
}

/// Lado largo mínimo exigido, en píxeles. Igual al de Atlas.
const ladoLargoMinimo = 1400;

/// La proporción de un carnet ID-1 (85.6 mm × 54 mm) y su tolerancia, igual que
/// Atlas: ±12 %, para que no haya que alinear el carnet al milímetro.
const _proporcionCarnet = 85.6 / 54;
const _toleranciaProporcion = 0.12;

/// Lee solo la cabecera de la imagen para saber su tamaño, sin decodificarla
/// entera — una foto de carnet pesa varios megapíxeles y no hace falta cargarlos
/// en memoria solo para medir el lado.
Future<DimensionesDeImagen> medir(Uint8List bytes) async {
  final buffer = await ui.ImmutableBuffer.fromUint8List(bytes);
  final descriptor = await ui.ImageDescriptor.encoded(buffer);
  final dimensiones = DimensionesDeImagen(descriptor.width, descriptor.height);
  descriptor.dispose();
  buffer.dispose();
  return dimensiones;
}

/// `null` si la captura pasa; si no, el mensaje que Atlas le muestra a la persona
/// para que repita la foto. Solo se aplica al anverso y al reverso del carnet —las
/// selfies no tienen una proporción fija que comprobar.
String? evaluarCalidad({
  required int ancho,
  required int alto,
  required bool esDocumento,
}) {
  final ladoLargo = ancho > alto ? ancho : alto;
  if (ladoLargo < ladoLargoMinimo) {
    return 'La imagen salió muy pequeña. Acerca un poco el teléfono.';
  }
  if (!esDocumento) return null;

  final ladoCorto = ancho > alto ? alto : ancho;
  if (ladoCorto == 0) {
    return 'No parece el carnet entero. Repite con los cuatro bordes a la vista.';
  }
  final proporcion = ladoLargo / ladoCorto;
  final desvio = (proporcion - _proporcionCarnet).abs() / _proporcionCarnet;
  if (desvio > _toleranciaProporcion) {
    return 'No parece el carnet entero. Repite con los cuatro bordes a la vista.';
  }
  return null;
}
