import 'package:aportaya_diseno/moleculas/intro_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

Widget _intro({bool peligro = false}) => conTema(
  IntroDePantalla(
    icono: Icons.key,
    titulo: 'Cambiá tu contraseña',
    explicacion: 'La nueva rige desde el próximo ingreso.',
    peligro: peligro,
  ),
);

void main() {
  testWidgets('el título es encabezado y la explicación se lee', (t) async {
    final semantica = t.ensureSemantics();
    await t.pumpWidget(_intro());
    expect(
      t.getSemantics(find.text('Cambiá tu contraseña')),
      matchesSemantics(label: 'Cambiá tu contraseña', isHeader: true),
    );
    expect(
      find.text('La nueva rige desde el próximo ingreso.'),
      findsOneWidget,
    );
    semantica.dispose();
  });

  testWidgets('el ícono es decorativo: no se anuncia', (t) async {
    final semantica = t.ensureSemantics();
    await t.pumpWidget(_intro());
    expect(find.bySemanticsLabel(RegExp('key')), findsNothing);
    semantica.dispose();
  });

  testWidgets('con peligro, el círculo va en el tono de error', (t) async {
    await t.pumpWidget(_intro(peligro: true));
    final caja = t.widget<DecoratedBox>(
      find
          .ancestor(of: find.byType(Icon), matching: find.byType(DecoratedBox))
          .first,
    );
    final tokens = Tokens.of(t.element(find.byType(IntroDePantalla)));
    expect((caja.decoration as BoxDecoration).color, tokens.errBg);
  });
}
