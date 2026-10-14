import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Cabecera del alta por fases, equivalente visual a `StepHeader` de Atlas.
class CabeceraDeFase extends StatelessWidget {
  const CabeceraDeFase({
    super.key,
    required this.numero,
    required this.titulo,
    this.total = 4,
  }) : assert(total > 0),
       assert(numero > 0),
       assert(numero <= total);

  final int numero;
  final int total;
  final String titulo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final fraccion = numero / total;
    return Semantics(
      label: 'Fase $numero de $total · $titulo',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            'FASE $numero DE $total',
            style: Tipo.etiqueta.copyWith(color: t.text2),
          ),
          const SizedBox(height: Espacio.s1),
          Text(titulo, style: Tipo.titulo2.copyWith(color: t.text)),
          const SizedBox(height: Espacio.s3),
          ClipRRect(
            borderRadius: BorderRadius.circular(Radios.pill),
            child: TweenAnimationBuilder<double>(
              tween: Tween(begin: 0, end: fraccion),
              duration: duracion(context, Movimiento.pagina),
              curve: Movimiento.llega,
              builder: (context, progreso, _) => LinearProgressIndicator(
                value: progreso,
                minHeight: Espacio.s1,
                backgroundColor: t.border,
                color: t.brand,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
