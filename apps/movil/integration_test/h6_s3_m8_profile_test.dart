import 'dart:ui' show FrameTiming;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/infraestructura/plataforma.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_prueba_de_vida.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

const _omitirStreamMl = bool.fromEnvironment('H6_PROFILE_OMITIR_STREAM_ML');

/// Keeps the native preview and ML stream active while forcing the documented
/// no-detector fallback, since the AVD has no person in front of its camera.
class _CamaraDePerfil implements CamaraEnVivo {
  _CamaraDePerfil() : _real = camaraEnVivoDeLaPlataforma();

  final CamaraEnVivo _real;

  @override
  Future<EstadoDePermiso> pedirPermiso() => _real.pedirPermiso();

  @override
  Future<void> iniciar({required bool frontal, bool conDeteccion = false}) =>
      _real.iniciar(frontal: frontal, conDeteccion: conDeteccion);

  @override
  Widget vista() => _real.vista();

  @override
  Future<String> tomarFoto() => _real.tomarFoto();

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async {
    if (!_omitirStreamMl) await _real.escucharRostros((_) {});
    return false;
  }

  @override
  Future<void> dejarDeEscuchar() => _real.dejarDeEscuchar();

  @override
  Future<void> liberar() => _real.liberar();
}

void main() {
  final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('H6.S3.M8 overlay de rendimiento durante la cuenta atrás', (
    tester,
  ) async {
    // ignore: avoid_print
    print('H6_S3_M8_STREAM_ML_OMITIDO=$_omitirStreamMl');
    var midiendoCuenta = false;
    final cuadrosDeCuenta = <FrameTiming>[];
    void registrarCuadros(List<FrameTiming> cuadros) {
      if (midiendoCuenta) cuadrosDeCuenta.addAll(cuadros);
    }

    binding.addTimingsCallback(registrarCuadros);
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          showPerformanceOverlay: true,
          theme: temaDesde(Tokens.oscuro, Brightness.dark),
          home: const PantallaDePruebaDeVida(
            poses: [PoseDeVida.frente],
            crearCamara: _CamaraDePerfil.new,
            ahora: DateTime.now,
          ),
        ),
      ),
    );

    var cuentaVisible = false;
    for (var frame = 0; frame < 100; frame++) {
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 100)),
      );
      // `runAsync` waits for the native camera, but does not advance the
      // integration test's fake clock. Advance one frame too, so the periodic
      // pose ticker can leave its 1.6 s breathing phase and reach the fallback.
      await tester.pump(const Duration(milliseconds: 100));
      if (find.byKey(const ValueKey(3)).evaluate().isNotEmpty ||
          find.byKey(const ValueKey(2)).evaluate().isNotEmpty ||
          find.byKey(const ValueKey(1)).evaluate().isNotEmpty) {
        cuentaVisible = true;
        break;
      }
    }
    expect(
      cuentaVisible,
      isTrue,
      reason: 'La cámara no llegó a mostrar la cuenta atrás en perfil.',
    );
    // Take the visual proof as soon as the countdown appears. Waiting through
    // the measurement window may auto-capture the photo and replace the state
    // the screenshot is supposed to document.
    await binding.convertFlutterSurfaceToImage();
    await tester.pump();
    final bytes = await binding.takeScreenshot('h6-s3-m8-rendimiento');
    // ignore: avoid_print
    print('H6_S3_M8_SCREENSHOT=${bytes.length} bytes');

    midiendoCuenta = true;
    // Keep the native camera alive while pumping regularly. One long
    // `runAsync` followed by one `pump` collected only a single FrameTiming,
    // which cannot validate a continuous 16 ms budget.
    for (var frame = 0; frame < 15; frame++) {
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 100)),
      );
      await tester.pump(const Duration(milliseconds: 100));
    }
    midiendoCuenta = false;
    binding.removeTimingsCallback(registrarCuadros);
    expect(cuadrosDeCuenta, isNotEmpty, reason: 'No hubo cuadros medibles.');
    final maximoUiMs = cuadrosDeCuenta
        .map((cuadro) => cuadro.buildDuration.inMicroseconds / 1000)
        .reduce((a, b) => a > b ? a : b);
    final maximoRasterMs = cuadrosDeCuenta
        .map((cuadro) => cuadro.rasterDuration.inMicroseconds / 1000)
        .reduce((a, b) => a > b ? a : b);
    final sobre16Ms = cuadrosDeCuenta
        .where(
          (cuadro) =>
              cuadro.buildDuration > const Duration(milliseconds: 16) ||
              cuadro.rasterDuration > const Duration(milliseconds: 16),
        )
        .length;
    // ignore: avoid_print
    print(
      'H6_S3_M8_COUNTDOWN_FRAME_TIMINGS count=${cuadrosDeCuenta.length} '
      'max_ui_ms=${maximoUiMs.toStringAsFixed(2)} '
      'max_raster_ms=${maximoRasterMs.toStringAsFixed(2)} '
      'frames_over_16ms=$sobre16Ms '
      'samples_ms=${cuadrosDeCuenta.map((cuadro) => '${cuadro.frameNumber}:'
          '${(cuadro.buildDuration.inMicroseconds / 1000).toStringAsFixed(2)}/'
          '${(cuadro.rasterDuration.inMicroseconds / 1000).toStringAsFixed(2)}').join(',')}',
    );

    await tester.tap(find.text('Cancelar'));
    await tester.pump(const Duration(seconds: 1));
  });
}
