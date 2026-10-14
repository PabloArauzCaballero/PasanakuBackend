import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:flutter/material.dart';

/// Cambio horizontal breve entre los pasos que comparten la pantalla de alta.
class TransicionDePasoAlta extends StatelessWidget {
  const TransicionDePasoAlta({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => AnimatedSwitcher(
    duration: duracion(context, Movimiento.pagina),
    reverseDuration: duracion(context, Movimiento.paginaDeVuelta),
    transitionBuilder: (hijo, animacion) {
      if (reducirMovimiento(context)) return hijo;
      final sale = animacion.status == AnimationStatus.reverse;
      final desde = (sale ? -1 : 1) * Movimiento.corrimiento;
      final desplazamiento = Tween<double>(
        begin: desde,
        end: 0,
      ).animate(CurvedAnimation(parent: animacion, curve: Movimiento.llega));
      return AnimatedBuilder(
        animation: desplazamiento,
        child: hijo,
        builder: (context, contenido) => Opacity(
          opacity: animacion.value,
          child: Transform.translate(
            offset: Offset(desplazamiento.value, 0),
            child: contenido,
          ),
        ),
      );
    },
    child: child,
  );
}
