import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// El vale: cupón con muesca, QR rotativo, código corto, estado y condiciones (D-21).
class Vale extends StatelessWidget {
  const Vale({
    super.key,
    required this.comercio,
    required this.beneficio,
    required this.estado,
    required this.origen,
    required this.condiciones,
    this.codigoCorto,
    this.qr,
    this.tono = Tono.ok,
  });
  final String comercio;
  final String beneficio;
  final String estado;
  final String origen;
  final List<String> condiciones;
  final String? codigoCorto;
  final Widget? qr;
  final Tono tono;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return ClipPath(
      clipper: _Muesca(),
      child: Container(
        padding: const EdgeInsets.all(Espacio.s4),
        decoration: BoxDecoration(
          color: t.surface,
          borderRadius: BorderRadius.circular(Radios.lg),
          border: Border.all(color: t.border, width: Borde.fino),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    comercio,
                    style: texto.titleMedium?.copyWith(color: t.text),
                  ),
                ),
                ChipEstado(texto: estado, tono: tono),
              ],
            ),
            Text(
              beneficio,
              style: texto.headlineSmall?.copyWith(
                fontFamily: Fuente.display,
                color: t.accentTexto,
                fontWeight: FontWeight.w700,
              ),
            ),
            Text(
              'Lo pone $comercio, no sale de tu saldo · $origen',
              style: texto.bodySmall?.copyWith(color: t.text3),
            ),
            if (qr != null) ...[
              const SizedBox(height: Espacio.s3),
              Center(child: qr),
            ],
            if (codigoCorto != null) ...[
              const SizedBox(height: Espacio.s2),
              Center(
                child: Semantics(
                  label: 'Código corto $codigoCorto',
                  child: Text(
                    codigoCorto!,
                    style: texto.headlineSmall?.copyWith(
                      fontFamily: Fuente.display,
                      color: t.text,
                      letterSpacing: Espacio.s1,
                    ),
                  ),
                ),
              ),
            ],
            Divider(color: t.border, height: Espacio.s5),
            for (final c in condiciones)
              Text('· $c', style: texto.bodySmall?.copyWith(color: t.text2)),
          ],
        ),
      ),
    );
  }
}

class _Muesca extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final y = size.height * 0.62;
    const r = Espacio.s3;
    return Path()
      ..addRRect(
        RRect.fromRectAndRadius(
          Offset.zero & size,
          const Radius.circular(Radios.lg),
        ),
      )
      ..addOval(Rect.fromCircle(center: Offset(0, y), radius: r))
      ..addOval(Rect.fromCircle(center: Offset(size.width, y), radius: r))
      ..fillType = PathFillType.evenOdd;
  }

  @override
  bool shouldReclip(covariant CustomClipper<Path> old) => false;
}
