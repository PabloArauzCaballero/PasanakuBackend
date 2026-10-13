import 'dart:convert';

import 'package:flutter/services.dart';

/// Los goldens usan Ahem por defecto; registra las fuentes incluidas en la app.
Future<void> cargarFuentesDeAplicacion() async {
  final manifiesto =
      jsonDecode(await rootBundle.loadString('FontManifest.json'))
          as List<dynamic>;

  for (final entrada in manifiesto.cast<Map<String, dynamic>>()) {
    final cargador = FontLoader(entrada['family'] as String);
    for (final fuente
        in (entrada['fonts'] as List<dynamic>).cast<Map<String, dynamic>>()) {
      cargador.addFont(rootBundle.load(fuente['asset'] as String));
    }
    await cargador.load();
  }
}
