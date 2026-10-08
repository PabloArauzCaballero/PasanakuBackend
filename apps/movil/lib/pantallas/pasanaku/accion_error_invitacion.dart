import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Acción de reintento para fallos temporales, o salida para enlaces rechazados.
class AccionErrorInvitacion extends StatelessWidget {
  const AccionErrorInvitacion({
    super.key,
    required this.recuperable,
    required this.onPressed,
  });

  final bool recuperable;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => TextButton(
    onPressed: onPressed,
    style: TextButton.styleFrom(
      minimumSize: const Size(Tactil.minimo, Tactil.minimo),
    ),
    child: Text(recuperable ? 'Reintentar' : 'Ir a portada'),
  );
}
