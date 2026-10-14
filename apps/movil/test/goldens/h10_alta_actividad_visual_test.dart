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
const _h8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-alta-oscuro-paso-5-360-130-scroll-restablecido.png';
const _omitirH8EnActualizacion = bool.fromEnvironment(
  'OMITIR_H8_EN_ACTUALIZACION',
);

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final tema in ['claro', 'oscuro']) {
      final reutilizaH8 = ancho == 360 && tema == 'oscuro';
      testWidgets('alta actividad vacía · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
        tester.view.physicalSize = Size(
          ancho.toDouble(),
          ancho == 360
              ? 740
              : ancho == 412
              ? 850
              : 960,
        );
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
        for (var i = 0; i < 4; i++) {
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
        expect(estado.paso, PasoAlta.actividad);
        expect(estado.origenDeFondos, isEmpty);
        expect(estado.detalleDelOrigen, isEmpty);
        expect(estado.actividadEconomica, isEmpty);
        expect(estado.detalleDeLaActividad, isEmpty);
        expect(estado.montoMensualEstimado, isNull);
        expect(
          find.text('FASE ${estado.numeroDeFase} DE ${estado.totalDeFases}'),
          findsOneWidget,
        );
        expect(
          Theme.of(tester.element(find.byType(Scaffold).first)).brightness,
          tema == 'oscuro' ? Brightness.dark : Brightness.light,
        );
        expect(
          MediaQuery.textScalerOf(
            tester.element(find.byType(Scaffold).first),
          ).scale(10),
          closeTo(13, 0.001),
        );
        final campos = tester.widgetList<EditableText>(
          find.byType(EditableText),
        );
        expect(campos, isNotEmpty);
        expect(campos.every((campo) => campo.controller.text.isEmpty), isTrue);
        expect(campos.any((campo) => campo.focusNode.hasFocus), isFalse);
        expect(tester.testTextInput.isVisible, isFalse);
        expect(tester.takeException(), isNull);

        await expectLater(
          find.byType(PantallaDeRegistro),
          matchesGoldenFile(
            reutilizaH8
                ? _h8
                : '${_evidencia}h10-alta-actividad-vacia-$tema-$ancho-130.png',
          ),
        );
        expect(campos.every((campo) => campo.controller.text.isEmpty), isTrue);
        await tester.pumpWidget(const SizedBox.shrink());
      }, skip: reutilizaH8 && _omitirH8EnActualizacion);
    }
  }
}
