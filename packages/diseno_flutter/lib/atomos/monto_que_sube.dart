import 'package:flutter/material.dart';

import '../dinero/formatear.dart';
import '../tokens/tokens.dart';

/// El importe contando hacia arriba: de cero al saldo la primera vez, y del valor
/// anterior al nuevo cuando cambia.
///
/// **Esto es dinero, así que la animación no puede hacer dudar a nadie.** Las reglas
/// que la hacen segura, todas probadas en `monto_que_sube_test.dart`:
///
/// - **Nunca queda en cero.** El estado en reposo es siempre el importe real: si el
///   widget se destruye, si el sistema mata las animaciones o si algo falla a mitad de
///   camino, lo que queda en pantalla es el saldo, no el arranque de la cuenta.
/// - **Es corta.** Medio segundo largo. Nadie tiene que esperar para saber cuánto tiene.
/// - **El lector de pantalla anuncia el valor final desde el primer frame**, nunca los
///   intermedios: quien escucha recibe su saldo, no un conteo.
/// - **Si el valor no cambió, no se reanima.** Refrescar la pantalla y que el saldo sea
///   el mismo no lo manda de vuelta a cero.
/// - **Respeta «reducir movimiento».** Con esa opción del sistema activada, el importe
///   aparece directamente en su valor final.
class MontoQueSube extends StatefulWidget {
  const MontoQueSube({
    super.key,
    required this.monto,
    required this.moneda,
    this.etiqueta,
    this.estilo,
    this.duracion = const Duration(milliseconds: 650),
  });

  /// El importe como cadena del contrato (`^-?\d+\.\d{2}$`).
  final String monto;
  final String moneda;

  /// Lo que el lector de pantalla dice antes de la cifra («Saldo disponible»).
  final String? etiqueta;
  final TextStyle? estilo;
  final Duration duracion;

  @override
  State<MontoQueSube> createState() => _MontoQueSubeState();
}

class _MontoQueSubeState extends State<MontoQueSube>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: widget.duracion,
  );
  late int _desde = 0;
  late int _hasta = centavosDe(widget.monto);

  /// Se decide acá y no en `build`: mover el controlador mientras se construye el
  /// árbol dispara otra construcción en el mismo frame.
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    _arrancar();
  }

  @override
  void didUpdateWidget(MontoQueSube anterior) {
    super.didUpdateWidget(anterior);
    // Mismo importe, ninguna animación: refrescar la pantalla y que el saldo no haya
    // cambiado no puede mandar la cifra de vuelta al arranque.
    if (anterior.monto == widget.monto) return;
    // De donde estaba, no de cero: cuando el saldo cambia, lo que cuenta es el salto.
    _desde = _centavosVisibles;
    _hasta = centavosDe(widget.monto);
    _control.value = 0;
    _arrancar();
  }

  void _arrancar() {
    // El sistema puede pedir que no haya movimiento —por mareo, por batería, por
    // preferencia—. Ahí el importe aparece entero y listo.
    if (_desde == _hasta || MediaQuery.disableAnimationsOf(context)) {
      _control.value = 1;
      return;
    }
    _control.forward();
  }

  int get _centavosVisibles {
    final t = Curves.easeOutCubic.transform(_control.value);
    // Redondeo, no truncado: el último frame tiene que dar exactamente el importe.
    return _desde + ((_hasta - _desde) * t).round();
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final base = widget.estilo ?? Theme.of(context).textTheme.titleLarge!;
    final estilo = base.copyWith(
      fontFamily: Fuente.display,
      fontFeatures: const [FontFeature.tabularFigures()],
    );
    return Semantics(
      // Siempre el valor final, desde el primer frame. Quien escucha necesita su
      // saldo, no enterarse de que hay una animación.
      label: montoParaLectura(
        monto: widget.monto,
        moneda: widget.moneda,
        etiqueta: widget.etiqueta,
      ),
      excludeSemantics: true,
      child: AnimatedBuilder(
        animation: _control,
        builder: (context, _) => Text(
          formatearMonto(
            monto: montoDesdeCentavos(_centavosVisibles),
            moneda: widget.moneda,
          ),
          style: estilo,
        ),
      ),
    );
  }
}
