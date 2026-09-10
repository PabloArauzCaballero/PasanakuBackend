import 'package:flutter/material.dart';

import '../atomos/chip_estado.dart';
import '../atomos/tono.dart';
import '../tokens/tokens.dart';

/// Las etapas de cobranza, contiguas, con su canal y su tope: nadie decide a mano cuánto insistir.
class EscaleraDeEtapas extends StatelessWidget {
  const EscaleraDeEtapas({
    super.key,
    required this.etapas,
    required this.actual,
  });
  final List<({String nombre, String dias, String canales, int topeSemanal})>
  etapas;
  final int actual;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      children: [
        for (var i = 0; i < etapas.length; i += 1)
          Semantics(
            label:
                '${etapas[i].nombre}, días ${etapas[i].dias}, ${etapas[i].canales}, hasta ${etapas[i].topeSemanal} contactos por semana${i == actual ? ', etapa actual' : ''}',
            excludeSemantics: true,
            child: Container(
              margin: const EdgeInsets.only(bottom: Espacio.s1),
              padding: const EdgeInsets.all(Espacio.s3),
              decoration: BoxDecoration(
                color: i == actual
                    ? t.warnBg
                    : i < actual
                    ? t.surface2
                    : t.surface,
                borderRadius: BorderRadius.circular(Radios.md),
                border: Border.all(
                  color: i == actual ? t.warn : t.border,
                  width: Borde.fino,
                ),
              ),
              child: Row(
                children: [
                  SizedBox(
                    width: Espacio.s7,
                    child: Text(
                      etapas[i].dias,
                      style: texto.labelMedium?.copyWith(color: t.text3),
                    ),
                  ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          etapas[i].nombre,
                          style: texto.titleSmall?.copyWith(color: t.text),
                        ),
                        Text(
                          etapas[i].canales,
                          style: texto.bodySmall?.copyWith(color: t.text2),
                        ),
                      ],
                    ),
                  ),
                  ChipEstado(
                    texto: '${etapas[i].topeSemanal}/sem',
                    tono: i == actual ? Tono.aviso : Tono.neutro,
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
