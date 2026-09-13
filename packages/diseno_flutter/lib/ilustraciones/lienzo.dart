import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// La base común de las ilustraciones: el escenario del fondo y los pinceles con los
/// que se dibuja encima.
///
/// **Se dibujan, no se importan.** Un dibujo en vector es nítido en cualquier pantalla
/// y a cualquier tamaño —no hay un «@3x» que se vea borroso en el teléfono siguiente—,
/// sigue el tema claro y oscuro sin exportar dos archivos, y no suma un kilobyte al
/// instalador.
///
/// Lo que hace que un dibujo así no parezca un ícono estirado es el volumen: degradado
/// en vez de color plano, sombra apoyada debajo, y un brillo arriba donde daría la luz.
/// Esos tres recursos están acá para que las cuatro ilustraciones los usen igual y se
/// vean de la misma familia.
class Lienzo {
  const Lienzo(this.canvas, this.medida, this.t);

  final Canvas canvas;
  final Size medida;
  final Tokens t;

  double get lado => medida.shortestSide;
  Offset get centro => Offset(medida.width / 2, medida.height / 2);

  /// Un valor proporcional al tamaño del dibujo, para que todo escale junto.
  double p(double fraccion) => lado * fraccion;

  /// El disco del fondo, con luz arriba a la izquierda: da peso a la ilustración sin
  /// encerrarla en una caja, y ya establece de dónde viene la luz.
  void escenario({double radio = 0.47}) {
    final r = p(radio);
    canvas.drawCircle(
      centro,
      r,
      Paint()
        ..shader = ui.Gradient.radial(
          centro - Offset(r * 0.35, r * 0.45),
          r * 1.5,
          [
            Color.lerp(t.brandBg, t.surface, 0.55)!,
            t.brandBg,
          ],
        ),
    );
  }

  Paint trazo(Color color, double grosor) => Paint()
    ..style = PaintingStyle.stroke
    ..strokeWidth = grosor
    ..strokeCap = StrokeCap.round
    ..strokeJoin = StrokeJoin.round
    ..color = color;

  Paint relleno(Color color) => Paint()..color = color;

  /// Relleno con volumen: el mismo color aclarado arriba y oscurecido abajo. Es lo que
  /// separa una forma con cuerpo de una mancha de color.
  ///
  /// Oscurece contra `Paleta.g900` y no contra negro: una sombra teñida del verde de
  /// la marca pertenece al dibujo; el negro puro lo ensucia.
  Paint volumen(Color color, Rect caja, {double fuerza = 0.22}) => Paint()
    ..shader = ui.Gradient.linear(
      caja.topLeft,
      caja.bottomRight,
      [
        Color.lerp(color, Paleta.white, fuerza)!,
        color,
        Color.lerp(color, Paleta.g900, fuerza * 0.7)!,
      ],
      [0, 0.55, 1],
    );

  /// La sombra que apoya un objeto sobre el escenario. Difusa y corta: es un dibujo,
  /// no una maqueta 3D.
  void sombra(Path forma, {double desplazamiento = 0.022, double difuso = 0.028}) {
    canvas.drawPath(
      forma.shift(Offset(0, p(desplazamiento))),
      Paint()
        ..color = t.brandInk.withValues(alpha: 0.16)
        ..maskFilter = MaskFilter.blur(BlurStyle.normal, p(difuso)),
    );
  }

  /// El brillo de la luz sobre una superficie curva.
  void brillo(Offset donde, double radio, {double fuerza = 0.55}) {
    canvas.drawCircle(
      donde,
      radio,
      Paint()
        ..shader = ui.Gradient.radial(donde, radio, [
          Paleta.white.withValues(alpha: fuerza),
          Paleta.white.withValues(alpha: 0),
        ]),
    );
  }

  /// Una persona vista de frente, reducida a lo mínimo que sigue leyéndose como
  /// alguien: cabeza y hombros, con su sombra debajo.
  void persona(Offset donde, double alto, Color color) {
    final cabeza = alto * 0.34;
    final centroCabeza = donde - Offset(0, alto * 0.30);
    final hombros = Rect.fromCenter(
      center: donde + Offset(0, alto * 0.30),
      width: alto * 0.94,
      height: alto * 0.68,
    );
    final silueta = Path()
      ..addOval(Rect.fromCircle(center: centroCabeza, radius: cabeza))
      ..addArc(hombros, 3.14159, 3.14159);
    sombra(silueta, desplazamiento: 0.012, difuso: 0.016);
    canvas
      ..drawCircle(
        centroCabeza,
        cabeza,
        volumen(color, Rect.fromCircle(center: centroCabeza, radius: cabeza)),
      )
      ..drawArc(hombros, 3.14159, 3.14159, false, volumen(color, hombros));
    brillo(centroCabeza - Offset(cabeza * 0.3, cabeza * 0.35), cabeza * 0.55, fuerza: 0.35);
  }
}

/// El envoltorio que todas las ilustraciones comparten: tamaño cuadrado, semántica
/// propia y el pintor que le toque.
class Ilustracion extends StatelessWidget {
  const Ilustracion({
    super.key,
    required this.pintor,
    required this.etiqueta,
    this.tamano = 160,
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
