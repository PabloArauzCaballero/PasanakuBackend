import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../dominio/pautas_de_fotos.dart';

/// Ilustración geométrica original: no contiene ni depende de fotos personales.
class IlustracionDePauta extends StatelessWidget {
  const IlustracionDePauta({super.key, required this.escena});

  final EscenaDePauta escena;

  @override
  Widget build(BuildContext context) => AspectRatio(
    aspectRatio: 2,
    child: ExcludeSemantics(
      child: CustomPaint(painter: _PintorDePauta(escena, Tokens.of(context))),
    ),
  );
}

class _PintorDePauta extends CustomPainter {
  const _PintorDePauta(this.escena, this.t);

  final EscenaDePauta escena;
  final Tokens t;

  @override
  void paint(Canvas canvas, Size size) {
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Offset.zero & size,
        const Radius.circular(Radios.md),
      ),
      Paint()..color = t.surface2,
    );
    if (escena.name.startsWith('carnet')) {
      _pintarCarnet(canvas, size);
    } else {
      _pintarRostro(canvas, size);
    }
  }

  void _pintarCarnet(Canvas canvas, Size size) {
    final inclinado = escena == EscenaDePauta.carnetInclinado;
    canvas.save();
    canvas.translate(size.width / 2, size.height / 2);
    if (inclinado) canvas.rotate(-0.24);
    final caja = Rect.fromCenter(
      center: Offset.zero,
      width: size.width * 0.70,
      height: size.height * 0.62,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(caja, const Radius.circular(Radios.md)),
      Paint()..color = t.surface,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(caja, const Radius.circular(Radios.md)),
      Paint()
        ..color = t.text.withValues(alpha: 0.55)
        ..style = PaintingStyle.stroke
        ..strokeWidth = Borde.fino,
    );
    final foto = Rect.fromLTWH(
      caja.left + 8,
      caja.top + 8,
      caja.height * 0.43,
      caja.height - 16,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(foto, const Radius.circular(Radios.sm)),
      Paint()..color = t.surface2,
    );
    canvas.drawCircle(
      Offset(foto.center.dx, foto.top + foto.height * 0.34),
      foto.width * 0.20,
      Paint()..color = t.text2,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromCenter(
          center: Offset(foto.center.dx, foto.bottom - 12),
          width: foto.width * 0.62,
          height: 16,
        ),
        const Radius.circular(Radios.sm),
      ),
      Paint()..color = t.text2,
    );
    final textoX = foto.right + 7;
    for (var i = 0; i < 3; i++) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(
            textoX,
            caja.top + 14 + i * 12,
            caja.right - textoX - 8,
            4,
          ),
          const Radius.circular(Radios.sm),
        ),
        Paint()..color = t.text.withValues(alpha: i == 0 ? 0.7 : 0.35),
      );
    }
    canvas.restore();

    if (escena == EscenaDePauta.carnetOscuro) {
      canvas.drawRect(
        Offset.zero & size,
        Paint()..color = t.text.withValues(alpha: 0.6),
      );
    } else if (escena == EscenaDePauta.carnetReflejo) {
      final banda = Path()
        ..moveTo(size.width * 0.57, 0)
        ..lineTo(size.width * 0.78, 0)
        ..lineTo(size.width * 0.48, size.height)
        ..lineTo(size.width * 0.28, size.height)
        ..close();
      canvas.drawPath(
        banda,
        Paint()..color = t.surface.withValues(alpha: 0.75),
      );
    } else if (escena == EscenaDePauta.carnetBorroso) {
      for (var i = 0; i < 3; i++) {
        canvas.drawOval(
          Rect.fromCenter(
            center: Offset(size.width * (0.4 + i * 0.1), size.height / 2),
            width: 30,
            height: 18,
          ),
          Paint()..color = t.surface.withValues(alpha: 0.4),
        );
      }
    }
  }

  void _pintarRostro(Canvas canvas, Size size) {
    final ladeado = escena == EscenaDePauta.rostroLadeado;
    canvas.save();
    canvas.translate(
      ladeado ? size.width * 0.61 : size.width / 2,
      size.height / 2,
    );
    if (ladeado) canvas.rotate(0.20);
    final cabeza = Rect.fromCenter(
      center: Offset.zero,
      width: size.height * 0.46,
      height: size.height * 0.63,
    );
    canvas.drawOval(cabeza, Paint()..color = t.accent);
    canvas.drawArc(
      Rect.fromCenter(
        center: Offset(0, cabeza.top + 8),
        width: cabeza.width * 1.08,
        height: cabeza.height * 0.52,
      ),
      3.14,
      3.14,
      true,
      Paint()..color = t.text,
    );
    canvas.drawCircle(const Offset(-8, -2), 2.5, Paint()..color = t.text);
    canvas.drawCircle(const Offset(8, -2), 2.5, Paint()..color = t.text);
    canvas.drawLine(
      const Offset(-5, 13),
      const Offset(5, 13),
      Paint()
        ..color = t.text2
        ..strokeWidth = 2
        ..strokeCap = StrokeCap.round,
    );
    if (escena == EscenaDePauta.rostroConAccesorios) {
      final lentes = Paint()
        ..color = t.text
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4;
      canvas.drawOval(const Rect.fromLTRB(-18, -8, -1, 5), lentes);
      canvas.drawOval(const Rect.fromLTRB(1, -8, 18, 5), lentes);
      canvas.drawLine(const Offset(-1, -2), const Offset(1, -2), lentes);
    }
    canvas.restore();

    if (escena == EscenaDePauta.rostroContraluz) {
      canvas.drawCircle(
        Offset(size.width * 0.84, size.height * 0.30),
        16,
        Paint()..color = t.accent,
      );
      canvas.drawRect(
        Offset.zero & size,
        Paint()..color = t.text.withValues(alpha: 0.35),
      );
    }
  }

  @override
  bool shouldRepaint(_PintorDePauta oldDelegate) =>
      oldDelegate.escena != escena || oldDelegate.t != t;
}
