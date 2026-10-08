import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'dominio/detalle_visible_de_invitacion.dart';

class DetalleDeInvitacion extends StatelessWidget {
  const DetalleDeInvitacion({
    super.key,
    required this.detalle,
    required this.aceptaReglamento,
    required this.enviando,
    required this.alCambiarAceptacion,
    required this.alAceptar,
  });

  final DetalleVisibleDeInvitacion detalle;
  final bool aceptaReglamento;
  final bool enviando;
  final ValueChanged<bool?> alCambiarAceptacion;
  final VoidCallback alAceptar;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Text(detalle.nombre, style: Theme.of(context).textTheme.headlineSmall),
      const SizedBox(height: Espacio.s3),
      Text('Aporte por período: ${detalle.aporteFormateado}'),
      Text('Frecuencia: ${detalle.frecuencia}'),
      const SizedBox(height: Espacio.s5),
      const Text('Reglamento del grupo'),
      const SizedBox(height: Espacio.s2),
      Text(detalle.reglamento),
      CheckboxListTile(
        value: aceptaReglamento,
        onChanged: alCambiarAceptacion,
        title: const Text('Leí y acepto este reglamento'),
        controlAffinity: ListTileControlAffinity.leading,
      ),
      ElevatedButton(
        onPressed: aceptaReglamento && !enviando ? alAceptar : null,
        child: const Text('Unirme al grupo'),
      ),
    ],
  );
}
