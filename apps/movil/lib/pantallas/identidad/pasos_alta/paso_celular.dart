import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/notifier_alta.dart';
import '../textos.dart';

/// Paso de contacto pausado hasta que Identidad publique emisión y validación OTP.
/// No muestra un código que nadie envió ni deja avanzar con seis dígitos arbitrarios.
class PasoCelular extends ConsumerWidget {
  const PasoCelular({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Alerta(
            titulo: TextosIdentidad.verificacionNoDisponibleTitulo,
            detalle: TextosIdentidad.verificacionNoDisponibleDetalle,
            tono: Tono.aviso,
          ),
          const SizedBox(height: Espacio.s4),
          Boton(
            texto: TextosIdentidad.volverAlPasoAnterior,
            variante: BotonVariante.fantasma,
            expandido: true,
            onPressed: () => ref.read(altaProvider.notifier).atras(),
          ),
        ],
      ),
    );
  }
}
