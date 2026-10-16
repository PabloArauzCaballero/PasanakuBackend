import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final tema in ['claro', 'oscuro']) {
      testWidgets('alta documento inicial · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
        final alto = ancho == 360
            ? 740.0
            : ancho == 412
            ? 850.0
            : 960.0;
        tester.view.physicalSize = Size(ancho.toDouble(), alto);
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 1.3;
        tester.platformDispatcher.accessibilityFeaturesTestValue =
            const FakeAccessibilityFeatures(disableAnimations: true);
        addTearDown(tester.view.reset);
        addTearDown(tester.platformDispatcher.clearAllTestValues);
        addTearDown(
          tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
        );

        final (:dio, adaptador: _) = dioSimulado();
        final contenedor = ProviderContainer(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(dio),
          ],
        );
        addTearDown(contenedor.dispose);
        final notifier = contenedor.read(altaProvider.notifier);
        for (var i = 0; i < 2; i++) {
          notifier.siguiente();
        }

        await tester.pumpWidget(
          UncontrolledProviderScope(
            container: contenedor,
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: tema == 'oscuro' ? ThemeMode.dark : ThemeMode.light,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(context).copyWith(disableAnimations: true),
                child: child!,
              ),
              home: const PantallaDeRegistro(),
            ),
          ),
        );
        await asentar(tester);

        final estado = contenedor.read(altaProvider);
        expect(estado.paso, PasoAlta.documento);
        expect(estado.datos.numeroDocumento, isEmpty);
        expect(estado.datos.lugarExpedicion, isNull);
        expect(estado.datos.fechaExpiracion, isNull);
        expect(estado.capturas.porCara, isEmpty);
        final campos = tester.widgetList<EditableText>(
          find.byType(EditableText),
        );
        expect(campos, hasLength(1));
        expect(campos.single.controller.text, isEmpty);
        expect(campos.single.focusNode.hasFocus, isFalse);
        expect(tester.testTextInput.isVisible, isFalse);
        expect(find.text('Tomar foto'), findsOneWidget);
        expect(tester.takeException(), isNull);

        await expectLater(
          find.byType(PantallaDeRegistro),
          matchesGoldenFile(
            '${_evidencia}h10-alta-documento-inicial-$tema-$ancho-130.png',
          ),
        );
        expect(estado.capturas.porCara, isEmpty);
        await tester.pumpWidget(const SizedBox.shrink());
      });
    }
  }
}
