import 'dart:ui' show FramePhase, FrameTiming;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/scheduler.dart';

import 'dominio/puertos/camara_en_vivo.dart';
import 'infraestructura/plataforma.dart';
import 'pantallas/identidad/dominio/juez_de_pose.dart';
import 'pantallas/identidad/pantalla_prueba_de_vida.dart';

/// Punto de entrada temporal para medir H6.S3.M8 sin alterar `main.dart`.
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final medicion = _MedicionDeCuentaAtras();
  SchedulerBinding.instance.addTimingsCallback(medicion.registrar);
  final camara = _CamaraDePerfil(alIniciarStream: medicion.iniciarVentana);
  runApp(
    ProviderScope(
      child: MaterialApp(
        showPerformanceOverlay: true,
        theme: temaDesde(Tokens.oscuro, Brightness.dark),
        home: PantallaDePruebaDeVida(
          poses: [PoseDeVida.frente],
          crearCamara: () => camara,
          ahora: DateTime.now,
        ),
      ),
    ),
  );
}

class _MedicionDeCuentaAtras {
  Duration? _desde;
  Duration? _hasta;
  final _cuadros = <FrameTiming>[];
  var _reportada = false;

  void iniciarVentana() {
    final base = SchedulerBinding.instance.currentSystemFrameTimeStamp;
    final desde = base + const Duration(milliseconds: 1500);
    final hasta = base + const Duration(milliseconds: 4600);
    _desde = desde;
    _hasta = hasta;
    // ignore: avoid_print
    print('H6_PROFILE_WINDOW=${desde.inMicroseconds}:${hasta.inMicroseconds}');
  }

  void registrar(List<FrameTiming> cuadros) {
    final desde = _desde;
    final hasta = _hasta;
    if (desde == null || hasta == null || _reportada) return;
    for (final cuadro in cuadros) {
      final inicio = Duration(
        microseconds: cuadro.timestampInMicroseconds(FramePhase.vsyncStart),
      );
      if (inicio >= desde && inicio <= hasta) _cuadros.add(cuadro);
      if (inicio > hasta) {
        _imprimir();
        return;
      }
    }
  }

  void _imprimir() {
    _reportada = true;
    if (_cuadros.isEmpty) {
      // ignore: avoid_print
      print('H6_PROFILE_FRAME_TIMINGS count=0');
      return;
    }
    final maxUiMs = _cuadros
        .map((frame) => frame.buildDuration.inMicroseconds / 1000)
        .reduce((a, b) => a > b ? a : b);
    final maxRasterMs = _cuadros
        .map((frame) => frame.rasterDuration.inMicroseconds / 1000)
        .reduce((a, b) => a > b ? a : b);
    final over16Ms = _cuadros
        .where(
          (frame) =>
              frame.buildDuration > const Duration(milliseconds: 16) ||
              frame.rasterDuration > const Duration(milliseconds: 16),
        )
        .length;
    final samples = _cuadros
        .map(
          (frame) =>
              '${frame.frameNumber}:${(frame.buildDuration.inMicroseconds / 1000).toStringAsFixed(2)}/'
              '${(frame.rasterDuration.inMicroseconds / 1000).toStringAsFixed(2)}',
        )
        .join(',');
    // ignore: avoid_print
    print(
      'H6_PROFILE_FRAME_TIMINGS count=${_cuadros.length} '
      'max_ui_ms=${maxUiMs.toStringAsFixed(2)} '
      'max_raster_ms=${maxRasterMs.toStringAsFixed(2)} '
      'frames_over_16ms=$over16Ms samples_ms=$samples',
    );
  }
}

class _CamaraDePerfil implements CamaraEnVivo {
  _CamaraDePerfil({required this.alIniciarStream})
    : _real = camaraEnVivoDeLaPlataforma();

  final CamaraEnVivo _real;
  final VoidCallback alIniciarStream;

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
    await _real.escucharRostros((_) {});
    alIniciarStream();
    return false;
  }

  @override
  Future<void> dejarDeEscuchar() => _real.dejarDeEscuchar();

  @override
  Future<void> liberar() => _real.liberar();
}
