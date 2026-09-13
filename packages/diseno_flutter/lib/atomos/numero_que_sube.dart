import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Un número entero contando hacia arriba: puntos, experiencia, calificación.
///
/// Sigue las mismas reglas de seguridad que `MontoQueSube` —termina exacto, nunca
/// queda en el arranque, anuncia el valor final al lector de pantalla desde el primer
/// frame, no se reanima si el número no cambió y respeta «reducir movimiento»—, pero
/// además **cuenta desde el valor anterior**: en un puntaje, lo que la persona quiere
/// ver es que subió, y eso solo se ve si arranca de donde estaba.
class NumeroQueSube extends StatefulWidget {
  const NumeroQueSube({
    super.key,
    required this.valor,
    this.etiqueta,
    this.estilo,
    this.sufijo = '',
    this.duracion = const Duration(milliseconds: 650),
  });

  final int valor;

  /// Lo que el lector de pantalla dice antes del número («Tu puntaje»).
  final String? etiqueta;
  final TextStyle? estilo;

  /// Lo que va pegado al número: `' pts'`, `' %'`.
  final String sufijo;
  final Duration duracion;

  @override
  State<NumeroQueSube> createState() => _NumeroQueSubeState();
}

class _NumeroQueSubeState extends State<NumeroQueSube>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: widget.duracion,
  );
  late int _desde = 0;
  late int _hasta = widget.valor;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _arrancar();
  }

  @override
  void didUpdateWidget(NumeroQueSube anterior) {
    super.didUpdateWidget(anterior);
    if (anterior.valor == widget.valor) return;
    _desde = _visible;
    _hasta = widget.valor;
    _control.value = 0;
    _arrancar();
  }

  void _arrancar() {
    if (_desde == _hasta || MediaQuery.disableAnimationsOf(context)) {
      _control.value = 1;
      return;
    }
    _control.forward();
  }

  int get _visible {
    final t = Curves.easeOutCubic.transform(_control.value);
    return _desde + ((_hasta - _desde) * t).round();
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final base = widget.estilo ?? Theme.of(context).textTheme.displaySmall!;
    final etiqueta = widget.etiqueta;
    return Semantics(
      label: etiqueta == null
          ? '${widget.valor}${widget.sufijo}'
          : '$etiqueta: ${widget.valor}${widget.sufijo}',
      excludeSemantics: true,
      child: AnimatedBuilder(
        animation: _control,
        builder: (context, _) => Text(
          '$_visible${widget.sufijo}',
          style: base.copyWith(
            fontFamily: Fuente.display,
            fontFeatures: const [FontFeature.tabularFigures()],
          ),
        ),
      ),
    );
  }
}
