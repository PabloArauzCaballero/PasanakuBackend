import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Un título de sección con su acción al costado («Movimientos … Todos»).
/// Es el único texto que va suelto sobre el fondo: todo lo demás vive en una superficie.
class TituloDeSeccion extends StatelessWidget {
  const TituloDeSeccion({
    super.key,
    required this.titulo,
    this.accion,
    this.alTocarAccion,
    this.alCostado,
  });

  final String titulo;

  /// El texto del enlace de la derecha («Ver los 3»).
  final String? accion;
  final VoidCallback? alTocarAccion;

  /// Algo que no es un enlace —una insignia de plazo, por ejemplo— a la derecha.
  final Widget? alCostado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: Semantics(
              header: true,
              child: Text(titulo, style: Tipo.titulo3.copyWith(color: t.text)),
            ),
          ),
          ?alCostado,
          if (accion != null)
            TextButton(onPressed: alTocarAccion, child: Text(accion!)),
        ],
      ),
    );
  }
}

/// La superficie donde se apoya un grupo de contenido. Lo que no está acá adentro es
/// un título de sección o la cifra principal: nada más anda suelto sobre el fondo.
class Panel extends StatelessWidget {
  const Panel({
    super.key,
    required this.hijo,
    this.relleno = const EdgeInsets.all(Espacio.s3),
    this.destacado = false,
  });

  final Widget hijo;
  final EdgeInsets relleno;

  /// Un panel destacado pierde el borde y gana sombra: se usa para lo que vence, que
  /// es lo único que tiene que saltar de la página.
  final bool destacado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return DecoratedBox(
      decoration: BoxDecoration(
        color: t.surface,
        borderRadius: BorderRadius.circular(Radios.lg),
        border: destacado
            ? null
            : Border.all(color: t.border, width: Borde.fino),
        boxShadow: destacado ? [t.sombra2] : null,
      ),
      child: Padding(padding: relleno, child: hijo),
    );
  }
}

/// Las filas de un [Panel], separadas por una línea que no llega a los bordes.
class FilasDePanel extends StatelessWidget {
  const FilasDePanel({super.key, required this.filas});

  final List<Widget> filas;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      children: [
        for (var i = 0; i < filas.length; i++) ...[
          if (i > 0) Divider(height: Espacio.s4, thickness: 1, color: t.border),
          filas[i],
        ],
      ],
    );
  }
}
