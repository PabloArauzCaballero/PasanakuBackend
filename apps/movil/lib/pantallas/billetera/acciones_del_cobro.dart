import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos.dart';

/// La única acción del cobro en cada momento, para el pie de la pantalla:
/// - con un QR de prueba sin pagar, «Simular pago», con una línea que explica qué hace;
/// - acreditada, volver a la billetera;
/// - en cualquier otro caso no hay botón (esperando a un banco real no hay nada que tocar, y con
///   error el reintento vive en el aviso del error: una sola acción, no dos).
///
/// [hayAccion] dice si este pie dibuja algo, para no reservarle lugar cuando no.
class AccionesDelCobro extends StatelessWidget {
  const AccionesDelCobro({
    super.key,
    required this.acreditada,
    required this.simulable,
    required this.pagando,
    required this.pagar,
  });
  final bool acreditada;

  /// Hay un QR de prueba sin pagar ni fallar: se puede simular el pago.
  final bool simulable;
  final bool pagando;
  final VoidCallback pagar;

  static bool hayAccion({required bool acreditada, required bool simulable}) =>
      acreditada || simulable;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    if (acreditada) {
      return Boton(
        texto: TextosBilletera.volverALaBilletera,
        variante: BotonVariante.primario,
        expandido: true,
        onPressed: () => Navigator.of(context).maybePop(),
      );
    }
    if (!simulable) return const SizedBox.shrink();
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          TextosBilletera.qrDePruebaDetalle,
          style: Tipo.ayuda.copyWith(color: t.text2),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: Espacio.s2),
        Boton(
          texto: TextosBilletera.simularPago,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: pagando,
          onPressed: pagar,
        ),
      ],
    );
  }
}
