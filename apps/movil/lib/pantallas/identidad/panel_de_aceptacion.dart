import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'dominio/estado_contrato.dart';
import 'textos_del_alta.dart';

/// Las tres casillas y el botón de CU-05, fijos debajo del texto. Las casillas se
/// habilitan recién cuando el contrato se leyó hasta el final.
class PanelDeAceptacion extends ConsumerWidget {
  const PanelDeAceptacion({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(contratoProvider);
    final notifier = ref.read(contratoProvider.notifier);
    return // Borde arriba: separa lo que se firma de lo que se lee.
    DecoratedBox(
      decoration: BoxDecoration(
        border: Border(top: BorderSide(color: Tokens.of(context).border)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(Espacio.s4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Casilla(
              etiqueta: TextosDelAlta.aceptoContrato,
              valor: estado.aceptaContrato,
              onChanged: estado.leidoHastaElFinal
                  ? notifier.alternarContrato
                  : null,
            ),
            Casilla(
              etiqueta: TextosDelAlta.aceptoTarifario,
              valor: estado.aceptaTarifario,
              onChanged: estado.leidoHastaElFinal
                  ? notifier.alternarTarifario
                  : null,
            ),
            Casilla(
              etiqueta: TextosDelAlta.aceptoTratamientoDatos,
              valor: estado.aceptaTratamientoDatos,
              onChanged: estado.leidoHastaElFinal
                  ? notifier.alternarTratamientoDatos
                  : null,
            ),
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: TextosDelAlta.aceptarYContinuar,
              variante: BotonVariante.primario,
              expandido: true,
              // El servidor es quien realmente registra la aceptación
              // (CU-05); acá solo se habilita seguir. Cuando exista el
              // cliente de cumplimiento, este onPressed llama a
              // `POST /contratos/{id}/aceptaciones` antes de volver.
              onPressed: estado.puedeAceptar ? () => context.pop(true) : null,
            ),
          ],
        ),
      ),
    );
  }
}
