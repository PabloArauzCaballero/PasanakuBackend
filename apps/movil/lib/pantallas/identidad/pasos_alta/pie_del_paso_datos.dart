import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';

class PieDelPasoDatos extends StatelessWidget {
  const PieDelPasoDatos({required this.onContinuar, super.key});

  final VoidCallback onContinuar;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Boton(
          texto: TextosIdentidad.continuar,
          icono: Icons.arrow_forward,
          variante: BotonVariante.primario,
          expandido: true,
          onPressed: onContinuar,
        ),
        const SizedBox(height: Espacio.s3),
        Text(
          TextosIdentidad.registroPie,
          textAlign: TextAlign.center,
          style: Tipo.ayuda.copyWith(color: t.text3),
        ),
      ],
    );
  }
}
