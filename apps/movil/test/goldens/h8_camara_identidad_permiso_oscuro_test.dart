import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';

class _CamaraSinPermiso implements CamaraEnVivo {
  _CamaraSinPermiso(this.resultado);

  final EstadoDePermiso resultado;
  int pedidos = 0;
  int inicios = 0;
  int fotos = 0;
  int liberaciones = 0;

  @override
  Future<EstadoDePermiso> pedirPermiso() async {
    pedidos++;
    return resultado;
  }

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {
    inicios++;
    throw StateError('No se debe iniciar la cámara si no hay permiso.');
  }

  @override
  Widget vista() => throw StateError('El visor no existe sin permiso.');

  @override
  Future<String> tomarFoto() async {
    fotos++;
    throw StateError('No se debe tomar fotos sin permiso.');
  }

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async =>
      false;

  @override
  Future<void> dejarDeEscuchar() async {}

  @override
  Future<void> liberar() async {
    liberaciones++;
  }
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final (permiso, titulo, accion, archivo) in [
    (
      EstadoDePermiso.denegado,
      TextosDeCaptura.necesitamosTuCamara,
      TextosDeCaptura.permitirCamara,
      'h8-camara-identidad-denegada-oscura-360-130.png',
    ),
    (
      EstadoDePermiso.denegadoParaSiempre,
      TextosDeCaptura.permisoBloqueado,
      TextosDeCaptura.abrirAjustes,
      'h8-camara-identidad-bloqueada-oscura-360-130.png',
    ),
  ]) {
    testWidgets('permiso de identidad $permiso · oscuro · 360 · texto 130 %', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 740);
      tester.view.devicePixelRatio = 1;
      tester.platformDispatcher.textScaleFactorTestValue = 1.3;
      tester.platformDispatcher.accessibilityFeaturesTestValue =
          const FakeAccessibilityFeatures(disableAnimations: true);
      addTearDown(tester.view.reset);
      addTearDown(tester.platformDispatcher.clearAllTestValues);
      addTearDown(
        tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
      );

      final camara = _CamaraSinPermiso(permiso);
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            theme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: ThemeMode.dark,
            home: PantallaDeCamara(cara: CaraDelCarril.anverso, camara: camara),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        Theme.of(tester.element(find.byType(PantallaDeCamara))).brightness,
        Brightness.dark,
      );
      expect(find.text(titulo), findsOneWidget);
      expect(find.text(accion), findsOneWidget);
      expect(find.text(TextosDeCaptura.tomarFoto), findsOneWidget);
      expect(find.text(TextosIdentidad.elegirDeLasFotos), findsOneWidget);
      expect(
        tester
            .widget<Boton>(
              find.ancestor(
                of: find.text(TextosDeCaptura.tomarFoto),
                matching: find.byType(Boton),
              ),
            )
            .onPressed,
        isNull,
      );
      expect(camara.pedidos, 1);
      expect(camara.inicios, 0);
      expect(camara.fotos, 0);
      expect(tester.takeException(), isNull);
      expect(tester.getRect(find.text(titulo)).top, greaterThanOrEqualTo(0));
      expect(
        tester.getRect(find.text(TextosIdentidad.elegirDeLasFotos)).bottom,
        lessThanOrEqualTo(tester.view.physicalSize.height),
      );

      await expectLater(
        find.byType(PantallaDeCamara),
        matchesGoldenFile('$_evidencia$archivo'),
      );

      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
      expect(camara.liberaciones, 1);
      expect(camara.inicios, 0);
      expect(camara.fotos, 0);
      expect(tester.takeException(), isNull);
    });
  }
}
