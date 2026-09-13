import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Dibuja los seis trazos de [Marca]. Las coordenadas son las del SVG de la marca,
/// en su lienzo original de 160×160 con origen en (20, 20); el pintor lo escala al
/// tamaño que le toque.
class PintorDeMarca extends CustomPainter {
  const PintorDeMarca({
    required this.trazo,
    required this.acento,
    this.avance = 1,
    this.volumen = false,
  });

  final Color trazo;
  final Color acento;

  /// De 0 a 1: qué proporción de cada trazo está dibujada.
  final double avance;

  /// Da **volumen** a los trazos: cada uno lleva un degradado de arriba abajo, más
  /// claro donde pega la luz y más oscuro donde se apoya.
  ///
  /// Solo se enciende cuando la marca es la protagonista y se ve en grande (el zoom
  /// de entrada). En un sello de 48 px no se distingue y solo ensucia el color.
  final bool volumen;

  /// (puntos de la curva, grosor, ¿es acento?) — el orden es el del SVG: primero los
  /// tres trazos exteriores, después los tres de adentro.
  static const _trazos = <({List<double> curva, double grosor, bool acento})>[
    (curva: [92, 30, 74, 66, 52, 108, 40, 168], grosor: 13, acento: false),
    (curva: [108, 30, 126, 66, 148, 108, 160, 168], grosor: 13, acento: false),
    (curva: [46, 158, 86, 140, 114, 140, 154, 158], grosor: 12, acento: false),
    (curva: [100, 58, 84, 92, 68, 130, 62, 170], grosor: 12, acento: true),
    (curva: [100, 58, 116, 92, 132, 130, 138, 170], grosor: 12, acento: true),
    (curva: [70, 150, 100, 134, 100, 134, 130, 150], grosor: 10, acento: true),
  ];

  @override
  void paint(Canvas lienzo, Size medida) {
    const lienzoOriginal = 160.0;
    const origen = 20.0;
    final escala = medida.shortestSide / lienzoOriginal;
    final recorte = avance.clamp(0.0, 1.0);
    if (recorte == 0) return;

    lienzo.save();
    lienzo.scale(escala);
    lienzo.translate(-origen, -origen);

    for (final t in _trazos) {
      final c = t.curva;
      final camino = Path()
        ..moveTo(c[0], c[1])
        ..cubicTo(c[2], c[3], c[4], c[5], c[6], c[7]);
      final color = t.acento ? acento : trazo;
      final pincel = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = t.grosor
        ..strokeCap = StrokeCap.round
        ..color = color;
      if (volumen) pincel.shader = _degradado(color, camino.getBounds());
      lienzo.drawPath(
        recorte == 1 ? camino : _parcial(camino, recorte),
        pincel,
      );
    }
    lienzo.restore();
  }

  /// El degradado de un trazo: se aclara arriba y se apaga abajo, dentro de la caja
  /// del propio trazo, para que cada uno tenga su luz y no una común que los aplane.
  Shader _degradado(Color color, Rect caja) => LinearGradient(
    begin: Alignment.topCenter,
    end: Alignment.bottomCenter,
    colors: [
      Color.lerp(color, Paleta.white, 0.22)!,
      color,
      Color.lerp(color, Paleta.g900, 0.18)!,
    ],
    stops: const [0, 0.55, 1],
  ).createShader(caja);

  /// El tramo inicial de un camino, para que el trazo se dibuje como si lo estuvieran
  /// escribiendo en vez de aparecer entero.
  Path _parcial(Path camino, double proporcion) {
    final salida = Path();
    for (final medida in camino.computeMetrics()) {
      salida.addPath(
        medida.extractPath(0, medida.length * proporcion),
        Offset.zero,
      );
    }
    return salida;
  }

  @override
  bool shouldRepaint(PintorDeMarca otro) =>
      otro.trazo != trazo ||
      otro.acento != acento ||
      otro.avance != avance ||
      otro.volumen != volumen;
}
