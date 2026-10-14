import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// Mensaje de privacidad al pie de cada formulario del alta.
class TarjetaDeConfianza extends StatelessWidget {
  const TarjetaDeConfianza({super.key});

  static const mensaje =
      'Tus datos viajan cifrados y solo los usa AportaYa para verificar que sos vos.';

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Container(
      padding: const EdgeInsets.all(Espacio.s4),
      decoration: BoxDecoration(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.md),
        border: Border.all(color: t.border, width: Borde.fino),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.lock_outline, size: 18, color: t.text3),
          const SizedBox(width: Espacio.s2),
          Expanded(
            child: Text(
              mensaje,
              style: Tipo.cuerpoChico.copyWith(color: t.text2),
            ),
          ),
        ],
      ),
    );
  }
}
