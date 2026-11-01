import 'package:aportaya_diseno/atomos/iconos_de_marca.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/organismos/estado_de_tramite.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart' show RenderParagraph;
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

Widget _tramite({int actual = 1, bool fallo = false, double escala = 1}) =>
    conTema(
      Scaffold(
        body: EstadoDeTramite(
          icono: IconosDeMarca.enEspera,
          tono: fallo ? Tono.error : Tono.info,
          titulo: 'Recibimos tus documentos',
          detalle: 'Un especialista está verificando tu identidad.',
          pasos: const ['Recibido', 'En revisión', 'Listo'],
          pasoActual: actual,
          fallo: fallo,
          caso: 'Caso a11ce001',
        ),
      ),
      escala: escala,
      pantallaEntera: true,
    );

/// ADR-053: dónde está el trámite se dice con texto, no solo con color ni con puntos.
void main() {
  testWidgets('las etapas se anuncian con su estado, en orden', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_tramite());
    await tester.pumpAndSettle();
    expect(
      find.bySemanticsLabel(
        RegExp(
          r'Paso 1 de 3: Recibido, hecho\. Paso 2 de 3: En revisión, en curso\. '
          r'Paso 3 de 3: Listo, pendiente',
        ),
      ),
      findsOneWidget,
    );
    expect(find.text('Caso a11ce001'), findsOneWidget);
    semantica.dispose();
  });

  testWidgets('si falló, la etapa lo dice con palabras', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_tramite(actual: 2, fallo: true));
    await tester.pumpAndSettle();
    expect(
      find.bySemanticsLabel(
        RegExp(r'Paso 3 de 3: Listo, no se pudo completar'),
      ),
      findsOneWidget,
    );
    semantica.dispose();
  });

  testWidgets('todo hecho cuando el índice llega al final', (tester) async {
    final semantica = tester.ensureSemantics();
    await tester.pumpWidget(_tramite(actual: 3));
    await tester.pumpAndSettle();
    expect(
      find.bySemanticsLabel(RegExp(r'Paso 3 de 3: Listo, hecho')),
      findsOneWidget,
    );
    semantica.dispose();
  });

  for (final escala in [1.3, 2.0]) {
    testWidgets('a 320 px y texto al ${escala * 100} % no desborda ni parte '
        'las etapas', (tester) async {
      tester.view.physicalSize = const Size(320, 900);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      await tester.pumpWidget(_tramite(escala: escala));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      for (final etapa in ['Recibido', 'En revisión', 'Listo']) {
        expect(
          tester
              .renderObject<RenderParagraph>(find.text(etapa))
              .didExceedMaxLines,
          isFalse,
          reason: '«$etapa» entra en una línea',
        );
      }
    });
  }
}
