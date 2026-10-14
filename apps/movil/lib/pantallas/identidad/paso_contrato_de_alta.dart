import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';
import 'textos_del_alta.dart';

class PasoContratoDeAlta extends StatelessWidget {
  const PasoContratoDeAlta({
    super.key,
    required this.onIrAlContrato,
    required this.enviando,
  });

  final VoidCallback onIrAlContrato;
  final bool enviando;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.all(Espacio.s4),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Último paso: leer y aceptar el contrato de adhesión y el '
          'tarifario vigente.',
        ),
        const SizedBox(height: Espacio.s4),
        Boton(
          texto: enviando
              ? TextosDelAlta.altaEnviando
              : TextosIdentidad.continuar,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: enviando,
          onPressed: enviando ? null : onIrAlContrato,
        ),
      ],
    ),
  );
}
