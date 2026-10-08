import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('el primer error reaparece aunque su campo ya tenía foco', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          home: const PantallaDeRegistro(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final primerCampo = find.byType(TextField).first;
    await tester.ensureVisible(primerCampo);
    await tester.tap(primerCampo);
    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();

    final continuar = find.text(TextosIdentidad.continuar);
    await tester.ensureVisible(continuar);
    await tester.pumpAndSettle();
    expect(tester.getTopLeft(primerCampo).dy, lessThan(0));
    expect(tester.widget<TextField>(primerCampo).focusNode?.hasFocus, isTrue);

    await tester.tap(continuar);
    await tester.pumpAndSettle();

    final error = find.text('Escribí al menos dos letras.').first;
    expect(error, findsOneWidget);
    expect(tester.getTopLeft(primerCampo).dy, greaterThanOrEqualTo(80));
    expect(tester.getBottomRight(error).dy, lessThanOrEqualTo(460));
    expect(tester.takeException(), isNull);
  });
}
