import 'package:flutter/widgets.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../dominio/puertos/visor_qr.dart';

/// El adaptador real sobre `mobile_scanner`: solo QR, y la cámara vive lo que vive el
/// widget (se apaga al salir de la pantalla).
Widget visorDeMobileScanner({
  required ValueChanged<String> alLeer,
  required Widget Function(ProblemaDeCamara) problema,
}) => _Visor(alLeer: alLeer, problema: problema);

class _Visor extends StatefulWidget {
  const _Visor({required this.alLeer, required this.problema});

  final ValueChanged<String> alLeer;
  final Widget Function(ProblemaDeCamara) problema;

  @override
  State<_Visor> createState() => _VisorState();
}

class _VisorState extends State<_Visor> {
  final _controlador = MobileScannerController(
    formats: const [BarcodeFormat.qrCode],
  );

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => MobileScanner(
    controller: _controlador,
    onDetect: (captura) {
      for (final codigo in captura.barcodes) {
        final texto = codigo.rawValue;
        if (texto != null) {
          widget.alLeer(texto);
          return;
        }
      }
    },
    errorBuilder: (context, error) => widget.problema(switch (error.errorCode) {
      MobileScannerErrorCode.permissionDenied => ProblemaDeCamara.denegado,
      MobileScannerErrorCode.unsupported => ProblemaDeCamara.noDisponible,
      _ => ProblemaDeCamara.otro,
    }),
  );
}
