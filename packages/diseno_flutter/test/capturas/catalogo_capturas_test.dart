import 'dart:io';

import 'package:aportaya_diseno/catalogo/catalogo.dart';
import 'package:aportaya_diseno/catalogo/muestras.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import '../goldens/catalogo_golden_test.dart' show sinAcentos;

/// Evidencia del rediseño bancario (PLAN H0.S3.M7 y H4.S3.M8): cada grupo del catálogo con
/// las fuentes REALES (los goldens usan la de bloques), en claro y oscuro, a 360 px.
/// `flutter test test/capturas --update-goldens --dart-define=FASE=antes|despues`.
/// NO son goldens de comparación: son capturas para mirar.
const _fase = String.fromEnvironment('FASE', defaultValue: 'antes');
const _salida =
    '../../../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/componentes/$_fase';

Future<void> _cargarFuentesReales() async {
  Future<void> familia(String nombre, List<String> rutas) async {
    final cargador = FontLoader(nombre);
    for (final r in rutas) {
      cargador.addFont(
        File(r).readAsBytes().then((b) => ByteData.view(b.buffer)),
      );
    }
    await cargador.load();
  }

  await familia('Bricolage', [
    for (final w in [600, 700, 800]) 'lib/fuentes/Bricolage-$w.ttf',
  ]);
  await familia('Instrument', [
    for (final w in [400, 500, 600]) 'lib/fuentes/Instrument-$w.ttf',
  ]);
  final sdk = Platform.environment['FLUTTER_ROOT'];
  if (sdk != null) {
    await familia('MaterialIcons', [
      '$sdk/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf',
    ]);
  }
}

void main() {
  final hallazgos = <String>[];
  setUpAll(_cargarFuentesReales);
  // Un desborde con las fuentes reales es un HALLAZGO del antes: se anota y se fotografía.
  tearDownAll(
    () =>
        File(
            '../../docs/trabajo/2026-10-21-rediseno-bancario/evidencia/componentes/$_fase/registro.txt',
          )
          ..createSync(recursive: true)
          ..writeAsStringSync('${hallazgos.join('\n')}\n'),
  );
  for (final grupo in catalogo) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      testWidgets('${grupo.grupo} · $tema', (tester) async {
        tester.view.physicalSize = const Size(360, 2400);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conTema(
            PaginaDeCatalogo(grupo: grupo),
            brillo: brillo,
            pantallaEntera: true,
          ),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));
        final error = tester.takeException();
        hallazgos.add(
          '${grupo.grupo} · $tema\t'
          '${error == null ? 'sin-excepcion' : '$error'.split('\n').first}',
        );
        final archivo = sinAcentos(grupo.grupo)
            .toLowerCase()
            .replaceAll(RegExp(r'[^a-z]+'), '_')
            .replaceAll(RegExp(r'^_|_$'), '');
        await expectLater(
          find.byType(PaginaDeCatalogo),
          matchesGoldenFile('$_salida/${archivo}__$tema.png'),
        );
      });
    }
  }
}
