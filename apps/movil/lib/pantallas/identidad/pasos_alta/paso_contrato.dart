import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import '../textos.dart';
import '../textos_del_alta.dart';

class PasoContrato extends StatelessWidget {
  const PasoContrato({
    super.key,
    required this.onIrAlContrato,
    required this.enviando,
    required this.error,
  });

  final VoidCallback onIrAlContrato;
  final bool enviando;
  final String? error;

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
        if (error != null) ...[
          const SizedBox(height: Espacio.s4),
          Alerta(tono: Tono.error, titulo: error!),
        ],
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
