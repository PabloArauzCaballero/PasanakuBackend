import 'package:aportaya_diseno/catalogo/catalogo.dart';
import 'package:aportaya_diseno/catalogo/muestras.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Accesibilidad bloqueante sobre **todo el catálogo**: área táctil, etiquetas y
/// contraste, en claro y en oscuro. Una pieza que no pasa acá no entra al sistema.
void main() {
  for (final grupo in catalogo) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('${grupo.grupo} · ${brillo.name} cumple las guías', (
        tester,
      ) async {
        pantallaChica(tester);
        await tester.pumpWidget(
          conTema(
            PaginaDeCatalogo(grupo: grupo),
            brillo: brillo,
            pantallaEntera: true,
          ),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 300));
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
      });
    }
  }
}
