import 'dart:io';

import 'package:integration_test/integration_test_driver.dart';

Future<void> main() async {
  await integrationDriver(
    responseDataCallback: (data) async {
      final screenshots = data?['screenshots'] as List<dynamic>? ?? const [];
      if (screenshots.length != 1) {
        throw StateError(
          'Se esperaba una captura H5.S4.M3; recibidas: ${screenshots.length}.',
        );
      }
      final raw = screenshots.single as Map<String, dynamic>;
      final bytes = List<int>.from(raw['bytes'] as List<dynamic>);
      final destino = File.fromUri(
        Directory.current.uri.resolve(
          '../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/h5-s4-m3-samsung-a06.png',
        ),
      );
      await destino.parent.create(recursive: true);
      await destino.writeAsBytes(bytes);
      // ignore: avoid_print
      print(
        'Evidencia guardada: ${destino.absolute.path} (${bytes.length} bytes)',
      );
    },
  );
}
