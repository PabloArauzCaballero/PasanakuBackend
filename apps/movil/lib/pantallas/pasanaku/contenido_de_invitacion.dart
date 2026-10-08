import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Mantiene legible la invitación en tablet sin quitar el scroll del teléfono.
class ContenidoDeInvitacion extends StatelessWidget {
  const ContenidoDeInvitacion({super.key, required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Center(
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 560),
      child: ListView(
        padding: const EdgeInsets.all(Espacio.s5),
        children: children,
      ),
    ),
  );
}
