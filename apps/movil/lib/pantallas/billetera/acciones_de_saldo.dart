import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'textos.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

/// Las tres acciones del saldo: **una sola naranja**.
///
/// Aportar es lo que la persona viene a hacer —es lo que mantiene vivo su pasanaku—,
/// así que se lleva el color de acción; recargar y retirar quedan en contorno. Cuatro
/// íconos iguales en una grilla no dicen cuál importa; esto sí.
///
/// Ninguna acción puede quedar con el texto cortado («Recarg…» no le dice nada a nadie), así
/// que la disposición depende de lo que cabe: en una fila si hay ancho (tablet y escritorio),
/// con Aportar arriba y las otras dos debajo en un teléfono, y las tres apiladas si el texto
/// está muy agrandado.
class AccionesDeSaldo extends StatelessWidget {
  const AccionesDeSaldo({super.key, required this.cuentaId});

  final String cuentaId;

  /// Desde este ancho las tres caben en una fila.
  static const double _anchoParaUnaFila = 480;

  /// Por encima de esta escala de texto ni dos columnas alcanzan: se apilan.
  static const double _escalaMaximaEnDosColumnas = 1.3;

  @override
  Widget build(BuildContext context) {
    final escala = MediaQuery.textScalerOf(context).scale(1);
    final aportar = _accion(
      TextosBilletera.aportar,
      Icons.arrow_outward,
      BotonVariante.primario,
      () => context.pushNamed('pasanaku.miEstado'),
    );
    final recargar = _accion(
      TextosBilletera.recargar,
      Icons.add,
      BotonVariante.fantasma,
      () => context.pushNamed(
        'billetera.recargar',
        queryParameters: {'cuenta': cuentaId},
      ),
    );
    final retirar = _accion(
      TextosBilletera.retirar,
      Icons.account_balance_outlined,
      BotonVariante.fantasma,
      () => context.pushNamed(
        'billetera.retirar',
        queryParameters: {'cuenta': cuentaId},
      ),
    );
    const hueco = SizedBox(width: Espacio.s2, height: Espacio.s2);

    return LayoutBuilder(
      builder: (context, restriccion) {
        if (escala > _escalaMaximaEnDosColumnas) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [aportar, hueco, recargar, hueco, retirar],
          );
        }
        if (restriccion.maxWidth >= _anchoParaUnaFila) {
          return IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(child: aportar),
                hueco,
                Expanded(child: recargar),
                hueco,
                Expanded(child: retirar),
              ],
            ),
          );
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            aportar,
            hueco,
            IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(child: recargar),
                  hueco,
                  Expanded(child: retirar),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _accion(
    String texto,
    IconData icono,
    BotonVariante variante,
    VoidCallback alPresionar,
  ) => Boton(
    texto: texto,
    icono: icono,
    variante: variante,
    expandido: true,
    maxLineas: 1,
    onPressed: alPresionar,
  );
}
