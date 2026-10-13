import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
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
    'evidencia/';

class _CamaraDePrueba implements CamaraEnVivo {
  const _CamaraDePrueba();

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
  Future<String> tomarFoto() async => 'sintetica';

  @override
  Future<void> liberar() async {}
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('pose frontal oscura · 360 px · texto 130 %', (tester) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.oscuro, Brightness.dark),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: child!,
          ),
          home: const PantallaDePruebaDeVida(
            poses: PoseDeVida.values,
            crearCamara: _CamaraDePrueba.new,
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
    expect(find.text('Mirá de frente'), findsOneWidget);
    expect(find.text('Poné tu cara dentro del óvalo.'), findsOneWidget);
    expect(find.text(TextosDeCaptura.tomarAhora), findsOneWidget);
    expect(find.text(TextosDeCaptura.cancelar), findsOneWidget);
    expect(tester.takeException(), isNull);

    for (final texto in [
      'FOTO 1 DE 3',
      'Mirá de frente',
      'Poné tu cara dentro del óvalo.',
      TextosDeCaptura.tomarAhora,
      TextosDeCaptura.cancelar,
    ]) {
      final rect = tester.getRect(find.text(texto));
      expect(rect.top, greaterThanOrEqualTo(0), reason: '$texto queda arriba');
      expect(
        rect.bottom,
        lessThanOrEqualTo(tester.view.physicalSize.height),
        reason: '$texto queda dentro de la vista',
      );
    }

    await expectLater(
      find.byType(PantallaDePruebaDeVida),
      matchesGoldenFile(
        '${_evidencia}h8-prueba-vida-oscura-frente-360-130.png',
      ),
    );
  });
}
