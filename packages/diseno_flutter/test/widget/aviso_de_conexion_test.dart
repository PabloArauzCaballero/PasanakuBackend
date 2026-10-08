import 'package:aportaya_diseno/organismos/aviso_de_conexion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets(
      'no llama offline a la sonda pendiente y permite reintentar ($brillo)',
      (tester) async {
        pantallaChica(tester);
        var reintentos = 0;
        Widget aviso(AsyncValue<bool> estado) => conTema(
          AvisoDeConexion(
            estado: estado,
            detalleSinConexion: 'El envío está pausado.',
            reintentar: () => reintentos++,
          ),
          brillo: brillo,
          escala: 2,
        );

        await tester.pumpWidget(aviso(const AsyncLoading<bool>()));
        expect(find.text('Comprobando conexión'), findsOneWidget);
        expect(find.text('Sin conexión'), findsNothing);
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        expect(tester.takeException(), isNull);

        await tester.pumpWidget(
          aviso(AsyncError<bool>(StateError('sonda'), StackTrace.empty)),
        );
        expect(find.text('No pudimos comprobar la conexión'), findsOneWidget);
        await tester.tap(find.text('Volver a comprobar'));
        expect(reintentos, 1);
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));

        await tester.pumpWidget(aviso(const AsyncData(false)));
        expect(find.text('Sin conexión'), findsOneWidget);
        expect(find.text('El envío está pausado.'), findsOneWidget);
        await tester.pumpWidget(aviso(const AsyncData(true)));
        expect(find.text('Sin conexión'), findsNothing);
        expect(find.text('Comprobando conexión'), findsNothing);
        expect(find.text('No pudimos comprobar la conexión'), findsNothing);
      },
    );
  }
}
