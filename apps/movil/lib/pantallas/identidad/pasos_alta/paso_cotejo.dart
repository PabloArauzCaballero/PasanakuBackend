import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/moleculas/fila_de_cotejo.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';

/// Paso 6 de 8 — cotejo campo a campo (D-1: "cotejo campo a campo"). Sin OCR
/// contratado todavía (hueco H-CLIENTE), `leido` reusa lo declarado por la persona
/// — la fila igual muestra la mecánica de coincidencia/corrección que pide la
/// maqueta, y queda lista para que el proveedor real reemplace ese origen de dato.
class PasoCotejo extends ConsumerWidget {
  const PasoCotejo({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final datos = ref.watch(altaProvider).datos;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          FilaDeCotejo(
            campo: TextosIdentidad.nombres,
            declarado: datos.nombres,
            leido: datos.nombres,
          ),
          const SizedBox(height: Espacio.s2),
          FilaDeCotejo(
            campo: TextosIdentidad.apellidos,
            declarado: datos.apellidos,
            leido: datos.apellidos,
          ),
          const SizedBox(height: Espacio.s2),
          FilaDeCotejo(
            campo: TextosIdentidad.numeroDocumento,
            declarado: datos.numeroDocumento,
            leido: datos.numeroDocumento,
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: () => ref.read(altaProvider.notifier).siguiente(),
          ),
        ],
      ),
    );
  }
}
