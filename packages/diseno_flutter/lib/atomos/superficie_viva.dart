import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../tokens/tokens.dart';
import 'luz_de_boton.dart';
import 'movimiento.dart';

/// **La piel de un botón con relleno: profundidad, respuesta al dedo y un brillo.**
///
/// - **Profundidad**: un degradado vertical sutil del mismo color (apenas más claro
///   arriba, apenas más hondo abajo), un filo de luz de 1 px en el borde superior y una
///   sombra en dos capas teñida con el color del botón — de contacto y de ambiente.
/// - **Respuesta**: al apoyar el dedo se hunde un 3,5 % y la sombra se recoge hacia el
///   botón, como algo que baja; al soltar vuelve con un resorte que apenas se pasa.
///   Con mouse, al pasar por encima se eleva un poco.
/// - **Brillo**: si [brilla], una franja de luz cruza el botón en diagonal al aparecer
///   y unas pocas veces más, espaciadas. Mientras [cargando], cruza seguido: es lo que
///   dice «estoy trabajando» sin depender solo del girador.
///
/// Apagado no tiene nada de esto: ni degradado, ni sombra, ni brillo. Con «reducir
/// movimiento», la profundidad queda y el movimiento se va.
class SuperficieViva extends StatefulWidget {
  const SuperficieViva({
    super.key,
    required this.color,
    required this.radio,
    required this.child,
    this.activo = true,
    this.brilla = false,
    this.cargando = false,
  });

  final Color color;
  final double radio;
  final bool activo;
  final bool brilla;
  final bool cargando;
  final Widget child;

  /// Cuántas veces cruza el brillo en reposo después de la primera. Finito a
  /// propósito: un destello eterno gasta batería y deja de decir algo al tercer ciclo.
  static const vecesDelBrillo = 3;

  @override
  State<SuperficieViva> createState() => _SuperficieVivaState();
}

class _SuperficieVivaState extends State<SuperficieViva>
    with TickerProviderStateMixin {
  late final AnimationController _hundido = AnimationController.unbounded(
    vsync: this,
  );
  late final AnimationController _brillo = AnimationController(
    vsync: this,
    duration: Movimiento.brillo + Movimiento.pausaDelBrillo,
  );
  bool _encima = false;
  bool _quieto = false;

  static const _cuanto = 0.035;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _quieto = MediaQuery.disableAnimationsOf(context);
    _ajustarBrillo();
  }

  @override
  void didUpdateWidget(SuperficieViva viejo) {
    super.didUpdateWidget(viejo);
    if (viejo.cargando != widget.cargando ||
        viejo.brilla != widget.brilla ||
        viejo.activo != widget.activo) {
      _ajustarBrillo();
    }
  }

  void _ajustarBrillo() {
    if (_quieto || !(widget.brilla || widget.cargando)) {
      _brillo.stop();
      _brillo.value = 0;
    } else if (widget.cargando) {
      _brillo.repeat(period: Movimiento.brillo);
    } else if (widget.activo) {
      _brillo.repeat(count: SuperficieViva.vecesDelBrillo + 1);
    }
  }

  void _apretar() {
    if (!widget.activo || _quieto) return;
    _hundido.animateTo(1, duration: Movimiento.micro, curve: Curves.easeOut);
  }

  void _soltar() {
    if (_quieto) return;
    _hundido.animateWith(
      SpringSimulation(Movimiento.resorte, _hundido.value, 0, -3),
    );
  }

  @override
  void dispose() {
    _hundido.dispose();
    _brillo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final c = widget.color;
    final forma = BorderRadius.circular(widget.radio);
    return MouseRegion(
      onEnter: (_) => setState(() => _encima = true),
      onExit: (_) => setState(() => _encima = false),
      child: Listener(
        onPointerDown: (_) => _apretar(),
        onPointerUp: (_) => _soltar(),
        onPointerCancel: (_) => _soltar(),
        child: AnimatedBuilder(
          animation: Listenable.merge([_hundido, _brillo]),
          child: widget.child,
          builder: (context, hijo) {
            final h = _hundido.value.clamp(0.0, 1.0);
            final alto = widget.activo ? (_encima ? 1.25 : 1.0) * (1 - h) : 0.0;
            return Transform.scale(
              scale: 1 - _cuanto * _hundido.value,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: forma,
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color.lerp(c, Paleta.white, 0.10)!,
                      Color.lerp(c, Paleta.ink, 0.10)!,
                    ],
                  ),
                  boxShadow: [
                    BoxShadow(
                      offset: Offset(0, 1 + alto),
                      blurRadius: 2 + 2 * alto,
                      color: c.withValues(alpha: 0.30 * (alto > 0 ? 1 : 0)),
                    ),
                    BoxShadow(
                      offset: Offset(0, 8 * alto),
                      blurRadius: 22 * alto,
                      spreadRadius: -4,
                      color: c.withValues(alpha: 0.38 * alto.clamp(0.0, 1.0)),
                    ),
                    if (alto == 0) t.sombra1,
                  ],
                ),
                child: ClipRRect(
                  borderRadius: forma,
                  child: CustomPaint(
                    foregroundPainter: LuzDeBoton(
                      avance: _avanceDelBrillo(),
                      filo: Paleta.white,
                    ),
                    child: hijo,
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  /// Fracción del cruce del brillo dentro del ciclo, o `null` en la pausa.
  double? _avanceDelBrillo() {
    if (!_brillo.isAnimating) return null;
    if (widget.cargando) return _brillo.value;
    final parte =
        Movimiento.brillo.inMilliseconds /
        (Movimiento.brillo + Movimiento.pausaDelBrillo).inMilliseconds;
    final v = _brillo.value / parte;
    return v > 1 ? null : Movimiento.pareja.transform(v);
  }
}
