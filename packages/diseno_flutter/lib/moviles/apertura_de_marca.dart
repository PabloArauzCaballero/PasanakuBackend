import 'package:flutter/material.dart';

import '../atomos/marca.dart';
import '../tokens/tokens.dart';

/// La apertura de la app: la marca se acerca a la cámara y el nombre aparece detrás.
///
/// El gesto es el de Netflix —arranca chica, se acerca acelerando y termina pasando de
/// largo apenas, mientras se funde—, pero con una regla que Netflix no tiene que
/// respetar: **esto está delante del dinero de alguien.** Por eso dura menos de un
/// segundo y medio, se puede saltar tocando la pantalla, y con «reducir movimiento»
/// del sistema no se muestra en absoluto. Si algo falla, [alTerminar] se llama igual:
/// una apertura rota no puede dejar a nadie sin entrar a su billetera.
class AperturaDeMarca extends StatefulWidget {
  const AperturaDeMarca({
    super.key,
    required this.alTerminar,
    this.duracion = const Duration(milliseconds: 1400),
  });

  final VoidCallback alTerminar;
  final Duration duracion;

  @override
  State<AperturaDeMarca> createState() => _AperturaDeMarcaState();
}

class _AperturaDeMarcaState extends State<AperturaDeMarca>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: widget.duracion,
  )..addStatusListener((estado) {
    if (estado == AnimationStatus.completed) _terminar();
  });

  bool _terminado = false;

  /// Idempotente a propósito: la puede llamar el final de la animación, el toque de
  /// quien la saltea, o el temporizador de respaldo. La primera gana y el resto no
  /// hace nada.
  void _terminar() {
    if (_terminado) return;
    _terminado = true;
    widget.alTerminar();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (MediaQuery.disableAnimationsOf(context)) {
      _terminar();
      return;
    }
    if (!_control.isAnimating && _control.value == 0) _control.forward();
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  // El acercamiento: casi quieta al principio, se lanza al final. Es lo que da la
  // sensación de que la marca viene hacia uno y no de que simplemente crece.
  static final _zoom = CurveTween(curve: Curves.easeInCubic);
  static final _desvanecer = CurveTween(curve: const Interval(0.72, 1));
  static final _nombre = CurveTween(
    curve: const Interval(0.35, 0.85, curve: Curves.easeOut),
  );

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: 'AportaYa',
      button: true,
      hint: 'Tocá para saltar la apertura',
      child: GestureDetector(
        onTap: _terminar,
        behavior: HitTestBehavior.opaque,
        // `Material` y no `ColoredBox`: esta pantalla va por encima de todo, fuera
        // del `Scaffold` de cualquier ruta, y un `Text` sin un `Material` arriba sale
        // con el subrayado amarillo de depuración de Flutter. Se veía en el video del
        // arranque, no en los goldens —ahí el texto son cajas—.
        child: Material(
          color: t.bg,
          child: Center(
            child: AnimatedBuilder(
              animation: _control,
              builder: (context, _) {
                final v = _control.value;
                final escala = 0.62 + _zoom.transform(v) * 2.1;
                final opacidad = 1 - _desvanecer.transform(v);
                return Opacity(
                  opacity: opacidad.clamp(0.0, 1.0),
                  child: Transform.scale(
                    scale: escala,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        // El trazo se escribe mientras se acerca: la marca llega
                        // dibujándose, no aparece ya hecha.
                        Marca(tamano: 96, avance: (v * 2.4).clamp(0.0, 1.0)),
                        const SizedBox(height: Espacio.s3),
                        Opacity(
                          opacity: _nombre.transform(v),
                          child: _Nombre(t: t),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

class _Nombre extends StatelessWidget {
  const _Nombre({required this.t});
  final Tokens t;

  @override
  Widget build(BuildContext context) => ExcludeSemantics(
    child: Text.rich(
      TextSpan(
        children: [
          TextSpan(text: 'Aporta', style: TextStyle(color: t.text)),
          TextSpan(text: 'Ya', style: TextStyle(color: t.accentTexto)),
        ],
      ),
      style: Tipo.titulo1,
    ),
  );
}
