import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// El retiro ya instruido. «Instruido» no es «llegó»: el banco todavía tiene que acreditarlo,
/// así que el trámite se pinta en espera (no como listo) y el paso en curso es «En camino».
/// Muestra el neto y el costo tal como los devolvió el servidor: nada se calcula acá.
class ComprobanteDeRetiro extends StatelessWidget {
  const ComprobanteDeRetiro({super.key, required this.resultado});

  final SalidaRetiro resultado;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return EstadoDeTramite(
      icono: IconosDeMarca.enEspera,
      tono: Tono.info,
      titulo: TextosBilletera.retiroInstruido,
      detalle: TextosBilletera.retiroInstruidoDetalle,
      extra: Column(
        children: [
          Monto(
            monto: resultado.montoNeto.monto,
            moneda: resultado.montoNeto.moneda.value,
            etiqueta: TextosBilletera.montoNeto,
            estilo: Tipo.cifraGrande.copyWith(color: t.text),
          ),
          const SizedBox(height: Espacio.s2),
          MergeSemantics(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '${TextosBilletera.costoDelRetiro}: ',
                  style: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
                Monto(
                  monto: resultado.costoRetiro.monto,
                  moneda: resultado.costoRetiro.moneda.value,
                  etiqueta: TextosBilletera.costoDelRetiro,
                  estilo: Tipo.cuerpoChico.copyWith(color: t.text2),
                ),
              ],
            ),
          ),
        ],
      ),
      pasos: const [
        TextosBilletera.pasoInstruido,
        TextosBilletera.pasoEnCamino,
        TextosBilletera.pasoAcreditadoEnTuCuenta,
      ],
      pasoActual: 1,
    );
  }
}
