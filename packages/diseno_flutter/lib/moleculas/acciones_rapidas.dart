import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Recargar · Retirar · Vales · Aportes: cuatro accesos, cada uno con ícono y nombre.
class AccionesRapidas extends StatelessWidget {
  const AccionesRapidas({super.key, required this.acciones});
  final List<({String texto, IconData icono, VoidCallback onTap})> acciones;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Row(
      children: [
        for (final a in acciones)
          Expanded(
            child: Semantics(
              button: true,
              label: a.texto,
              excludeSemantics: true,
              child: InkWell(
                onTap: a.onTap,
                borderRadius: BorderRadius.circular(Radios.md),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
                  child: Column(
                    children: [
                      Container(
                        width: Tactil.minimo,
                        height: Tactil.minimo,
                        decoration: BoxDecoration(
                          color: t.brandBg,
                          borderRadius: BorderRadius.circular(Radios.md),
                        ),
                        child: Icon(a.icono, color: t.brandTexto),
                      ),
                      const SizedBox(height: Espacio.s1),
                      Text(
                        a.texto,
                        style: texto.labelMedium?.copyWith(color: t.text2),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
      ],
    );
  }
}
