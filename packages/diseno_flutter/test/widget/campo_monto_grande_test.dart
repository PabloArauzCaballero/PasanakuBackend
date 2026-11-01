import 'package:aportaya_diseno/atomos/campo_monto_grande.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

Widget _campo(
  ValueChanged<String?> onChanged, {
  List<int> sugeridos = const [],
  double escala = 1,
}) => conTema(
  Scaffold(
    body: CampoMontoGrande(
      etiqueta: 'Monto a recargar',
      onChanged: onChanged,
      sugeridos: sugeridos,
    ),
  ),
  escala: escala,
  pantallaEntera: true,
);

/// ADR-053: el importe es la cifra protagonista, con el mismo contrato que CampoMonto.
void main() {
  testWidgets('devuelve la cadena del contrato, nunca un double', (
    tester,
  ) async {
    String? ultimo = 'sin-cambio';
    await tester.pumpWidget(_campo((v) => ultimo = v));
    await tester.enterText(find.byType(TextField), '1.240,5');
    expect(ultimo, '1240.50');
    await tester.enterText(find.byType(TextField), 'abc');
    // El formateador solo deja pasar dígitos, puntos y comas: lo escrito queda vacío.
    expect(ultimo, isNull);
  });

  testWidgets('tiene nombre accesible y la etiqueta se ve', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_campo((_) {}));
    expect(find.text('Monto a recargar'), findsOneWidget);
    expect(find.bySemanticsLabel(RegExp('Monto a recargar')), findsWidgets);
    semantica.dispose();
  });

  testWidgets('los montos sugeridos rellenan el campo y se marcan', (
    tester,
  ) async {
    String? ultimo;
    await tester.pumpWidget(
      _campo((v) => ultimo = v, sugeridos: const [50, 100]),
    );
    await tester.tap(find.text('Bs 100'));
    await tester.pump();
    expect(ultimo, '100.00');
    expect(
      tester.widget<TextField>(find.byType(TextField)).controller!.text,
      '100',
    );
  });

  testWidgets('a 320 px y texto al 200 % no desborda', (tester) async {
    tester.view.physicalSize = const Size(320, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      _campo((_) {}, sugeridos: const [50, 100, 200, 500], escala: 2),
    );
    await tester.enterText(find.byType(TextField), '9.999.999,99');
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
}
