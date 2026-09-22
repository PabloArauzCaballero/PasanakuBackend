import 'package:flutter/widgets.dart';

import '../tokens/tokens.dart';

/// Aire alrededor del elemento, para que el anillo no lo toque.
const double _holgura = 6;

/// **El velo con un agujero.** Oscurece la pantalla salvo el elemento del que habla el
/// tutorial, y dibuja un anillo alrededor: el foco no se comunica solo con la sombra,
/// porque el oscurecimiento solo no se ve en una pantalla al sol ni con poca vista.
///
/// El agujero **deja pasar el toque** salvo que [bloquea] sea verdadero: un paso que
/// dice «tocá este botón» tiene que dejar tocarlo de verdad.
class FocoDeTutorial extends StatelessWidget {
  const FocoDeTutorial({
    super.key,
    required this.recuadro,
    this.bloquea = false,
    this.alTocarFuera,
  });

  /// `null` mientras se busca el elemento o cuando el paso no señala nada: el velo se
  /// cierra entero y el globo habla de la pantalla.
  final Rect? recuadro;
  final bool bloquea;
  final VoidCallback? alTocarFuera;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final quieto = MediaQuery.disableAnimationsOf(context);
    final actual = recuadro;
    final pintura = actual == null
        // Sin recuadro no hay nada que animar —y `RectTween` sin destino revienta—:
        // el velo se cierra entero y el globo habla de la pantalla.
        ? CustomPaint(
            painter: _PintorDeFoco(
              recuadro: null,
              velo: Paleta.ink,
              anillo: t.accent,
            ),
            size: Size.infinite,
          )
        : TweenAnimationBuilder<Rect?>(
            tween: RectTween(end: actual),
            duration: quieto
                ? Duration.zero
                : const Duration(milliseconds: 220),
            curve: Curves.easeOutCubic,
            builder: (context, animado, _) => CustomPaint(
              painter: _PintorDeFoco(
                recuadro: animado ?? actual,
                velo: Paleta.ink,
                anillo: t.accent,
              ),
              size: Size.infinite,
            ),
          );
    // Con el hueco tocable, el velo no puede comerse los toques de la app: se ignora
    // el puntero y el agujero queda vivo porque no hay nada encima de él.
    return ExcludeSemantics(
      child: IgnorePointer(
        ignoring: !bloquea,
        child: GestureDetector(
          onTap: alTocarFuera,
          behavior: HitTestBehavior.opaque,
          child: pintura,
        ),
      ),
    );
  }
}

class _PintorDeFoco extends CustomPainter {
  const _PintorDeFoco({
    required this.recuadro,
    required this.velo,
    required this.anillo,
  });

  final Rect? recuadro;
  final Color velo;
  final Color anillo;

  @override
  void paint(Canvas lienzo, Size medida) {
    final pantalla = Offset.zero & medida;
    final pintura = Paint()..color = velo.withValues(alpha: 0.62);
    if (recuadro == null) {
      lienzo.drawRect(pantalla, pintura);
      return;
    }
    final hueco = RRect.fromRectAndRadius(
      recuadro!.inflate(_holgura),
      const Radius.circular(Radios.lg),
    );
    final conAgujero = Path.combine(
      PathOperation.difference,
      Path()..addRect(pantalla),
      Path()..addRRect(hueco),
    );
    lienzo.drawPath(conAgujero, pintura);
    lienzo.drawRRect(
      hueco,
      Paint()
        ..color = anillo
        ..style = PaintingStyle.stroke
        ..strokeWidth = Borde.foco,
    );
  }

  @override
  bool shouldRepaint(_PintorDeFoco anterior) =>
      anterior.recuadro != recuadro ||
      anterior.velo != velo ||
      anterior.anillo != anillo;
}
