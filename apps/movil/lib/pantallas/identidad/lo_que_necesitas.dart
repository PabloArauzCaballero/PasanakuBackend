import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';

import 'textos_de_portada.dart';

/// Qué hay que tener a mano para el alta, dicho en la bienvenida.
///
/// Quien empieza el alta sin el carnet cerca la abandona en el paso del documento.
/// Decirlo antes de «Crear mi cuenta» quita la sorpresa: la persona decide con lo que
/// le espera a la vista, no a mitad del camino.
class LoQueNecesitas extends StatelessWidget {
  const LoQueNecesitas({super.key});

  static const _items = <({IconData icono, String texto})>[
    (icono: Icons.badge_outlined, texto: TextosDePortada.necesitasCarnet),
    (
      icono: Icons.smartphone_outlined,
      texto: TextosDePortada.necesitasContacto,
    ),
    (icono: Icons.wb_sunny_outlined, texto: TextosDePortada.necesitasLuz),
  ];

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      container: true,
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: t.surface,
          borderRadius: BorderRadius.circular(Radios.lg),
          border: Border.all(color: t.border, width: Borde.fino),
        ),
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Semantics(
                header: true,
                child: Text(
                  TextosDePortada.necesitasTitulo,
                  style: Tipo.cuerpoFuerte.copyWith(color: t.text),
                ),
              ),
              for (final item in _items) ...[
                const SizedBox(height: Espacio.s3),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(item.icono, size: Espacio.s5, color: t.brandTexto),
                    const SizedBox(width: Espacio.s3),
                    Expanded(
                      child: Text(
                        item.texto,
                        style: Tipo.cuerpoChico.copyWith(color: t.text2),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
