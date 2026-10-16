import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_cuenta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_confianza.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Fase 1 del alta — contacto y contraseña antes de confirmar el correo.
///
/// La contraseña comparte el paso `cuenta` con teléfono y correo, antes de confirmar
/// el correo y completar identidad.
void main() {
  Future<ProviderContainer> montar(WidgetTester tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/usuarios/verificaciones/correo',
      (s) => s.reply(202, {
        'verificacionId': 'dddddddd-0000-4000-8000-000000000003',
        'destinoEnmascarado': 'ro***@example.com',
        'expiraEn': '2026-10-07T20:10:00Z',
      }),
      data: Matchers.any,
    );
    final contenedor = ProviderContainer(
      retry: (r, e) => null,
      overrides: [dioProvider.overrideWithValue(dio)],
    );
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
            correo: 'rosa@example.com',
          ),
        );
    // El estado inicial ya corresponde a la fase de cuenta.
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          // Con el tema real: el paso lee `Tokens.of(context)`, y sin tema el `of`
          // revienta antes de dibujar nada.
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(
            body: SingleChildScrollView(child: PasoCuenta()),
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
    await tester.enterText(campos.at(2), clave);
    await tester.enterText(campos.at(3), repetida);
    await tester.pump();
    await tester.ensureVisible(find.text('Continuar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Continuar'));
    await asentar(tester);
  }

  testWidgets('una clave de ocho o más, repetida bien, avanza de paso', (
    tester,
  ) async {
    final c = await montar(tester);
    expect(find.text('Mínimo 8 caracteres'), findsOneWidget);
    expect(find.text('Igual a la de arriba'), findsOneWidget);
    expect(find.text('CÓMO ENTRÁS'), findsOneWidget);
    expect(find.byType(TarjetaDeConfianza), findsOneWidget);

    await escribir(tester, 'una-clave-larga-2026', 'una-clave-larga-2026');

    expect(c.read(altaProvider).contrasena, 'una-clave-larga-2026');
    expect(c.read(altaProvider).paso, PasoAlta.correo);
  });

  testWidgets('una clave corta no avanza y dice por qué', (tester) async {
    final c = await montar(tester);

    await escribir(tester, 'corta', 'corta');

    expect(c.read(altaProvider).paso, PasoAlta.cuenta);
    expect(find.textContaining('ocho'), findsWidgets);
  });

  testWidgets('si las dos no coinciden, no avanza', (tester) async {
    final c = await montar(tester);

    await escribir(tester, 'una-clave-larga-2026', 'una-clave-larga-2027');

    expect(c.read(altaProvider).paso, PasoAlta.cuenta);
    expect(find.textContaining('no coinciden'), findsOneWidget);
  });

  testWidgets('el rechazo del servidor aparece y enfoca el campo de clave', (
    tester,
  ) async {
    final contenedor = ProviderContainer(
      overrides: [altaProvider.overrideWith(_AltaConErrorDeClave.new)],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const Scaffold(
            body: SingleChildScrollView(child: PasoCuenta()),
          ),
        ),
      ),
    );
    await asentar(tester);

    expect(find.textContaining('Esa contraseña no sirve'), findsOneWidget);
    expect(find.text('Referencia: traza-prueba'), findsOneWidget);
    expect(
      tester
          .widgetList<TextField>(find.byType(TextField))
          .elementAt(2)
          .focusNode
          ?.hasFocus,
      isTrue,
    );
  });

  testWidgets(
    'una clave que lleva el celular adentro se rechaza acá, no en el servidor',
    (tester) async {
      final c = await montar(tester);

      // La misma regla que aplica `PoliticaDeClave`. Gastar un viaje al servidor para
      // enterarse al final del alta sería el peor momento para descubrirlo.
      await escribir(tester, 'mi78123456segura', 'mi78123456segura');

      expect(c.read(altaProvider).paso, PasoAlta.cuenta);
      expect(find.textContaining('celular'), findsWidgets);
    },
  );
}

class _AltaConErrorDeClave extends AltaNotifier {
  @override
  EstadoAlta build() => const EstadoAlta(
    paso: PasoAlta.cuenta,
    contrasena: 'placeholder-demo',
    error: 'Esa contraseña no sirve: elegí otra.',
    campoError: CampoDeErrorApi.contrasena,
    trazaId: 'traza-prueba',
  );
}
