import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../tokens/tokens.dart';
import 'luz_de_boton.dart';
import 'movimiento.dart';

/// **Piel de botón con relleno**: profundidad, respuesta al dedo y brillo finito.
/// El degradado conserva el contraste del texto; el movimiento se suprime si el
/// usuario prefiere reducirlo. En carga, el brillo comunica actividad.
class SuperficieViva extends StatefulWidget {
  const SuperficieViva({
    super.key,
    required this.color,
    required this.colorDelTexto,
    required this.radio,
    required this.child,
    this.activo = true,
    this.brilla = false,
    this.cargando = false,
  });

  final Color color;
  final Color colorDelTexto;
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
  Timer? _brilloEnEspera;

  static const _escalaPresion = 0.97;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _quieto = reducirMovimiento(context);
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
    if (_quieto || !widget.activo || !(widget.brilla || widget.cargando)) {
      _brilloEnEspera?.cancel();
      _brilloEnEspera = null;
      _brillo.stop();
      _brillo.value = 0;
    } else if (widget.cargando) {
      _brilloEnEspera?.cancel();
      _brilloEnEspera = null;
      _brillo.repeat(period: Movimiento.brillo);
    } else if (widget.activo) {
      if (_brillo.isAnimating ||
          _brillo.value != 0 ||
          _brilloEnEspera != null) {
        return;
      }
      _brilloEnEspera = Timer(Movimiento.pausaDelBrillo, () {
        _brilloEnEspera = null;
        if (!mounted || _quieto || !widget.activo || !widget.brilla) return;
        _brillo.repeat(count: SuperficieViva.vecesDelBrillo + 1);
      });
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
    _brilloEnEspera?.cancel();
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
            final direccionDelDegradado =
                widget.colorDelTexto.computeLuminance() > c.computeLuminance()
                ? Paleta.ink
                : Paleta.white;
            return Transform.scale(
              scale: 1 - (1 - _escalaPresion) * _hundido.value,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: forma,
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Color.lerp(c, direccionDelDegradado, 0.06)!,
                      Color.lerp(c, direccionDelDegradado, 0.10)!,
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
