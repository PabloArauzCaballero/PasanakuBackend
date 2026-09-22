import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

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
    return Container(
      padding: const EdgeInsets.all(Espacio.s1),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.pill),
      ),
      child: Row(
        children: [
          for (final e in opciones.entries)
            Expanded(
              child: Semantics(
                button: true,
                selected: e.key == valor,
                label: e.value,
                child: GestureDetector(
                  onTap: () => onChanged(e.key),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 120),
                    height: Tactil.minimo,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: e.key == valor
                          ? t.verdeSolido
                          : Colors.transparent,
                      borderRadius: BorderRadius.circular(Radios.pill),
                    ),
                    child: Text(
                      e.value,
                      style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        color: e.key == valor ? t.sobreVerdeSolido : t.text2,
                        fontWeight: FontWeight.w600,
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
