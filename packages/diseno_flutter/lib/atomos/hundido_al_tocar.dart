import 'package:flutter/material.dart';

/// Hunde apenas lo que envuelve mientras el dedo está encima, y lo suelta al levantarlo.
///
/// Es la respuesta que una pantalla táctil le debe a un dedo: sin ella, entre el toque
/// y lo que pasa después hay un hueco en el que la persona no sabe si el botón la
/// escuchó, y vuelve a tocar. Un 3 % alcanza; más se siente un juguete.
///
/// No se traga los gestos: sigue tocando su hijo y respeta «reducir movimiento».
class HundidoAlTocar extends StatefulWidget {
  const HundidoAlTocar({super.key, required this.child, this.activo = true});

  final Widget child;
  final bool activo;

  @override
  State<HundidoAlTocar> createState() => _HundidoAlTocarState();
}

class _HundidoAlTocarState extends State<HundidoAlTocar> {
  bool _apretado = false;

  void _marcar(bool valor) {
    if (!widget.activo || _apretado == valor) return;
    setState(() => _apretado = valor);
  }

  @override
  Widget build(BuildContext context) {
    final sinMovimiento = MediaQuery.disableAnimationsOf(context);
    return Listener(
      // `Listener` y no `GestureDetector`: no compite en la arena de gestos, así que
      // el botón de adentro sigue recibiendo su toque tal cual.
      onPointerDown: (_) => _marcar(true),
      onPointerUp: (_) => _marcar(false),
      onPointerCancel: (_) => _marcar(false),
      child: AnimatedScale(
        scale: _apretado && !sinMovimiento ? 0.97 : 1,
        duration: const Duration(milliseconds: 90),
        curve: Curves.easeOut,
        child: widget.child,
      ),
    );
  }
}
