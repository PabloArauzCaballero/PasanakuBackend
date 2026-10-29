import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/atomos/palabra_de_marca.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// El verde del saldo como banda de entrada, con la marca sobre su sello de acción y la
/// palabra en claro. Es lo primero que se ve de AportaYa: la misma tarjeta protagonista
/// de la app, a lo ancho.
class BandaDeMarca extends StatelessWidget {
  const BandaDeMarca({super.key, required this.t});
  final Tokens t;

  @override
  Widget build(BuildContext context) {
    final arriba = MediaQuery.paddingOf(context).top;
    const redondeo = BorderRadius.vertical(bottom: Radius.circular(Radios.xxl));
    return DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: redondeo,
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [t.heroInicio, t.heroFin],
        ),
        boxShadow: Elevacion.heroe(t),
      ),
      child: DecoratedBox(
        decoration: BoxDecoration(
          borderRadius: redondeo,
          gradient: RadialGradient(
            center: Alignment.topRight,
            radius: 1.1,
            colors: [t.heroBrillo, t.heroBrillo.withValues(alpha: 0)],
          ),
        ),
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            Espacio.s5,
            arriba + Espacio.s3,
            Espacio.s5,
            Espacio.s4,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: Espacio.s7 + Espacio.s4,
                height: Espacio.s7 + Espacio.s4,
                decoration: BoxDecoration(
                  color: t.accent,
                  borderRadius: BorderRadius.circular(Radios.xl),
                  boxShadow: [t.sombra3],
                ),
                child: Center(
                  child: Marca(
                    tamano: Espacio.s7 + Espacio.s2,
                    colorDelTrazo: t.accentInk,
                  ),
                ),
              ),
              const SizedBox(height: Espacio.s3),
              PalabraDeMarca(ancho: 148, color: t.heroTexto),
            ],
          ),
        ),
      ),
    );
  }
}
