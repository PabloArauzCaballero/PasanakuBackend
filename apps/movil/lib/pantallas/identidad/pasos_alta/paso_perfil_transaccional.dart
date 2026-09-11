import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/estado_alta.dart';
import '../textos.dart';

/// Paso 7 de 8 — perfil transaccional declarado (CU-01 flujo 7): origen de fondos,
/// actividad económica y monto mensual estimado.
class PasoPerfilTransaccional extends ConsumerStatefulWidget {
  const PasoPerfilTransaccional({super.key});

  @override
  ConsumerState<PasoPerfilTransaccional> createState() =>
      _PasoPerfilTransaccionalState();
}

class _PasoPerfilTransaccionalState
    extends ConsumerState<PasoPerfilTransaccional> {
  final _origen = TextEditingController();
  final _actividad = TextEditingController();
  final _monto = TextEditingController();

  @override
  void dispose() {
    _origen.dispose();
    _actividad.dispose();
    _monto.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final valido =
        _origen.text.trim().isNotEmpty && _actividad.text.trim().isNotEmpty;
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Campo(
            etiqueta: TextosIdentidad.origenDeFondos,
            controlador: _origen,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosIdentidad.actividadEconomica,
            controlador: _actividad,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: Espacio.s3),
          Campo(
            etiqueta: TextosIdentidad.montoMensualEstimado,
            controlador: _monto,
            tipoDeTeclado: TextInputType.number,
            onChanged: (_) => setState(() {}),
          ),
          const SizedBox(height: Espacio.s5),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.primario,
            expandido: true,
            onPressed: valido
                ? () {
                    ref
                        .read(altaProvider.notifier)
                        .actualizarPerfilTransaccional(
                          origen: _origen.text,
                          actividad: _actividad.text,
                          monto: double.tryParse(_monto.text),
                        );
                    ref.read(altaProvider.notifier).siguiente();
                  }
                : null,
          ),
        ],
      ),
    );
  }
}
