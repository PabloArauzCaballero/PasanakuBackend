import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/puertos/visor_qr.dart';
import '../infraestructura/visor_qr_mobile_scanner.dart';

/// El visor de QR. Se sobreescribe en pruebas con uno que no necesita cámara.
final visorQrProvider = Provider<ConstructorDeVisor>(
  (_) => visorDeMobileScanner,
);
