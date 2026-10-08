import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('Atrás del sistema retrocede un paso antes de salir del alta', (
    tester,
  ) async {
    final contenedor = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria())],
    );
    addTearDown(contenedor.dispose);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: TextButton(
              onPressed: () =>
                  Navigator.of(tester.element(find.text('Abrir'))).push(
                    MaterialPageRoute<void>(
                      builder: (_) => const PantallaDeRegistro(),
                    ),
                  ),
              child: const Text('Abrir'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    final alta = contenedor.read(altaProvider.notifier);
    alta.actualizarDatos(const DatosPersonales(nombres: 'Demo'));
    alta.siguiente();
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrasena);
    alta.siguiente();
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.celular);

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(find.byType(PantallaDeRegistro), findsOneWidget);
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrasena);

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(find.byType(PantallaDeRegistro), findsOneWidget);
    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    expect(contenedor.read(altaProvider).datos.nombres, 'Demo');

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    expect(find.byType(PantallaDeRegistro), findsNothing);
    expect(find.text('Abrir'), findsOneWidget);
    await tester.pump(const Duration(milliseconds: 450));
  });
}
