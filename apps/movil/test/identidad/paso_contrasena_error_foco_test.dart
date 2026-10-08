import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_contrasena.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_del_alta.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Future<void> montarConTeclado(WidgetTester tester) async {
  tester.view.physicalSize = const Size(320, 760);
  tester.view.devicePixelRatio = 1;
  tester.view.viewInsets = const FakeViewPadding(bottom: 300);
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    ProviderScope(
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(
            context,
          ).copyWith(textScaler: const TextScaler.linear(2)),
          child: child!,
        ),
        home: const Scaffold(
          body: SafeArea(child: SingleChildScrollView(child: PasoContrasena())),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  for (final caso in [
    (
      clave: 'corta',
      repetida: 'corta',
      indice: 0,
      error: TextosDelAlta.claveCorta,
    ),
    (
      clave: 'clave-sintetica-larga',
      repetida: 'otra-clave-sintetica',
      indice: 1,
      error: TextosDelAlta.clavesNoCoinciden,
    ),
  ]) {
    testWidgets(
      'el error de contraseña ${caso.indice} reaparece sobre teclado',
      (tester) async {
        await montarConTeclado(tester);
        final campos = find.byType(TextField);
        await tester.enterText(campos.at(0), caso.clave);
        await tester.enterText(campos.at(1), caso.repetida);
        await tester.ensureVisible(campos.at(caso.indice));
        await tester.pumpAndSettle();
        await tester.tap(campos.at(caso.indice));
        await tester.pumpAndSettle();

        final continuar = find.text(TextosIdentidad.continuar);
        await tester.ensureVisible(continuar);
        await tester.pumpAndSettle();
        expect(tester.getTopLeft(campos.at(caso.indice)).dy, lessThan(0));

        await tester.tap(continuar);
        await tester.pumpAndSettle();

        final error = find.text(caso.error);
        expect(error, findsOneWidget);
        expect(
          tester.getTopLeft(campos.at(caso.indice)).dy,
          greaterThanOrEqualTo(80),
        );
        expect(tester.getBottomRight(error).dy, lessThanOrEqualTo(460));
        expect(
          tester.widget<TextField>(campos.at(0)).controller?.text,
          caso.clave,
        );
        expect(
          tester.widget<TextField>(campos.at(1)).controller?.text,
          caso.repetida,
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
}
