import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_datos.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<ProviderContainer> abrir(WidgetTester tester) async {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(body: SingleChildScrollView(child: PasoDatos())),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return contenedor;
  }

  testWidgets('identidad valida nombres y fecha, sin mezclar contacto ni CI', (
    tester,
  ) async {
    final contenedor = await abrir(tester);
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continuar'));
    await tester.pumpAndSettle();

    expect(find.text('Escribí tu primer nombre.'), findsOneWidget);
    expect(find.text('Escribí al menos un apellido.'), findsOneWidget);
    expect(find.text('Elegí una fecha.'), findsOneWidget);
    expect(find.text('Núm. CI'), findsNothing);
    expect(find.text('Correo electrónico'), findsNothing);
    expect(contenedor.read(altaProvider).paso, PasoAlta.cuenta);
  });

  testWidgets(
    'al completar identidad avanza a actividad y conserva los datos',
    (tester) async {
      final contenedor = ProviderContainer();
      addTearDown(contenedor.dispose);
      contenedor.read(altaProvider.notifier).siguiente();
      contenedor.read(altaProvider.notifier).siguiente();
      contenedor.read(altaProvider.notifier).siguiente();
      contenedor
          .read(altaProvider.notifier)
          .actualizarDatos(
            contenedor
                .read(altaProvider)
                .datos
                .copiarCon(fechaNacimiento: DateTime(1995, 4, 12)),
          );
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: contenedor,
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: const Scaffold(
              body: SingleChildScrollView(child: PasoDatos()),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField).at(0), 'Ana');
      await tester.enterText(find.byType(TextField).at(2), 'Quispe');
      await tester.enterText(find.byType(TextField).at(3), 'Mamani');
      expect(contenedor.read(altaProvider).datos.primerNombre, 'Ana');

      await tester.ensureVisible(find.text('Continuar'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Continuar'));
      await tester.pumpAndSettle();

      expect(contenedor.read(altaProvider).paso, PasoAlta.actividad);
      expect(contenedor.read(altaProvider).datos.apellidos, 'Quispe Mamani');
      expect(
        contenedor.read(altaProvider).datos.fechaNacimiento,
        DateTime(1995, 4, 12),
      );
    },
  );
}
