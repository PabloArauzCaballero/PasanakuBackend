import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/codigo_q_r.dart';
import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'latido_de_espera.dart';
import 'textos.dart';

/// La tarjeta protagonista del cobro: en qué está el pago, cuánto se acredita y el QR.
///
/// El monto es lo más grande de la pantalla porque es lo que la persona tiene que reconocer en
/// la app de su banco; el saldo va abajo, como dato secundario. Con [dePrueba] lleva la etiqueta
/// «QR de prueba» en la esquina: discreta, pero escrita, nunca sólo un color.
class TarjetaDelQr extends StatelessWidget {
  const TarjetaDelQr({
    super.key,
    required this.orden,
    required this.dePrueba,
    required this.fallo,
  });
  final SalidaRecarga orden;
  final bool dePrueba;

  /// El último intento de confirmar el pago falló: el estado lo dice en vez de seguir latiendo.
  final bool fallo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Tarjeta(
      child: Padding(
        padding: const EdgeInsets.all(Espacio.s1),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              spacing: Espacio.s2,
              runSpacing: Espacio.s2,
              children: [
                _EstadoDelPago(fallo: fallo),
                if (dePrueba)
                  const ChipEstado(
                    texto: TextosBilletera.qrDePrueba,
                    tono: Tono.aviso,
                    icono: IconosDeMarca.qr,
                  ),
              ],
            ),
            const SizedBox(height: Espacio.s5),
            Text(
              TextosBilletera.montoAAcreditar,
              style: Tipo.etiqueta.copyWith(color: t.text2),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: Espacio.s1),
            Center(
              child: FittedBox(
                fit: BoxFit.scaleDown,
                // Lo calcula el servidor (puede descontar el costo del proveedor): no se recalcula.
                child: Monto(
                  monto: orden.acreditara.monto,
                  moneda: orden.acreditara.moneda.value,
                  etiqueta: TextosBilletera.montoAAcreditar,
                  estilo: Tipo.cifraSaldo.copyWith(color: t.text),
                ),
              ),
            ),
            const SizedBox(height: Espacio.s5),
            LayoutBuilder(
              builder: (context, limites) {
                // El QR crece con el ancho disponible, sin pasar del tamaño que se lee bien de
                // cerca ni bajar del mínimo que una cámara enfoca sin esfuerzo.
                final lado = (limites.maxWidth - Espacio.s6).clamp(
                  Espacio.s7 * 4,
                  Espacio.s7 * 4.5,
                );
                return Center(
                  child: CodigoQR(
                    contenido: orden.cobroQr!.contenido,
                    etiqueta: TextosBilletera.qrDeLaRecarga,
                    tamano: lado,
                  ),
                );
              },
            ),
            const SizedBox(height: Espacio.s2),
          ],
        ),
      ),
    );
  }
}

/// «Esperando la confirmación del banco» con el punto que late, o el fallo con su ícono. El
/// texto va siempre: el color y el movimiento acompañan, no informan solos.
class _EstadoDelPago extends StatelessWidget {
  const _EstadoDelPago({required this.fallo});
  final bool fallo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = fallo ? t.errTexto : t.infoTexto;
    final texto = fallo
        ? TextosBilletera.pagoSinConfirmar
        : TextosBilletera.esperandoAlBanco;
    return Semantics(
      liveRegion: true,
      label: 'Estado: $texto',
      excludeSemantics: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (fallo)
            Icon(IconosDeMarca.tramiteFallido, size: Espacio.s5, color: color)
          else
            LatidoDeEspera(color: color),
          const SizedBox(width: Espacio.s2),
          Flexible(
            child: Text(texto, style: Tipo.cuerpoFuerte.copyWith(color: color)),
          ),
        ],
      ),
    );
  }
}
