import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../infraestructura/imagen_del_qr.dart';

/// Arma la imagen PNG del QR y devuelve su ruta. En pruebas se reemplaza por un doble
/// que no escribe en disco.
typedef ArmarImagenDelQr =
    Future<String> Function(String contenido, {String? titulo});

final imagenDelQrProvider = Provider<ArmarImagenDelQr>(
  (_) => guardarImagenDelQr,
);
