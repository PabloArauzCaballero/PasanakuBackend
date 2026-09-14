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
  Future<void> abrir(
    WidgetTester tester, {
    ProviderContainer? contenedor,
  }) async {
    final scope = contenedor ?? ProviderContainer();
    addTearDown(scope.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: scope,
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
    // Con el selector de departamento el formulario no entra en la ventana de
    // prueba: sin esto el toque cae fuera de pantalla y no pasa nada.
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();

    // Dice qué falta en cada campo, en vez de quedarse mudo.
    expect(find.text('Escribí al menos dos letras.'), findsNWidgets(2));
    expect(
      find.text('Un celular boliviano se escribe +591 y ocho dígitos.'),
      findsOneWidget,
    );
    expect(find.text('Revisá el número de documento.'), findsOneWidget);
    expect(find.text('Elegí dónde te expidieron el carnet.'), findsOneWidget);
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
    // La fecha se deja puesta ANTES de abrir: el paso lee el proveedor al construirse
    // y no lo observa, asi que inyectarla despues no llegaria al formulario. Elegirla
    // a mano seria abrir el calendario de Material, que no es lo que prueba esto.
    final contenedor = ProviderContainer();
    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          contenedor
              .read(altaProvider)
              .datos
              .copiarCon(fechaNacimiento: DateTime(1995, 4, 12)),
        );
    await abrir(tester, contenedor: contenedor);

    await tester.enterText(find.byType(TextField).at(0), 'Ana');
    await tester.enterText(find.byType(TextField).at(1), 'Quispe Mamani');
    await tester.enterText(find.byType(TextField).at(2), '71000090');
    await tester.enterText(find.byType(TextField).at(3), '9876543');
    // La extension del carnet se elige con un chip: el numero de CI se repite entre
    // departamentos y sin esto el alta no distingue a dos personas.
    await tester.ensureVisible(find.text('La Paz'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('La Paz'));
    await tester.pumpAndSettle();
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).datos.lugarExpedicion, 'LP');

    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    // Con el selector de departamento el formulario no entra en la ventana de
    // prueba: sin esto el toque cae fuera de pantalla y no pasa nada.
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.celular);
  });
}
