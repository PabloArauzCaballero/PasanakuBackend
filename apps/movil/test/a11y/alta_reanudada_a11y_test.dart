import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/borrador_de_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('alta reanudada al 200 % · ${brillo.name} cumple a11y', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final almacen = AlmacenEnMemoria();
      await BorradorDeAlta(almacen).guardar(
        const EstadoAlta(
          paso: PasoAlta.contrato,
          datos: DatosPersonales(
            nombres: 'Demo',
            apellidos: 'Prueba',
            telefono: '+59170000000',
          ),
        ),
      );
      final (:dio, adaptador: _) = dioSimulado();
      await tester.pumpWidget(
        conApp(
          const PantallaDeRegistro(),
          dio: dio,
          brillo: brillo,
          escalaDeTexto: 2,
          almacenSeguro: almacen,
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text(TextosIdentidad.registroReanudado), findsOneWidget);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));

      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      await tester.tap(find.byType(BackButton));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byType(SelectorSegmentado<String>));
      await tester.pumpAndSettle();
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      expect(tester.takeException(), isNull);
    });
  }
}
