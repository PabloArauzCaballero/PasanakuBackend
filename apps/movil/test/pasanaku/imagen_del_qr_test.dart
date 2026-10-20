import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:flutter_test/flutter_test.dart';

/// La imagen del QR que se manda por WhatsApp (2026-10-20): un PNG cuadrado de fondo
/// blanco, más alto si lleva el nombre del grupo debajo. El enlace es sintético.
void main() {
  const enlace =
      'https://app.aportaya.161.97.85.216.sslip.io/g/'
      'c6900000-0000-4000-8000-0000000000e2.'
      'abababababababababababababababababababababababababababababababab';

  Future<ui.Image> decodificar(List<int> png) async {
    final codec = await ui.instantiateImageCodec(
      png is Uint8List ? png : Uint8List.fromList(png),
    );
    return (await codec.getNextFrame()).image;
  }

  testWidgets('correcto: es un PNG de 1080 × 1080', (tester) async {
    await tester.runAsync(() async {
      final png = await codigoQrComoPng(enlace);
      expect(png.sublist(0, 4), [0x89, 0x50, 0x4E, 0x47]);
      final imagen = await decodificar(png);
      expect([imagen.width, imagen.height], [1080, 1080]);
    });
  });

  testWidgets('límite: con el nombre del grupo la imagen crece para el texto', (
    tester,
  ) async {
    await tester.runAsync(() async {
      final imagen = await decodificar(
        await codigoQrComoPng(enlace, titulo: 'Pasanaku del barrio'),
      );
      expect([imagen.width, imagen.height], [1080, 1200]);
    });
  });

  testWidgets('inválido: un nombre en blanco no agrega espacio vacío', (
    tester,
  ) async {
    await tester.runAsync(() async {
      final imagen = await decodificar(
        await codigoQrComoPng(enlace, titulo: '   '),
      );
      expect(imagen.height, 1080);
    });
  });
}
