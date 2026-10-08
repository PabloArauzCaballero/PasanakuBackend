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
    final escala = MediaQuery.textScalerOf(context).scale(14) / 14;
    final vertical = escala >= 1.5;
    final items = [
      for (final e in opciones.entries)
        _OpcionSegmentada(
          texto: e.value,
          seleccionada: e.key == valor,
          onTap: () => onChanged(e.key),
        ),
    ];
    return Container(
      padding: const EdgeInsets.all(Espacio.s1),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.pill),
      ),
      child: vertical
          ? Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (var i = 0; i < items.length; i++) ...[
                  if (i > 0) const SizedBox(height: Espacio.s1),
                  items[i],
                ],
              ],
            )
          : Row(children: [for (final item in items) Expanded(child: item)]),
    );
  }
}

class _OpcionSegmentada extends StatelessWidget {
  const _OpcionSegmentada({
    required this.texto,
    required this.seleccionada,
    required this.onTap,
  });

  final String texto;
  final bool seleccionada;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      button: true,
      selected: seleccionada,
      label: texto,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: AnimatedContainer(
          duration: MediaQuery.disableAnimationsOf(context)
              ? Duration.zero
              : MovimientoDeInterfaz.respuesta,
          constraints: const BoxConstraints(minHeight: Tactil.minimo),
          padding: const EdgeInsets.symmetric(
            horizontal: Espacio.s2,
            vertical: Espacio.s2,
          ),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: seleccionada ? t.verdeSolido : Colors.transparent,
            borderRadius: BorderRadius.circular(Radios.pill),
          ),
          child: Text(
            texto,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: seleccionada ? t.sobreVerdeSolido : t.text2,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}
