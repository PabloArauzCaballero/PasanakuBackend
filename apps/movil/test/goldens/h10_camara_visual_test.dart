import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/consejos_para_la_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/cabecera_de_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/guia_de_encuadre.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';

class _CamaraSintetica implements CamaraEnVivo {
  static const claveVista = Key('h10-camara-sintetica');

  bool? frontal;
  bool fotoTomada = false;
  bool liberada = false;

  @override
  Future<EstadoDePermiso> pedirPermiso() async => EstadoDePermiso.concedido;

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {
    this.frontal = frontal;
  }

  @override
  Widget vista() => const ColoredBox(key: claveVista, color: Color(0xFF101010));

  @override
  Future<String> tomarFoto() async {
    fotoTomada = true;
    throw StateError('La cámara sintética de H10 no debe tomar fotos.');
  }

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async =>
      false;

  @override
  Future<void> dejarDeEscuchar() async {}

  @override
  Future<void> liberar() async {
    liberada = true;
  }
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      testWidgets('cámara concedida · $tema · $ancho dp · texto 130 %', (
        tester,
      ) async {
        final alto = switch (ancho) {
          360 => 740,
          412 => 850,
          _ => 960,
        };
        tester.view.physicalSize = Size(ancho.toDouble(), alto.toDouble());
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
          MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.light
                ? ThemeMode.light
                : ThemeMode.dark,
            home: PantallaDeCamara(cara: CaraDelCarril.anverso, camara: camara),
          ),
        );
        await tester.pumpAndSettle();

        final pantalla = find.byType(PantallaDeCamara);
        final contexto = tester.element(pantalla);
        expect(Theme.of(contexto).brightness, brillo);
        expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
        expect(camara.frontal, isFalse);
        expect(camara.fotoTomada, isFalse);

        final visor = tester.getRect(find.byKey(_CamaraSintetica.claveVista));
        expect(visor.width, closeTo(ancho.toDouble(), 1));
        expect(visor.height, greaterThan(500));
        expect(find.byType(GuiaDeEncuadre), findsOneWidget);
        expect(find.byType(ConsejosParaLaFoto), findsOneWidget);
        expect(
          tester.getRect(find.byType(ConsejosParaLaFoto)).top,
          greaterThanOrEqualTo(
            tester.getRect(find.byType(CabeceraDeCamara)).bottom,
          ),
        );
        expect(find.text(TextosDeCaptura.tomarFoto), findsOneWidget);
        expect(find.text(TextosDeCaptura.cancelar), findsOneWidget);
        expect(
          tester
              .widget<Boton>(
                find.ancestor(
                  of: find.text(TextosDeCaptura.tomarFoto),
                  matching: find.byType(Boton),
                ),
              )
              .onPressed,
          isNotNull,
        );
        expect(
          tester.getRect(find.text(TextosDeCaptura.tomarFoto)).center.dy,
          greaterThan(alto * 0.7),
        );
        expect(tester.takeException(), isNull);

        await expectLater(
          pantalla,
          matchesGoldenFile(
            '${_evidencia}h10-camara-concedida-$tema-$ancho-130.png',
          ),
        );

        await tester.pumpWidget(const SizedBox.shrink());
        await tester.pump();
        expect(camara.liberada, isTrue);
        expect(camara.fotoTomada, isFalse);
        expect(tester.takeException(), isNull);
      });
    }
  }
}
