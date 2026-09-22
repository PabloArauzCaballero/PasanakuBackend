import 'package:flutter/material.dart';
import 'package:widgetbook/widgetbook.dart';

import '../catalogo/muestras.dart';
import '../tema.dart';
import '../tokens/tokens.dart';

/// **El catálogo vivo del sistema de diseño de la app**: cada pieza de [catalogo] en
/// claro y oscuro, en dispositivo. Se abre con
/// `flutter run -t lib/catalogo.dart` desde `apps/movil`. Es la referencia para la
/// revisión visual conjunta contra `/catalogo` de Angular y contra la maqueta.
class CatalogoDeDiseno extends StatelessWidget {
  const CatalogoDeDiseno({super.key});

  @override
  Widget build(BuildContext context) {
    return Widgetbook.material(
      addons: [
        MaterialThemeAddon(
          themes: [
            WidgetbookTheme(
              name: 'Claro',
              data: temaDesde(Tokens.claro, Brightness.light),
            ),
            WidgetbookTheme(
              name: 'Oscuro',
              data: temaDesde(Tokens.oscuro, Brightness.dark),
            ),
          ],
        ),
        TextScaleAddon(min: 1, max: 2),
      ],
      directories: [
        for (final grupo in catalogo)
          WidgetbookFolder(
            name: grupo.grupo,
            children: [
              for (final m in grupo.muestras)
                WidgetbookComponent(
                  name: m.nombre,
                  useCases: [
                    WidgetbookUseCase(
                      name: 'Muestra',
                      builder: (context) => Scaffold(
                        body: SingleChildScrollView(
                          padding: const EdgeInsets.all(Espacio.s4),
                          child: m.widget(),
                        ),
                      ),
                    ),
                  ],
                ),
            ],
          ),
      ],
    );
  }
}

/// Todas las muestras de un grupo en una sola columna: lo que congelan los goldens.
class PaginaDeCatalogo extends StatelessWidget {
  const PaginaDeCatalogo({super.key, required this.grupo});
  final ({String grupo, List<Muestra> muestras}) grupo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Scaffold(
      body: ListView(
        padding: const EdgeInsets.all(Espacio.s4),
        children: [
          for (final m in grupo.muestras) ...[
            Text(
              m.nombre,
              style: Theme.of(
                context,
              ).textTheme.labelMedium?.copyWith(color: t.text3),
            ),
            const SizedBox(height: Espacio.s1),
            m.widget(),
            const SizedBox(height: Espacio.s4),
          ],
        ],
      ),
    );
  }
}
