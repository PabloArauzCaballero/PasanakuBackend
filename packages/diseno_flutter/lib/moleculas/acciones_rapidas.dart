import 'package:flutter/material.dart';

import '../atomos/hundido_al_tocar.dart';
import '../tokens/tokens.dart';

/// Los accesos de la billetera como **círculos** con su nombre debajo, el lenguaje de
/// las apps bancarias (ADR-053). Cada círculo es una superficie elevada con el ícono en
/// verde de marca; [destacada] (si hay) va en verde sólido: es lo que la persona viene a
/// hacer. Cuatro como máximo: más que eso ya no es «rápido».
class AccionesRapidas extends StatelessWidget {
  const AccionesRapidas({super.key, required this.acciones, this.destacada});

  final List<({String texto, IconData icono, VoidCallback onTap})> acciones;

  /// Índice de la acción principal, o `null` si ninguna se destaca.
  final int? destacada;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final (i, a) in acciones.indexed)
          Expanded(
            child: Semantics(
              button: true,
              label: a.texto,
              excludeSemantics: true,
              child: HundidoAlTocar(
                child: InkWell(
                  onTap: a.onTap,
                  customBorder: const StadiumBorder(),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: Espacio.s1),
                    child: Column(
                      children: [
                        DecoratedBox(
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: i == destacada ? t.verdeSolido : t.surface,
                            border:
                                Theme.of(context).brightness == Brightness.dark
                                ? Border.all(color: t.border)
                                : null,
                            boxShadow: Elevacion.tarjeta(t),
                          ),
                          child: SizedBox.square(
                            dimension: Tactil.minimo + Espacio.s2,
                            child: Icon(
                              a.icono,
                              size: Espacio.s5,
                              color: i == destacada
                                  ? t.sobreVerdeSolido
                                  : t.brandTexto,
                            ),
                          ),
                        ),
                        const SizedBox(height: Espacio.s2),
                        Text(
                          a.texto,
                          textAlign: TextAlign.center,
                          maxLines: 2,
                          style: Tipo.cuerpoChico.copyWith(
                            color: t.text2,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
