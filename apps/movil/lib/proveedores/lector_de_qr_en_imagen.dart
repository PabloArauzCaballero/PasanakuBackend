import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/lector_de_qr_en_imagen.dart';
import '../infraestructura/lector_de_qr_en_galeria.dart';

/// Leer el QR de una imagen de la galería. Se sobreescribe en pruebas con un doble.
final lectorDeQrEnImagenProvider = Provider<LectorDeQrEnImagen>(
  (_) => LectorDeQrEnGaleria(),
);
