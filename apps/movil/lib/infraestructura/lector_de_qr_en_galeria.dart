import 'package:image_picker/image_picker.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../dominio/puertos/lector_de_qr_en_imagen.dart';

/// El adaptador real: `image_picker` abre la galería (el selector de fotos del sistema,
/// sin pedir permiso de almacenamiento) y `mobile_scanner` lee el QR del archivo con
/// `analyzeImage`, sin prender la cámara. La imagen no se guarda ni se sube.
class LectorDeQrEnGaleria implements LectorDeQrEnImagen {
  LectorDeQrEnGaleria({ImagePicker? selector})
    : _selector = selector ?? ImagePicker();

  final ImagePicker _selector;

  @override
  Future<LecturaDeImagen> elegirYLeer() async {
    final archivo = await _selector.pickImage(source: ImageSource.gallery);
    if (archivo == null) return const LecturaCancelada();
    final controlador = MobileScannerController(
      autoStart: false,
      formats: const [BarcodeFormat.qrCode],
    );
    try {
      final captura = await controlador.analyzeImage(
        archivo.path,
        formats: const [BarcodeFormat.qrCode],
      );
      for (final codigo in captura?.barcodes ?? const <Barcode>[]) {
        final texto = codigo.rawValue;
        if (texto != null && texto.isNotEmpty) return LecturaConQr(texto);
      }
      return const LecturaSinQr();
    } on Object {
      // Imagen dañada, formato raro o lector no disponible: para la persona es lo mismo.
      return const LecturaSinQr();
    } finally {
      await controlador.dispose();
    }
  }
}
