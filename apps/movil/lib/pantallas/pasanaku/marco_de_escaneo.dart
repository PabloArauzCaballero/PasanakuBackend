import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El marco que va encima de la cámara al escanear: oscurece lo que queda afuera y
/// marca con cuatro esquinas dónde tiene que quedar el QR. Al abrirse, las esquinas se
/// cierran sobre el hueco (con movimiento reducido aparecen quietas). Si lo leído no es
/// una invitación, [rechazado] las pinta de rojo junto con el aviso de texto: el color
/// nunca es la única señal.
///
/// No anima en bucle: la cámara ya es lo que se mueve, y la pantalla se asienta.
class MarcoDeEscaneo extends StatelessWidget {
  const MarcoDeEscaneo({
    super.key,
    required this.pista,
    this.rechazado = false,
  });

  final String pista;
  final bool rechazado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = rechazado ? t.err : t.camaraTinta;
    return Stack(
      fit: StackFit.expand,
      children: [
        TweenAnimationBuilder<double>(
          tween: Tween(begin: 1, end: 0),
          duration: duracion(context, Movimiento.celebracionDeAlta),
          curve: Movimiento.llega,
          builder: (context, apertura, _) => TweenAnimationBuilder<Color?>(
            tween: ColorTween(end: color),
            duration: duracion(context, Movimiento.cambioDeEstado),
            builder: (context, tinta, _) => CustomPaint(
              painter: _PintorDeMarco(
                velo: t.camaraVelo,
                tinta: tinta ?? color,
                apertura: apertura,
              ),
            ),
          ),
        ),
        Align(
          alignment: Alignment.bottomCenter,
          child: Padding(
            padding: const EdgeInsets.all(Espacio.s3),
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: t.camaraVelo,
                borderRadius: BorderRadius.circular(Radios.pill),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: Espacio.s3,
                  vertical: Espacio.s2,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.qr_code_scanner_rounded,
                      size: Espacio.s4 + Espacio.s1,
                      color: t.camaraTinta,
                    ),
                    const SizedBox(width: Espacio.s2),
                    Flexible(
                      child: Text(
                        pista,
                        textAlign: TextAlign.center,
                        style: Tipo.cuerpoChico.copyWith(color: t.camaraTinta),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _PintorDeMarco extends CustomPainter {
  const _PintorDeMarco({
    required this.velo,
    required this.tinta,
    required this.apertura,
  });

  final Color velo;
  final Color tinta;

  /// 1 al abrir (esquinas afuera, sobre el borde del visor), 0 en reposo.
  final double apertura;

  @override
  void paint(Canvas canvas, Size size) {
    final lado = size.shortestSide * 0.64;
    final hueco = Rect.fromCenter(
      center: size.center(Offset.zero).translate(0, -size.height * 0.04),
      width: lado,
      height: lado,
    );
    final radio = lado * 0.09;
    canvas.drawPath(
      Path.combine(
        PathOperation.difference,
        Path()..addRect(Offset.zero & size),
        Path()
          ..addRRect(RRect.fromRectAndRadius(hueco, Radius.circular(radio))),
      ),
      Paint()..color = velo,
    );
    final marco = hueco.inflate(lado * 0.08 * apertura);
    final brazo = lado * 0.2;
    final trazo = Paint()
      ..color = tinta
      ..style = PaintingStyle.stroke
      ..strokeWidth = Espacio.s1 + 1
      ..strokeCap = StrokeCap.round;
    for (final (esquina, dx, dy) in [
      (marco.topLeft, 1.0, 1.0),
      (marco.topRight, -1.0, 1.0),
      (marco.bottomLeft, 1.0, -1.0),
      (marco.bottomRight, -1.0, -1.0),
    ]) {
      canvas.drawPath(
        Path()
          ..moveTo(esquina.dx, esquina.dy + dy * brazo)
          ..lineTo(esquina.dx, esquina.dy + dy * radio)
          ..quadraticBezierTo(
            esquina.dx,
            esquina.dy,
            esquina.dx + dx * radio,
            esquina.dy,
          )
          ..lineTo(esquina.dx + dx * brazo, esquina.dy),
        trazo,
      );
    }
  }

  @override
  bool shouldRepaint(_PintorDeMarco viejo) =>
      viejo.velo != velo || viejo.tinta != tinta || viejo.apertura != apertura;
}
