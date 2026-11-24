import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// La confirmación de la recarga: el tilde, cuánto se acreditó y las tres etapas cumplidas.
///
/// Sobria a propósito: la celebración es el saldo que sube en la fila de abajo, no confeti. Sólo
/// se muestra con la respuesta de la acreditación en la mano, nunca antes (regla 91.3).
class RecargaAcreditada extends StatelessWidget {
  const RecargaAcreditada({super.key, required this.orden});
  final SalidaRecarga orden;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: Espacio.s4),
        child: EstadoDeTramite(
          icono: IconosDeMarca.tramiteListo,
          tono: Tono.ok,
          titulo: TextosBilletera.recargaAcreditada,
          detalle: TextosBilletera.recargaAcreditadaDetalle,
          extra: Column(
            children: [
              Text(
                TextosBilletera.acreditado,
                style: Tipo.etiqueta.copyWith(color: t.text2),
              ),
              const SizedBox(height: Espacio.s1),
              FittedBox(
                fit: BoxFit.scaleDown,
                child: Monto(
                  monto: orden.acreditara.monto,
                  moneda: orden.acreditara.moneda.value,
                  etiqueta: TextosBilletera.acreditado,
                  estilo: Tipo.cifraGrande.copyWith(color: t.okTexto),
                ),
              ),
            ],
          ),
          pasos: const [
            TextosBilletera.pasoPedida,
            TextosBilletera.pasoConfirmaElBanco,
            TextosBilletera.pasoAcreditada,
          ],
          pasoActual: 3,
        ),
      ),
    );
  }
}
