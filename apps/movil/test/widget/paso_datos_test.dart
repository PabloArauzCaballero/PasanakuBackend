import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// **El botón que no hacía nada.**
///
/// «Continuar» nacía deshabilitado y solo se encendía con el formulario entero
/// válido, sin decir nunca qué faltaba. Quien escribía su celular como `71000090`
/// —sin el prefijo del país— veía un botón muerto y no tenía forma de enterarse.
/// Estas pruebas fijan lo contrario: el toque siempre hace algo.
void main() {
  Future<void> abrir(WidgetTester tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(body: SingleChildScrollView(child: PasoDatos())),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets(
    'el formulario vacío no muestra errores: nadie escribió todavía',
    (tester) async {
      await abrir(tester);
      expect(find.text('Escribí al menos dos letras.'), findsNothing);
      expect(find.text('Elegí una fecha.'), findsNothing);
    },
  );

  testWidgets('tocar «Continuar» con el formulario vacío muestra qué falta', (
    tester,
  ) async {
    await abrir(tester);
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();

    // Dice qué falta en cada campo, en vez de quedarse mudo.
    expect(find.text('Escribí al menos dos letras.'), findsNWidgets(2));
    expect(
      find.text('Un celular boliviano se escribe +591 y ocho dígitos.'),
      findsOneWidget,
    );
    expect(find.text('Revisá el número de documento.'), findsOneWidget);
    expect(find.text('Elegí una fecha.'), findsOneWidget);
  });

  testWidgets('el celular lleva el prefijo puesto: se escriben ocho dígitos', (
    tester,
  ) async {
    await abrir(tester);
    // El prefijo se ve SIEMPRE, no solo con el campo enfocado.
    expect(find.text('+591'), findsOneWidget);

    await tester.enterText(find.byType(TextField).at(2), '71000090');
    await tester.pump();

    final contenedor = ProviderScope.containerOf(
      tester.element(find.byType(PasoDatos)),
    );
    expect(contenedor.read(altaProvider).datos.telefono, '+59171000090');
  });

  testWidgets('con todo completo, «Continuar» avanza de paso', (tester) async {
    await abrir(tester);
    final contenedor = ProviderScope.containerOf(
      tester.element(find.byType(PasoDatos)),
    );

    await tester.enterText(find.byType(TextField).at(0), 'Ana');
    await tester.enterText(find.byType(TextField).at(1), 'Quispe Mamani');
    await tester.enterText(find.byType(TextField).at(2), '71000090');
    await tester.enterText(find.byType(TextField).at(3), '9876543');
    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          contenedor
              .read(altaProvider)
              .datos
              .copiarCon(fechaNacimiento: DateTime(1995, 4, 12)),
        );
    await tester.pumpAndSettle();

    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.celular);
  });
}
