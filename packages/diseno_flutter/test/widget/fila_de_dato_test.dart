import 'package:aportaya_diseno/moleculas/fila_de_dato.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _huella =
    '9f2c4e1a7b3d5f6e8a0c2b4d6f8e0a1c3e5f7b9d1a3c5e7f9b1d3f5a7c9e1b3d5';

void main() {
  testWidgets('el valor se puede seleccionar para copiarlo', (t) async {
    await t.pumpWidget(
      conTema(const FilaDeDato(etiqueta: 'Huella', valor: _huella)),
    );
    expect(find.widgetWithText(SelectableText, _huella), findsOneWidget);
    expect(find.text('Huella'), findsOneWidget);
  });

  testWidgets('una huella larga no desborda a 360 dp', (t) async {
    t.view.physicalSize = const Size(360, 640);
    t.view.devicePixelRatio = 1;
    addTearDown(t.view.reset);
    await t.pumpWidget(
      conTema(const FilaDeDato(etiqueta: 'Huella', valor: _huella), escala: 2),
    );
    expect(t.takeException(), isNull);
  });
}
