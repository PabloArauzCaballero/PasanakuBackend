import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_contrasena.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Paso 2 del alta — la contraseña.
///
/// **Por qué existe este paso.** El alta pedía ocho cosas y ninguna era una
/// contraseña: la cuenta nacía sin credencial y, al terminar, el formulario de ingreso
/// pedía una clave que nunca se había elegido. Se completaban ocho pasos para quedar
/// afuera.
void main() {
  Future<ProviderContainer> montar(WidgetTester tester) async {
    // Este paso no toca la red ni el llavero: solo lee y escribe el estado del alta.
    final contenedor = ProviderContainer(retry: (r, e) => null);
    addTearDown(contenedor.dispose);
    // Los datos personales ya cargados: la política rechaza claves derivadas de ellos
    // y sin esto no habría contra qué comprobarlo.
    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          const DatosPersonales(
            nombres: 'Rosa',
            apellidos: 'Quispe',
            telefono: '+59178123456',
            numeroDocumento: '1234567',
          ),
        );
    // El asistente parado donde corresponde: montar el paso sin mover el estado haría
    // que un «Continuar» válido avanzara de `datos` a `contrasena` y la prueba mediría
    // otra cosa.
    contenedor.read(altaProvider.notifier).siguiente();
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          // Con el tema real: el paso lee `Tokens.of(context)`, y sin tema el `of`
          // revienta antes de dibujar nada.
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(
            body: SingleChildScrollView(child: PasoContrasena()),
          ),
        ),
      ),
    );
    await asentar(tester);
    return contenedor;
  }

  Future<void> escribir(
    WidgetTester tester,
    String clave,
    String repetida,
  ) async {
    final campos = find.byType(TextField);
    await tester.enterText(campos.at(0), clave);
    await tester.enterText(campos.at(1), repetida);
    await tester.pump();
    await tester.tap(find.text('Continuar'));
    await tester.pump();
  }

  testWidgets('una clave de ocho o más, repetida bien, avanza de paso', (
    tester,
  ) async {
    final c = await montar(tester);

    await escribir(tester, 'una-clave-larga-2026', 'una-clave-larga-2026');

    expect(c.read(altaProvider).contrasena, 'una-clave-larga-2026');
    expect(c.read(altaProvider).paso, PasoAlta.celular);
  });

  testWidgets('una clave corta no avanza y dice por qué', (tester) async {
    final c = await montar(tester);

    await escribir(tester, 'corta', 'corta');

    expect(c.read(altaProvider).paso, PasoAlta.contrasena);
    expect(find.textContaining('ocho'), findsWidgets);
  });

  testWidgets('si las dos no coinciden, no avanza', (tester) async {
    final c = await montar(tester);

    await escribir(tester, 'una-clave-larga-2026', 'una-clave-larga-2027');

    expect(c.read(altaProvider).paso, PasoAlta.contrasena);
    expect(find.textContaining('no coinciden'), findsOneWidget);
  });

  testWidgets(
    'una clave que lleva el celular adentro se rechaza acá, no en el servidor',
    (tester) async {
      final c = await montar(tester);

      // La misma regla que aplica `PoliticaDeClave`. Gastar un viaje al servidor para
      // enterarse al final del alta sería el peor momento para descubrirlo.
      await escribir(tester, 'mi78123456segura', 'mi78123456segura');

      expect(c.read(altaProvider).paso, PasoAlta.contrasena);
      expect(find.textContaining('celular'), findsWidgets);
    },
  );
}
