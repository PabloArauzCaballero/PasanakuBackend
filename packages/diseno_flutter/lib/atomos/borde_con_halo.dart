import 'package:flutter/material.dart';

/// El borde de un campo con foco: la línea de 2 px y, por fuera, un anillo suave
/// (H4.S1.M8). Se pinta desde el borde y no con una sombra alrededor del widget, para
/// que abrace solo la caja y no la ayuda ni el error que van debajo.
class BordeConHalo extends OutlineInputBorder {
  const BordeConHalo({
    super.borderSide,
    super.borderRadius,
    required this.halo,
    required this.anchoDeHalo,
  });

  final Color halo;
  final double anchoDeHalo;

  @override
  BordeConHalo copyWith({
    BorderSide? borderSide,
    BorderRadius? borderRadius,
    double? gapPadding,
  }) => BordeConHalo(
    borderSide: borderSide ?? this.borderSide,
    borderRadius: borderRadius ?? this.borderRadius,
    halo: halo,
    anchoDeHalo: anchoDeHalo,
  );

  @override
  void paint(
    Canvas canvas,
    Rect rect, {
    double? gapStart,
    double gapExtent = 0.0,
    double gapPercentage = 0.0,
    TextDirection? textDirection,
  }) {
    final mitad = anchoDeHalo / 2;
    final afuera = borderRadius
        .toRRect(rect)
        .inflate(borderSide.width / 2 + mitad);
    canvas.drawRRect(
      afuera,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = anchoDeHalo
        ..color = halo,
    );
    super.paint(
      canvas,
      rect,
      gapStart: gapStart,
      gapExtent: gapExtent,
      gapPercentage: gapPercentage,
      textDirection: textDirection,
    );
  }
}
