import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../ayudas_de_captura.dart';
import 'inventario.dart';
import 'matriz.dart';

/// Fotografía cada pantalla del inventario en la matriz completa. Cada captura deja una
/// línea en `evidencia/<fase>/registro.tsv` (pantalla, estado, viewport, tema, ruta final,
/// excepción, pedidos sin contrato) para que la revisión no dependa de la memoria.
void main() {
  final registro = <String>[];
  setUpAll(cargarFuentesReales);
  tearDownAll(() {
    final archivo = File(
      '../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/$fase/registro.tsv',
    )..createSync(recursive: true);
    final previo = archivo.readAsLinesSync().where((l) => l.isNotEmpty);
    archivo.writeAsStringSync('${{...previo, ...registro}.join('\n')}\n');
  });

  for (final p in pantallasDeCaptura.where((p) => flujoPedido(p.flujo))) {
    for (final estado in p.estados) {
      for (final MapEntry(key: vp, value: tam) in viewportsDeCaptura.entries) {
        for (final MapEntry(key: tema, value: brillo)
            in temasDeCaptura.entries) {
          testWidgets('${p.flujo}/${p.nombre} · ${estado.name} · $vp · $tema', (
            t,
          ) async {
            final (adaptador, salir) = await montarRuta(
              t,
              p,
              estado,
              tam,
              brillo,
            );
            final excepcion = t.takeException();
            await expectLater(
              find.byType(MaterialApp),
              matchesGoldenFile(rutaDeCaptura(p, estado, vp, tema)),
            );
            // Sale de la pantalla antes de terminar: sus temporizadores (sondeos,
            // cuentas atrás) no pueden quedar vivos.
            final alSalir = await salir();
            registro.add(
              [
                '${p.flujo}/${p.nombre}',
                estado.name,
                vp,
                tema,
                excepcion == null
                    ? 'sin-excepcion'
                    : '$excepcion'.split('\n').first,
                adaptador.sinContrato.join(' | '),
                alSalir.join(' | '),
              ].join('\t'),
            );
          });
        }
      }
    }
  }
}
