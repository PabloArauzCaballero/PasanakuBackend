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

  /// Geometría en el espacio de referencia, útil para validar el espejo del perfil.
  Path get contornoNormalizado {
    if (pose == PoseDeVida.frente) return contornoDeFrente();
    return _segunPose(contornoDePerfil());
  }

  Path get _detalleNormalizado {
    if (pose == PoseDeVida.frente) return orejasDeFrente();
    return _segunPose(orejaDePerfil());
  }

  Path _segunPose(Path trazo) {
    if (pose == PoseDeVida.izquierda) return trazo;
    final espejo = Matrix4.identity()
      ..translateByDouble(_lienzo, 0, 0, 1)
      ..scaleByDouble(-1.0, 1.0, 1, 1);
    return trazo.transform(espejo.storage);
  }

  Path areaVelada(Size size) {
    final escala = (size.width * 0.78 / _lienzo)
        .clamp(0.0, size.height * 0.5 / _lienzo)
        .toDouble();
    final dx = (size.width - _lienzo * escala) / 2;
    final matriz = Matrix4.identity()
      ..translateByDouble(dx, size.height * 0.2, 0, 1)
      ..scaleByDouble(escala, escala, 1, 1);
    final contorno = contornoNormalizado.transform(matriz.storage);
    return Path.combine(
      PathOperation.difference,
      Path()..addRect(Offset.zero & size),
      contorno,
    );
  }

  @override
  void paint(Canvas canvas, Size size) {
    if (size.isEmpty) return;

    // La cara ocupa ~78 % del ancho y la mitad del alto, dejando libre el pie.
    final escala = (size.width * 0.78 / _lienzo)
        .clamp(0.0, size.height * 0.5 / _lienzo)
        .toDouble();
    final dx = (size.width - _lienzo * escala) / 2;
    final dy = size.height * 0.2;
    final matriz = Matrix4.identity()
      ..translateByDouble(dx, dy, 0, 1)
      ..scaleByDouble(escala, escala, 1, 1);
    final transformacion = matriz.storage;
    final deFrente = pose == PoseDeVida.frente;
    final contorno = contornoNormalizado.transform(transformacion);
    final detalle = _detalleNormalizado.transform(transformacion);

    canvas.drawPath(areaVelada(size), Paint()..color = velo);

    final grosor = this.grosor;
    final trazo = Paint()
      ..style = PaintingStyle.stroke
      ..strokeJoin = StrokeJoin.round
      ..strokeCap = StrokeCap.round
      ..color = color
      ..strokeWidth = grosor;
    canvas.drawPath(contorno, trazo);
    canvas.drawPath(detalle, trazo..strokeWidth = grosor * 0.85);
    if (deFrente) {
      _punteado(
        canvas,
        guiasDeFrente().transform(transformacion),
        Paint()
          ..style = PaintingStyle.stroke
          ..color = guia
          ..strokeWidth = 1.5,
      );
    } else {
      canvas.drawPath(detalle, Paint()..color = color.withValues(alpha: 0.25));
    }
  }

  void _punteado(Canvas canvas, Path camino, Paint pintura) {
    for (final metrica in camino.computeMetrics()) {
      var inicio = 0.0;
      while (inicio < metrica.length) {
        canvas.drawPath(metrica.extractPath(inicio, inicio + 4), pintura);
        inicio += 10;
      }
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
