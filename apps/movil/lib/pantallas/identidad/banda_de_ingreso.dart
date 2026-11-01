import 'package:aportaya_diseno/atomos/marca.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

/// La banda de marca de las pantallas de entrada (ADR-053): el mismo verde que la tarjeta del
/// saldo, con la marca, el saludo y el titular. Es el primer momento de confianza de la app;
/// antes la marca era un ícono chico sobre un formulario.
///
/// Se extiende detrás de la barra de estado y va **dentro** del scroll: con el teclado abierto
/// se corre y deja el espacio al formulario, en vez de quedarse fija comiéndoselo.
class BandaDeIngreso extends StatelessWidget {
  const BandaDeIngreso({
    super.key,
    required this.saludo,
    required this.titular,
  });

  final String saludo;
  final String titular;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
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
        // El brillo de arriba a la derecha, el mismo de la tarjeta del saldo.
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
            Espacio.s4,
            arriba + Espacio.s2,
            Espacio.s4,
            Espacio.s6,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const CabeceraDeSeccion(titulo: ''),
              const SizedBox(height: Espacio.s3),
              Marca(
                tamano: Espacio.s7 + Espacio.s2,
                colorDelTrazo: t.heroTexto,
              ),
              const SizedBox(height: Espacio.s4),
              Text(saludo, style: Tipo.cuerpo.copyWith(color: t.heroTexto2)),
              Semantics(
                header: true,
                child: Text(
                  titular,
                  style: Tipo.tituloPantalla.copyWith(color: t.heroTexto),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
