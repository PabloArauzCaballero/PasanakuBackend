import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/boton_tamano.dart';
import 'package:aportaya_diseno/moleculas/barra_de_accion.dart';
import 'package:flutter/material.dart';

import 'textos_del_traspaso.dart';

/// La acción grande de abajo, siempre en el mismo lugar: «Continuar» en los dos
/// primeros pasos, «Traspasar cupo» en la revisión (abre la hoja de confirmación) e
/// «Ir a mis grupos» en el comprobante. Mientras se envía, queda bloqueada.
class AccionDelTraspaso extends StatelessWidget {
  const AccionDelTraspaso({
    super.key,
    required this.paso,
    required this.hayElegido,
    required this.largoDelMotivo,
    required this.enviando,
    required this.listo,
    this.confirmando = false,
    required this.alContinuar,
    required this.alTraspasar,
    required this.alTerminar,
  });

  final int paso;
  final bool hayElegido;
  final int largoDelMotivo;
  final bool enviando;

  /// La hoja de confirmación está abierta: la acción no responde, pero no gira.
  final bool confirmando;
  final bool listo;
  final VoidCallback alContinuar;
  final VoidCallback alTraspasar;
  final VoidCallback alTerminar;

  bool get _motivoValido => largoDelMotivo >= 10 && largoDelMotivo <= 300;

  @override
  Widget build(BuildContext context) {
    final (texto, bloqueo, alTocar) = listo
        ? (TextosDelTraspaso.irAMisGrupos, null, alTerminar)
        : switch (paso) {
            0 => (
              TextosDelTraspaso.continuar,
              null,
              hayElegido ? alContinuar : null,
            ),
            1 => (
              TextosDelTraspaso.continuar,
              _motivoValido ? null : TextosDelTraspaso.motivoCorto,
              _motivoValido ? alContinuar : null,
            ),
            _ => (
              TextosDelTraspaso.traspasar,
              null,
              enviando || confirmando ? null : alTraspasar,
            ),
          };
    return BarraDeAccion(
      hijo: Boton(
        key: const ValueKey('accion-del-traspaso'),
        texto: texto,
        variante: BotonVariante.primario,
        tamano: BotonTamano.lg,
        expandido: true,
        cargando: enviando && !listo,
        motivoDeBloqueo: bloqueo,
        onPressed: alTocar,
      ),
    );
  }
}
