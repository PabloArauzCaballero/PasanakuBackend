import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';

import '../dominio/juez_de_pose.dart';
import 'trazos_de_cara.dart';

/// Silueta facial de la prueba de vida, con el exterior del encuadre velado.
class SiluetaDeCara extends StatelessWidget {
  const SiluetaDeCara({
    super.key,
    required this.pose,
    this.progreso = 0,
    this.tomada = false,
  });

  final PoseDeVida pose;
  final double progreso;
  final bool tomada;

  @override
  Widget build(BuildContext context) {
    final tokens = Tokens.of(context);
    final objetivo = tomada ? 1.0 : progreso.clamp(0.0, 1.0).toDouble();

    return IgnorePointer(
      child: RepaintBoundary(
        child: TweenAnimationBuilder<double>(
          tween: Tween<double>(begin: objetivo, end: objetivo),
          duration: duracion(context, Movimiento.micro),
          builder: (context, avance, _) => CustomPaint(
            size: Size.infinite,
            painter: PintorDeSiluetaDeCara(
              pose: pose,
              color: Color.lerp(
                tokens.camaraMidiendo,
                tokens.camaraLista,
                avance,
              )!,
              velo: tokens.camaraVelo,
              guia: tokens.camaraGuia,
              progreso: avance,
            ),
          ),
        ),
      ),
    );
  }
}

/// Pinta el velo, las guías y el contorno en el espacio disponible.
class PintorDeSiluetaDeCara extends CustomPainter {
  const PintorDeSiluetaDeCara({
    required this.pose,
    required this.color,
    required this.velo,
    required this.guia,
    required this.progreso,
  });

  final PoseDeVida pose;
  final Color color;
  final Color velo;
  final Color guia;
  final double progreso;

  static const _lienzo = 300.0;

  double get grosor => 3.0 + progreso.clamp(0.0, 1.0).toDouble() * 2;

  /// Geometría en el espacio de referencia: el mismo óvalo para las tres poses.
  Path get contornoNormalizado => contornoDeRostro();

  /// Las flechas de giro, espejadas para el perfil derecho; ninguna de frente.
  Path? get flechasNormalizadas {
    if (pose == PoseDeVida.frente) return null;
    if (pose == PoseDeVida.izquierda) return flechasDeGiro();
    final espejo = Matrix4.identity()
      ..translateByDouble(_lienzo, 0, 0, 1)
      ..scaleByDouble(-1.0, 1.0, 1, 1);
    return flechasDeGiro().transform(espejo.storage);
  }

  /// El óvalo ocupa ~78 % del ancho y la mitad del alto, dejando libre el pie.
  Float64List _matriz(Size size) {
    final escala = (size.width * 0.78 / _lienzo)
        .clamp(0.0, size.height * 0.5 / _lienzo)
        .toDouble();
    final dx = (size.width - _lienzo * escala) / 2;
    return (Matrix4.identity()
          ..translateByDouble(dx, size.height * 0.2, 0, 1)
          ..scaleByDouble(escala, escala, 1, 1))
        .storage;
  }

  Path areaVelada(Size size) => Path.combine(
    PathOperation.difference,
    Path()..addRect(Offset.zero & size),
    contornoNormalizado.transform(_matriz(size)),
  );

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) return;
    final matriz = _matriz(size);

    canvas.drawPath(areaVelada(size), Paint()..color = velo);
    canvas.drawPath(
      contornoNormalizado.transform(matriz),
      Paint()
        ..style = PaintingStyle.stroke
        ..color = color
        ..strokeWidth = grosor,
    );

    final flechas = flechasNormalizadas;
    if (flechas == null) return;
    // La flecha más cercana al óvalo, la más marcada: se lee como movimiento.
    var opacidad = 1.0;
    for (final metrica in flechas.transform(matriz).computeMetrics()) {
      canvas.drawPath(
        metrica.extractPath(0, metrica.length),
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeCap = StrokeCap.round
          ..strokeJoin = StrokeJoin.round
          ..strokeWidth = 3
          ..color = guia.withValues(alpha: guia.a * opacidad),
      );
      opacidad -= 0.3;
    }
  }

  @override
  bool shouldRepaint(PintorDeSiluetaDeCara oldDelegate) =>
      pose != oldDelegate.pose ||
      color != oldDelegate.color ||
      velo != oldDelegate.velo ||
      guia != oldDelegate.guia ||
      progreso != oldDelegate.progreso;
}
