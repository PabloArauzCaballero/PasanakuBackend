import 'package:flutter/material.dart';

import '../atomos/monto.dart';
import '../tokens/tokens.dart';

/// El encabezado verde con el saldo: disponible en grande, retenido y en pasanakus
/// como líneas, y **una** acción naranja.
class TarjetaSaldo extends StatelessWidget {
  const TarjetaSaldo({
    super.key,
    required this.disponible,
    required this.moneda,
    this.retenido,
    this.enPasanakus,
    this.accionPrincipal,
    this.accionSecundaria,
  });

  final String disponible;
  final String moneda;
  final String? retenido;
  final String? enPasanakus;
  final Widget? accionPrincipal;
  final Widget? accionSecundaria;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(Espacio.s5),
      decoration: BoxDecoration(
        color: t.verdeSolido,
        borderRadius: BorderRadius.circular(Radios.lg),
        boxShadow: [t.sombra2],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Saldo disponible',
            style: texto.labelLarge?.copyWith(color: t.sobreVerdeSolido),
          ),
          const SizedBox(height: Espacio.s1),
          Monto(
            monto: disponible,
            moneda: moneda,
            etiqueta: 'Saldo disponible',
            permitirCorteEntreGrupos: true,
            estilo: texto.headlineMedium?.copyWith(
              color: t.sobreVerdeSolido,
              fontWeight: FontWeight.w700,
            ),
          ),
          if (retenido != null || enPasanakus != null)
            const SizedBox(height: Espacio.s3),
          if (retenido != null)
            _LineaDeSaldo(
              etiqueta: 'Retenido',
              monto: retenido!,
              moneda: moneda,
            ),
          if (enPasanakus != null)
            _LineaDeSaldo(
              etiqueta: 'En pasanakus',
              monto: enPasanakus!,
              moneda: moneda,
            ),
          if (accionPrincipal != null || accionSecundaria != null) ...[
            const SizedBox(height: Espacio.s4),
            _AccionesDeTarjetaSaldo(
              principal: accionPrincipal,
              secundaria: accionSecundaria,
            ),
          ],
        ],
      ),
    );
  }
}

class _LineaDeSaldo extends StatelessWidget {
  const _LineaDeSaldo({
    required this.etiqueta,
    required this.monto,
    required this.moneda,
  });

  final String etiqueta;
  final String monto;
  final String moneda;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final estilo = Theme.of(
      context,
    ).textTheme.bodySmall?.copyWith(color: t.sobreVerdeSolido);
    final cifra = Monto(
      monto: monto,
      moneda: moneda,
      etiqueta: etiqueta,
      estilo: estilo,
    );
    return LayoutBuilder(
      builder: (context, limites) {
        final escala = MediaQuery.textScalerOf(context).scale(16) / 16;
        if (limites.maxWidth < 220 * escala) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(etiqueta, style: estilo),
              cifra,
            ],
          );
        }
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded(child: Text(etiqueta, style: estilo)),
            cifra,
          ],
        );
      },
    );
  }
}

class _AccionesDeTarjetaSaldo extends StatelessWidget {
  const _AccionesDeTarjetaSaldo({this.principal, this.secundaria});

  final Widget? principal;
  final Widget? secundaria;

  @override
  Widget build(BuildContext context) => LayoutBuilder(
    builder: (context, limites) {
      final escala = MediaQuery.textScalerOf(context).scale(16) / 16;
      final apilar = limites.maxWidth < 280 * escala;
      if (apilar) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            ?principal,
            if (principal != null && secundaria != null)
              const SizedBox(height: Espacio.s2),
            ?secundaria,
          ],
        );
      }
      return Row(
        children: [
          if (principal != null) Expanded(child: principal!),
          if (principal != null && secundaria != null)
            const SizedBox(width: Espacio.s3),
          if (secundaria != null) Expanded(child: secundaria!),
        ],
      );
    },
  );
}
