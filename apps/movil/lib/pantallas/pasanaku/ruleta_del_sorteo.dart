import 'dart:math' as math;

import 'package:aportaya_diseno/atomos/colores_de_turno.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_detalle_del_grupo.dart';

/// La ruleta del sorteo (2026-10-20, la de la maqueta): un segmento por integrante, en
/// el orden de los cupos, con el color de la rueda del grupo. La aguja queda arriba y
/// la ruleta gira [angulo] radianes. **No decide nada**: el orden lo devolvió el
/// servidor; la ruleta solo lo muestra, persona por persona.
class RuletaDelSorteo extends StatelessWidget {
  const RuletaDelSorteo({
    super.key,
    required this.nombres,
    required this.angulo,
    this.diametro = 280,
  });

  final List<String> nombres;
  final double angulo;
  final double diametro;

  /// El giro que deja el segmento [indice] bajo la aguja, en la vuelta que sigue a
  /// [desde] más [vueltas] vueltas completas.
  static double anguloPara(
    int indice,
    int segmentos, {
    required double desde,
    int vueltas = 3,
  }) {
    final paso = 2 * math.pi / segmentos;
    final destino = -(indice + 0.5) * paso;
    var delta = (destino - desde) % (2 * math.pi);
    if (delta < 0) delta += 2 * math.pi;
    return desde + vueltas * 2 * math.pi + delta;
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: TextosDetalleDelGrupo.ruletaEtiqueta,
      image: true,
      child: SizedBox.square(
        dimension: diametro,
        child: Stack(
          alignment: Alignment.center,
          children: [
            Transform.rotate(
              angle: angulo,
              child: CustomPaint(
                size: Size.square(diametro),
                painter: _PintorDeRuleta(
                  nombres: nombres,
                  borde: t.bg,
                  estilo: Tipo.etiqueta.copyWith(color: Paleta.ink),
                ),
              ),
            ),
            Container(
              width: diametro * 0.26,
              height: diametro * 0.26,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: t.surface,
                shape: BoxShape.circle,
                border: Border.all(color: t.border, width: Borde.fino),
              ),
              child: Text(
                'TURNO',
                style: Tipo.etiqueta.copyWith(color: t.text2),
              ),
            ),
            Positioned(
              top: 0,
              child: CustomPaint(
                size: const Size(Espacio.s5, Espacio.s5),
                painter: _PintorDeAguja(t.text),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _PintorDeRuleta extends CustomPainter {
  _PintorDeRuleta({
    required this.nombres,
    required this.borde,
    required this.estilo,
  });
  final List<String> nombres;
  final Color borde;
  final TextStyle estilo;

  @override
  void paint(Canvas canvas, Size size) {
    final n = nombres.length;
    if (n == 0) return;
    final centro = size.center(Offset.zero);
    final radio = size.width / 2;
    final paso = 2 * math.pi / n;
    final rect = Rect.fromCircle(center: centro, radius: radio);
    for (var i = 0; i < n; i++) {
      // El segmento 0 empieza en la aguja (arriba) y siguen en el sentido del reloj.
      final inicio = -math.pi / 2 + i * paso;
      canvas.drawArc(
        rect,
        inicio,
        paso,
        true,
        Paint()..color = ColoresDeTurno.de(i),
      );
      canvas.drawArc(
        rect,
        inicio,
        paso,
        true,
        Paint()
          ..color = borde
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2,
      );
      final nombre = nombres[i].split(' ').first;
      final texto = TextPainter(
        text: TextSpan(text: nombre, style: estilo),
        textDirection: TextDirection.ltr,
        maxLines: 1,
        ellipsis: '…',
      )..layout(maxWidth: radio * 0.55);
      canvas.save();
      canvas.translate(centro.dx, centro.dy);
      canvas.rotate(inicio + paso / 2);
      texto.paint(canvas, Offset(radio * 0.36, -texto.height / 2));
      canvas.restore();
    }
  }

  @override
  bool shouldRepaint(_PintorDeRuleta viejo) =>
      viejo.nombres != nombres || viejo.borde != borde;
}

class _PintorDeAguja extends CustomPainter {
  _PintorDeAguja(this.color);
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final camino = Path()
      ..moveTo(0, 0)
      ..lineTo(size.width, 0)
      ..lineTo(size.width / 2, size.height)
      ..close();
    canvas.drawPath(camino, Paint()..color = color);
  }

  @override
  bool shouldRepaint(_PintorDeAguja viejo) => viejo.color != color;
}
