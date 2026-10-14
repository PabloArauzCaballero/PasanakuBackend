import 'package:flutter/widgets.dart';

/// Trazos vectoriales de Atlas en un lienzo normalizado de 300 × 300.
Path contornoDeFrente() => Path()
  ..moveTo(150, 30)
  ..cubicTo(208, 30, 242, 76, 242, 140)
  ..cubicTo(242, 190, 222, 238, 188, 266)
  ..cubicTo(174, 277, 162, 282, 150, 282)
  ..cubicTo(138, 282, 126, 277, 112, 266)
  ..cubicTo(78, 238, 58, 190, 58, 140)
  ..cubicTo(58, 76, 92, 30, 150, 30)
  ..close();

Path orejasDeFrente() => Path()
  ..moveTo(60, 118)
  ..cubicTo(44, 116, 40, 150, 56, 168)
  ..moveTo(240, 118)
  ..cubicTo(256, 116, 260, 150, 244, 168);

Path guiasDeFrente() => Path()
  ..moveTo(150, 42)
  ..lineTo(150, 270)
  ..moveTo(92, 140)
  ..lineTo(208, 140);

Path contornoDePerfil() => Path()
  ..moveTo(170, 34)
  ..cubicTo(120, 34, 100, 68, 100, 94)
  ..cubicTo(100, 108, 98, 116, 98, 122)
  ..cubicTo(98, 130, 104, 134, 104, 140)
  ..cubicTo(96, 152, 82, 164, 76, 172)
  ..cubicTo(74, 178, 82, 180, 96, 182)
  ..cubicTo(94, 188, 94, 192, 96, 194)
  ..cubicTo(100, 198, 98, 202, 100, 204)
  ..cubicTo(96, 208, 96, 212, 98, 216)
  ..cubicTo(100, 226, 100, 232, 106, 240)
  ..cubicTo(118, 252, 136, 252, 150, 258)
  ..cubicTo(176, 256, 206, 248, 220, 238)
  ..cubicTo(244, 214, 252, 170, 250, 126)
  ..cubicTo(248, 70, 216, 34, 170, 34)
  ..close();

Path orejaDePerfil() => Path()
  ..moveTo(184, 126)
  ..cubicTo(168, 122, 164, 150, 174, 170)
  ..cubicTo(182, 184, 198, 178, 198, 162)
  ..cubicTo(198, 146, 194, 130, 184, 126)
  ..close();
