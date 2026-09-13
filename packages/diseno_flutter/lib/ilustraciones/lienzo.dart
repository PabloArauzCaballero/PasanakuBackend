import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La base común de las ilustraciones: un escenario circular suave detrás y los
/// pinceles con los que se dibuja encima.
///
/// **Se dibujan, no se importan.** Una ilustración en vector es nítida en cualquier
/// pantalla y a cualquier tamaño —no hay «@3x» que se vea borroso en el teléfono
/// siguiente—, sigue el tema claro y oscuro sin exportar dos archivos, y no suma un
/// solo kilobyte al instalador. Un PNG «full HD» de 1920 px para un dibujo de 140 px
/// pesa cien veces más y se ve peor.
///
/// El lenguaje visual es el de la marca: trazo redondeado, verde para la estructura,
/// naranja para lo que importa. Nada de relleno plano.
class Lienzo {
  const Lienzo(this.canvas, this.medida, this.t);

  final Canvas canvas;
  final Size medida;
  final Tokens t;

  double get lado => medida.shortestSide;
  Offset get centro => Offset(medida.width / 2, medida.height / 2);

  /// Un valor proporcional al tamaño del dibujo, para que todo escale junto.
  double p(double fraccion) => lado * fraccion;

  /// El disco suave del fondo: da peso a la ilustración sin encerrarla en una caja.
  void escenario({double radio = 0.46}) {
    canvas.drawCircle(centro, p(radio), Paint()..color = t.brandBg);
  }

  Paint trazo(Color color, double grosor) => Paint()
    ..style = PaintingStyle.stroke
    ..strokeWidth = grosor
    ..strokeCap = StrokeCap.round
    ..strokeJoin = StrokeJoin.round
    ..color = color;

  Paint relleno(Color color) => Paint()..color = color;

  /// Una persona vista de frente, reducida a lo mínimo que sigue leyéndose como
  /// alguien: cabeza y hombros.
  void persona(Offset donde, double alto, Color color) {
    final cabeza = alto * 0.34;
    canvas.drawCircle(
      donde - Offset(0, alto * 0.28),
      cabeza,
      relleno(color),
    );
    final hombros = Rect.fromCenter(
      center: donde + Offset(0, alto * 0.30),
      width: alto * 0.92,
      height: alto * 0.66,
    );
    canvas.drawArc(hombros, 3.14159, 3.14159, false, relleno(color));
  }
}

/// El envoltorio que todas las ilustraciones comparten: tamaño cuadrado, semántica
/// propia y el pintor que le toque.
class Ilustracion extends StatelessWidget {
  const Ilustracion({
    super.key,
    required this.pintor,
    required this.etiqueta,
    this.tamano = 148,
  });

  final CustomPainter Function(Tokens) pintor;
  final String etiqueta;
  final double tamano;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta,
      image: true,
      child: SizedBox.square(
        dimension: tamano,
        child: CustomPaint(painter: pintor(t)),
      ),
    );
  }
}
