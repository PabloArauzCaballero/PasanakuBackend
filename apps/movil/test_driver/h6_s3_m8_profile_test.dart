import 'dart:io';

import 'package:integration_test/integration_test_driver.dart';

Future<void> main() async {
  await integrationDriver(
    responseDataCallback: (data) async {
      final screenshots = data?['screenshots'] as List<dynamic>? ?? const [];
      if (screenshots.length != 1) {
        throw StateError(
          'Se esperaba una captura H6.S3.M8; recibidas: ${screenshots.length}.',
        );
      }
      final raw = screenshots.single as Map<String, dynamic>;
      final bytes = List<int>.from(raw['bytes'] as List<dynamic>);
      final destino = File.fromUri(
        Directory.current.uri.resolve(
          '../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/h6-rendimiento.png',
        ),
      );
      await destino.parent.create(recursive: true);
      await destino.writeAsBytes(bytes);
      // ignore: avoid_print
      print(
        'Evidencia H6.S3.M8 guardada: ${destino.absolute.path} (${bytes.length} bytes)',
      );
    },
  );
}
