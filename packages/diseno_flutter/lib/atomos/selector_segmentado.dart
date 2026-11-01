import 'dart:async';

import 'package:flutter/material.dart';

import '../moviles/alcance_haptica.dart';
import '../tokens/tokens.dart';
import 'hundido_al_tocar.dart';
import 'movimiento.dart';

/// Cambio entre vistas equivalentes (lista · calendario; QR · código). **El elegido
/// lleva relleno de marca**: no depende de que dos fondos difieran (D-12).
class SelectorSegmentado<T> extends StatelessWidget {
  const SelectorSegmentado({
    super.key,
    required this.opciones,
    required this.valor,
    required this.onChanged,
  });

  final Map<T, String> opciones;
  final T valor;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    assert(opciones.isNotEmpty, 'SelectorSegmentado requiere opciones.');
    if (opciones.isEmpty) return const SizedBox.shrink();
    if (opciones.length == 1) {
      final unica = opciones.entries.single;
      return Semantics(
        container: true,
        label: unica.value,
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
          child: Text(
            unica.value,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: t.text2),
          ),
        ),
      );
    }
    return Container(
      padding: const EdgeInsets.all(Espacio.s1),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.pill),
        border: Border.all(color: t.border, width: Borde.fino),
      ),
      child: Row(
        children: [
          for (final e in opciones.entries)
            Expanded(
              child: Semantics(
                button: true,
                selected: e.key == valor,
                label: e.value,
                child: HundidoAlTocar(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTap: () {
                      if (e.key != valor) {
                        unawaited(AlcanceHaptica.maybeOf(context)?.seleccion());
                      }
                      onChanged(e.key);
                    },
                    child: AnimatedContainer(
                      duration: duracion(context, Movimiento.cambioDeEstado),
                      curve: Movimiento.llega,
                      height: Tactil.minimo,
                      alignment: Alignment.center,
                      decoration: BoxDecoration(
                        color: e.key == valor
                            ? t.verdeSolido
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(Radios.pill),
                        boxShadow: e.key == valor ? [t.sombra1] : null,
                      ),
                      child: AnimatedDefaultTextStyle(
                        duration: duracion(context, Movimiento.cambioDeEstado),
                        curve: Movimiento.llega,
                        style:
                            (Theme.of(context).textTheme.labelLarge ??
                                    Tipo.boton)
                                .copyWith(
                                  color: e.key == valor
                                      ? t.sobreVerdeSolido
                                      : t.text2,
                                  fontWeight: FontWeight.w600,
                                ),
                        child: Text(
                          e.value,
                          textAlign: TextAlign.center,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
