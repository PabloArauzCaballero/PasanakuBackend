import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_capturas.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../goldens/cargar_fuentes_de_aplicacion.dart';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('el peek oculta títulos vecinos y conserva los rótulos', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(412, 850);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [altaProvider.overrideWith(_AltaEnCapturas.new)],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: SingleChildScrollView(
              child: PasoCapturas(onContinuar: () {}),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    final activo = find.byType(TarjetaDeCaptura).first;
    final vecino = find.byType(TarjetaDeCaptura).at(1);
    final encabezadoActivo = find.descendant(
      of: activo,
      matching: find.byType(Visibility),
    );
    final encabezadoVecino = find.descendant(
      of: vecino,
      matching: find.byType(Visibility),
    );
    expect(tester.widget<Visibility>(encabezadoActivo).visible, isTrue);
    expect(tester.widget<Visibility>(encabezadoVecino).visible, isFalse);
    for (final rotulo in [
      'Frente',
      'Dorso',
      'De frente',
      'Perfil izq.',
      'Perfil der.',
    ]) {
      expect(find.text(rotulo), findsOneWidget);
    }
    expect(tester.takeException(), isNull);
  });
}

class _AltaEnCapturas extends AltaNotifier {
  @override
  EstadoAlta build() => const EstadoAlta(paso: PasoAlta.documento);
}
