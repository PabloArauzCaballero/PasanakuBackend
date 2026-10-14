import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_documento.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../goldens/cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h4-s3-icono';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('compara visualmente el carnet con tarjeta de pago', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(360, 800);
    addTearDown(tester.view.reset);

    final contenedor = ProviderContainer(
      overrides: [generacionDeSesionProvider.overrideWithValue(0)],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(
            body: SingleChildScrollView(child: PasoDocumento()),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.badge_outlined), findsWidgets);
    expect(find.byIcon(Icons.credit_card), findsNothing);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PasoDocumento),
      matchesGoldenFile('$_evidencia/anverso-360.png'),
    );

    await tester.ensureVisible(find.text('Número de CI'));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.badge_outlined), findsWidgets);
    expect(tester.takeException(), isNull);
    await expectLater(
      find.byType(PasoDocumento),
      matchesGoldenFile('$_evidencia/numero-ci-360.png'),
    );
  });
}
