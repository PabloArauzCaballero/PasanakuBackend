import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_prueba_de_vida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _h8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-prueba-vida-oscura-frente-360-130.png';
const _omitirH8EnActualizacion = bool.fromEnvironment(
  'OMITIR_H8_EN_ACTUALIZACION',
);

class _CamaraSintetica implements CamaraEnVivo {
  bool fotoTomada = false;
  bool liberada = false;

  @override
  Future<EstadoDePermiso> pedirPermiso() async => EstadoDePermiso.concedido;

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {}

  @override
  Widget vista() => const SizedBox.expand();

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) _) async => true;

  @override
  Future<void> dejarDeEscuchar() async {}

  @override
  Future<String> tomarFoto() async {
    fotoTomada = true;
    throw StateError('La captura está prohibida en esta prueba visual.');
  }

  @override
  Future<void> liberar() async => liberada = true;
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final tema in ['claro', 'oscuro']) {
      final reutilizaH8 = ancho == 360 && tema == 'oscuro';
      testWidgets(
        'prueba de vida inicial · $tema · $ancho dp · texto 130 %',
        (tester) async {
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

          final camara = _CamaraSintetica();
          await tester.pumpWidget(
            ProviderScope(
              child: MaterialApp(
                theme: temaDesde(
                  tema == 'oscuro' ? Tokens.oscuro : Tokens.claro,
                  tema == 'oscuro' ? Brightness.dark : Brightness.light,
                ),
                builder: (context, child) => MediaQuery(
                  data: MediaQuery.of(
                    context,
                  ).copyWith(disableAnimations: true),
                  child: child!,
                ),
                home: PantallaDePruebaDeVida(
                  poses: PoseDeVida.values,
                  crearCamara: () => camara,
                  ahora: () => DateTime.utc(2026, 1, 1),
                ),
              ),
            ),
          );
          await tester.pumpAndSettle();

          expect(
            tester.widget<Scaffold>(find.byType(Scaffold)).backgroundColor,
            Paleta.ink,
          );
          expect(find.text('FOTO 1 DE 3'), findsOneWidget);
          expect(
            find.text(TextosDeCaptura.tituloDePose(PoseDeVida.frente)),
            findsOneWidget,
          );
          expect(
            find.text(TextosDeCaptura.instruccionDePose(PoseDeVida.frente)),
            findsOneWidget,
          );
          expect(find.text(TextosDeCaptura.tomarAhora), findsOneWidget);
          expect(
            tester
                .widget<Boton>(
                  find.ancestor(
                    of: find.text(TextosDeCaptura.tomarAhora),
                    matching: find.byType(Boton),
                  ),
                )
                .onPressed,
            isNotNull,
          );
          expect(find.text(TextosDeCaptura.cancelar), findsOneWidget);
          expect(camara.fotoTomada, isFalse);
          expect(tester.takeException(), isNull);

          for (final texto in [
            'FOTO 1 DE 3',
            TextosDeCaptura.tituloDePose(PoseDeVida.frente),
            TextosDeCaptura.instruccionDePose(PoseDeVida.frente),
            TextosDeCaptura.tomarAhora,
            TextosDeCaptura.cancelar,
          ]) {
            final rect = tester.getRect(find.text(texto));
            expect(rect.top, greaterThanOrEqualTo(0), reason: '$texto arriba');
            expect(
              rect.bottom,
              lessThanOrEqualTo(tester.view.physicalSize.height),
              reason: '$texto dentro del viewport',
            );
          }

          await expectLater(
            find.byType(PantallaDePruebaDeVida),
            matchesGoldenFile(
              reutilizaH8
                  ? _h8
                  : '${_evidencia}h10-prueba-vida-inicial-$tema-$ancho-130.png',
            ),
          );
          expect(camara.fotoTomada, isFalse);
          await tester.pumpWidget(const SizedBox.shrink());
          expect(camara.liberada, isTrue);
        },
        skip: reutilizaH8 && _omitirH8EnActualizacion,
      );
    }
  }
}
