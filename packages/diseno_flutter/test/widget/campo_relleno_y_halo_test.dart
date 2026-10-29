import 'package:aportaya_diseno/atomos/borde_con_halo.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

InputDecoration _decoracion(WidgetTester t) =>
    t.widget<InputDecorator>(find.byType(InputDecorator)).decoration;

void main() {
  testWidgets('el campo tiene relleno suave, no blanco de tarjeta', (t) async {
    await t.pumpWidget(conTema(const Campo(etiqueta: 'Nombre')));
    expect(_decoracion(t).fillColor, Tokens.claro.surface2);
  });

  testWidgets('con foco: borde de marca de 2 px y anillo de marca', (t) async {
    await t.pumpWidget(conTema(const Campo(etiqueta: 'Nombre')));
    await t.tap(find.byType(TextField));
    await t.pump();
    final borde = _decoracion(t).focusedBorder! as BordeConHalo;
    expect(borde.borderSide.color, Tokens.claro.brand);
    expect(borde.borderSide.width, Borde.desfase);
    expect(borde.halo, Tokens.claro.halo);
  });

  testWidgets('con error, el anillo del foco es de error', (t) async {
    await t.pumpWidget(
      conTema(const Campo(etiqueta: 'Nombre', error: 'Falta el nombre')),
    );
    await t.tap(find.byType(TextField));
    await t.pump();
    final tema = Theme.of(t.element(find.byType(TextField)));
    final borde = tema.inputDecorationTheme.focusedErrorBorder! as BordeConHalo;
    expect(borde.borderSide.color, Tokens.claro.err);
    expect(borde.halo, Tokens.claro.errBg);
  });
}
