import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../tokens/tokens.dart';
import 'luz_de_boton.dart';
import 'movimiento.dart';

/// **Piel de botón con relleno**: superficie plana, respuesta sutil al dedo y brillo
/// finito sólo si se pide. El movimiento se suprime si el usuario prefiere
/// reducirlo. En carga, el brillo comunica actividad.
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
  bool _quieto = false;
  Timer? _brilloEnEspera;

  static const _escalaPresion = 0.98;

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
    return RepaintBoundary(
      child: Listener(
        onPointerDown: (_) => _apretar(),
        onPointerUp: (_) => _soltar(),
        onPointerCancel: (_) => _soltar(),
        child: AnimatedBuilder(
          animation: Listenable.merge([_hundido, _brillo]),
          child: widget.child,
          builder: (context, hijo) {
            // Plano, como una app bancaria: relleno sólido y una sombra mínima. Sin
            // degradado ni halo del color del botón —eso lo hacía de videojuego—.
            return Transform.scale(
              scale: 1 - (1 - _escalaPresion) * _hundido.value,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: forma,
                  color: c,
                  boxShadow: widget.activo ? [t.sombra1] : null,
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
