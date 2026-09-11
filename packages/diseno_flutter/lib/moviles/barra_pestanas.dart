import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Tab bar de 3–5 destinos, el activo en marca, con punto de novedades opcional.
class BarraPestanas extends StatelessWidget {
  const BarraPestanas({
    super.key,
    required this.destinos,
    required this.actual,
    required this.onChanged,
  });
  final List<
    ({String texto, IconData icono, IconData iconoActivo, int novedades})
  >
  destinos;
  final int actual;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    assert(
      destinos.length >= 3 && destinos.length <= 5,
      'entre 3 y 5 destinos',
    );
    return NavigationBar(
      selectedIndex: actual,
      onDestinationSelected: onChanged,
      backgroundColor: t.surface,
      indicatorColor: t.brandBg,
      height: Tactil.minimo + Espacio.s5,
      destinations: [
        for (final d in destinos)
          NavigationDestination(
            icon: Badge(
              isLabelVisible: d.novedades > 0,
              label: Text('${d.novedades}'),
              backgroundColor: t.err,
              child: Icon(d.icono, color: t.text2),
            ),
            selectedIcon: Icon(d.iconoActivo, color: t.brandTexto),
            label: d.texto,
            tooltip: d.novedades > 0
                ? '${d.texto}, ${d.novedades} sin leer'
                : d.texto,
          ),
      ],
    );
  }
}
