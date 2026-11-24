import 'dart:async';

import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El punto que late mientras se espera al banco: un anillo que crece y se apaga, una vez cada
/// `Movimiento.brillo`, con una pausa del mismo largo entre latido y latido.
///
/// Es una de las pocas cosas que se mueven solas en la app (`pasanaku-movimiento` §1: una
/// espera real es excepción legítima), y por eso es chico, lento y se apaga del todo con
/// movimiento reducido: queda el punto quieto, que sigue diciendo lo mismo junto a su texto.
/// El latido es finito y se re-arma con un `Timer` —como el brillo de `SuperficieViva`—: un
/// `repeat()` sin fin no dejaría asentarse nunca a la pantalla.
class LatidoDeEspera extends StatefulWidget {
  const LatidoDeEspera({super.key, required this.color});
  final Color color;

  @override
  State<LatidoDeEspera> createState() => _LatidoDeEsperaState();
}

class _LatidoDeEsperaState extends State<LatidoDeEspera>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: Movimiento.brillo,
  )..addStatusListener(_alTerminar);
  Timer? _pausa;
  bool _quieto = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _quieto = reducirMovimiento(context);
    if (_quieto) {
      _pausa?.cancel();
      _pausa = null;
      _control
        ..stop()
        ..value = 0;
    } else if (!_control.isAnimating && _pausa == null) {
      _control.forward(from: 0);
    }
  }

  void _alTerminar(AnimationStatus estado) {
    if (estado != AnimationStatus.completed || _quieto) return;
    _pausa = Timer(Movimiento.brillo, () {
      _pausa = null;
      if (mounted && !_quieto) _control.forward(from: 0);
    });
  }

  @override
  void dispose() {
    _pausa?.cancel();
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final punto = DecoratedBox(
      decoration: BoxDecoration(color: widget.color, shape: BoxShape.circle),
      child: const SizedBox.square(dimension: Espacio.s2 + Espacio.s1 / 2),
    );
    return ExcludeSemantics(
      child: RepaintBoundary(
        child: SizedBox.square(
          dimension: Espacio.s5,
          child: Stack(
            alignment: Alignment.center,
            children: [
              AnimatedBuilder(
                animation: _control,
                builder: (context, hijo) {
                  final v = Movimiento.llega.transform(_control.value);
                  return Opacity(
                    opacity: _control.isAnimating ? (1 - v) * 0.5 : 0,
                    child: Transform.scale(scale: 1 + v * 1.6, child: hijo),
                  );
                },
                child: punto,
              ),
              punto,
            ],
          ),
        ),
      ),
    );
  }
}
