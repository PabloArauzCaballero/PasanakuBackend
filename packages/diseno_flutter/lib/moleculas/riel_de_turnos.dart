import 'package:flutter/material.dart';

import '../atomos/movimiento.dart';
import '../tokens/tokens.dart';

/// El orden sorteado con tu lugar marcado y cuánto falta (D-14).
class RielDeTurnos extends StatelessWidget {
  const RielDeTurnos({
    super.key,
    required this.total,
    required this.actual,
    required this.mio,
    this.nombres = const {},
  });
  final int total;

  /// El turno que se está cobrando ahora (1..total).
  final int actual;
  final int mio;
  final Map<int, String> nombres;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final faltan = mio - actual;
    return Semantics(
      label: faltan <= 0
          ? 'Tu turno es el $mio: ya te tocó o te toca ahora'
          : 'Tu turno es el $mio de $total. Faltan $faltan',
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (var i = 1; i <= total; i += 1) ...[
                  _PulsoDeTurno(
                    key: ValueKey('turno-$i'),
                    activo: i == actual,
                    child: Container(
                      width: Espacio.s6,
                      height: Espacio.s6,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: i == mio
                            ? t.accent
                            : i <= actual
                            ? t.verdeSolido
                            : t.surface2,
                        border: Border.all(
                          color: i == mio
                              ? t.accent
                              : i <= actual
                              ? t.verdeSolido
                              : t.border,
                          width: Borde.fino,
                        ),
                      ),
                      child: Text(
                        '$i',
                        style: texto.labelMedium?.copyWith(
                          color: i == mio
                              ? t.accentInk
                              : i <= actual
                              ? t.sobreVerdeSolido
                              : t.text2,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                  ),
                  if (i < total)
                    Container(
                      width: Espacio.s3,
                      height: Borde.foco - Borde.fino,
                      color: i < actual ? t.verdeSolido : t.border,
                    ),
                ],
              ],
            ),
          ),
          const SizedBox(height: Espacio.s2),
          Text(
            faltan <= 0
                ? 'Turno $mio de $total · te toca ahora'
                : 'Turno $mio de $total · faltan $faltan',
            style: texto.bodyMedium?.copyWith(color: t.text2),
          ),
        ],
      ),
    );
  }
}

class _PulsoDeTurno extends StatefulWidget {
  const _PulsoDeTurno({super.key, required this.activo, required this.child});

  final bool activo;
  final Widget child;

  @override
  State<_PulsoDeTurno> createState() => _PulsoDeTurnoState();
}

class _PulsoDeTurnoState extends State<_PulsoDeTurno>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 800),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _actualizarPulso();
  }

  @override
  void didUpdateWidget(covariant _PulsoDeTurno oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.activo != widget.activo) _actualizarPulso();
  }

  void _actualizarPulso() {
    if (!widget.activo || reducirMovimiento(context)) {
      _control.stop();
      _control.value = 1;
    } else if (!_control.isAnimating) {
      _control.value = 0;
      _control.repeat(reverse: true);
    }
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(
    opacity: Tween<double>(begin: 0.6, end: 1).animate(_control),
    child: widget.child,
  );
}
