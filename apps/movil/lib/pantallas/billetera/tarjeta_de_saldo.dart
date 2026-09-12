import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'package:aportaya_diseno/atomos/monto.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'boton_de_dos_lineas.dart';
import 'cifra.dart';
import 'mancha_radial.dart';
import 'textos.dart';

/// La tarjeta de saldo (`docs/Views/AportaYa-Maqueta.html`, función `pintarSaldo`):
/// etiqueta, monto, nota de custodia, las dos acciones y el desglose de retenido.
/// Separada de `pantalla_de_saldo.dart` por el barrido de 200 líneas.
class TarjetaDeSaldo extends StatelessWidget {
  const TarjetaDeSaldo({super.key, required this.saldo});
  final SaldoBilletera saldo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final hayRetenido = saldo.retenido.monto != '0.00';
    // El degradado diagonal y las dos manchas radiales son la tarjeta `.saldo` de
    // la maqueta al pixel (docs/Views/AportaYa-Maqueta.html): sin ellos queda un
    // verde plano que no es lo que la maqueta muestra.
    return ClipRRect(
      borderRadius: BorderRadius.circular(Radios.xl),
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: const LinearGradient(
            begin: Alignment(-0.75, -1),
            end: Alignment(0.75, 1),
            colors: [Paleta.g500, Paleta.g600, Paleta.g800],
            stops: [0, 0.42, 1],
          ),
          boxShadow: [t.sombra2],
        ),
        child: Stack(
          children: [
            Positioned(
              right: -58,
              top: -72,
              child: ManchaRadial(
                diametro: 190,
                colorCentro: Paleta.o500.withValues(alpha: 0.36),
              ),
            ),
            Positioned(
              left: -40,
              bottom: -90,
              child: ManchaRadial(
                diametro: 180,
                colorCentro: Paleta.white.withValues(alpha: 0.10),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(Espacio.s5),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Cada bloque es su propio límite de semántica: sin esto, un lector de
                  // pantalla fusiona la tarjeta entera —etiqueta, monto, custodia y
                  // desglose— en un solo anuncio ilegible en vez de dejar que la persona
                  // navegue bloque por bloque.
                  MergeSemantics(
                    child: Semantics(
                      container: true,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          ExcludeSemantics(
                            child: Row(
                              children: [
                                Icon(
                                  Icons.verified_user_outlined,
                                  color: t.sobreVerdeSolido,
                                  size: 18,
                                ),
                                const SizedBox(width: Espacio.s1),
                                Text(
                                  TextosBilletera.saldoDisponible.toUpperCase(),
                                  style: texto.labelSmall?.copyWith(
                                    color: t.sobreVerdeSolido,
                                    letterSpacing: 0.6,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: Espacio.s2),
                          Monto(
                            monto: saldo.disponible.monto,
                            moneda: saldo.disponible.moneda.value,
                            etiqueta: TextosBilletera.saldoDisponible,
                            estilo: texto.headlineMedium?.copyWith(
                              color: t.sobreVerdeSolido,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: Espacio.s2),
                  Text(
                    TextosBilletera.custodia,
                    style: texto.bodySmall?.copyWith(color: t.sobreVerdeSolido),
                  ),
                  const SizedBox(height: Espacio.s4),
                  IntrinsicHeight(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Expanded(
                          child: BotonDeDosLineas(
                            texto: TextosBilletera.verAportesPendientes,
                            fondo: t.accent,
                            frente: t.accentInk,
                            onTap: () => context.pushNamed('pasanaku.miEstado'),
                          ),
                        ),
                        const SizedBox(width: Espacio.s2),
                        Expanded(
                          child: BotonDeDosLineas(
                            texto: TextosBilletera.movimientos,
                            fondo: Colors.transparent,
                            frente: t.sobreVerdeSolido,
                            borde: t.sobreVerdeSolido,
                            onTap: () => context.pushNamed(
                              'billetera.extracto',
                              queryParameters: {'cuenta': saldo.cuentaId},
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: Espacio.s4),
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: Cifra(
                          etiqueta: TextosBilletera.saldoRetenido,
                          monto: saldo.retenido,
                          nota: hayRetenido
                              ? null
                              : TextosBilletera.nadaTrabado,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
