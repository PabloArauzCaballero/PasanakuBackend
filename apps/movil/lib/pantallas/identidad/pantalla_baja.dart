import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// CU-09 — solicitar la baja. Confirmación explícita y sin ambigüedad: el botón de
/// peligro pide un segundo toque en un diálogo, y explica que hace falta saldo en
/// cero y sin cupos activos (supuesto: la bóveda no detalla la condición exacta en
/// la pantalla, así que se declara acá y se deja que el servidor la rechace con su
/// propio código si no se cumple — invariante 7).
class PantallaDeBaja extends StatelessWidget {
  const PantallaDeBaja({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text(TextosIdentidad.tituloBaja)),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Alerta(titulo: TextosIdentidad.avisoBaja, tono: Tono.aviso),
              const SizedBox(height: Espacio.s5),
              Boton(
                texto: TextosIdentidad.confirmarBaja,
                variante: BotonVariante.peligro,
                expandido: true,
                onPressed: () => showDialog<void>(
                  context: context,
                  builder: (dialogContext) => AlertDialog(
                    title: const Text(TextosIdentidad.tituloBaja),
                    content: const Text(TextosIdentidad.avisoBaja),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.of(dialogContext).pop(),
                        child: const Text(TextosIdentidad.atras),
                      ),
                      TextButton(
                        onPressed: () => Navigator.of(dialogContext).pop(),
                        child: const Text(TextosIdentidad.confirmarBaja),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
