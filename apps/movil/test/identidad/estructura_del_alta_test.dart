import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/transicion_de_paso_alta.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  testWidgets('el cambio de paso usa el eje y duraciones de entrada/salida', (
    tester,
  ) async {
    final dio = dioSimulado().dio;
    await tester.pumpWidget(conApp(const PantallaDeRegistro(), dio: dio));
    final transicion = tester.widget<AnimatedSwitcher>(
      find.descendant(
        of: find.byType(TransicionDePasoAlta),
        matching: find.byType(AnimatedSwitcher),
      ),
    );
    expect(transicion.duration, const Duration(milliseconds: 300));
    expect(transicion.reverseDuration, const Duration(milliseconds: 240));
  });

  test('los seis pasos siguen la secuencia Atlas y quedan en cuatro fases', () {
    expect(PasoAlta.values, [
      PasoAlta.cuenta,
      PasoAlta.correo,
      PasoAlta.documento,
      PasoAlta.datos,
      PasoAlta.actividad,
      PasoAlta.contrato,
    ]);
    expect(PasoAlta.values.map((paso) => paso.fase).toList(), [
      FaseAlta.cuenta,
      FaseAlta.cuenta,
      FaseAlta.identidad,
      FaseAlta.datos,
      FaseAlta.datos,
      FaseAlta.contrato,
    ]);
    expect(const EstadoAlta().paso, PasoAlta.cuenta);
  });

  testWidgets('la cabecera muestra fase, total y nombre de Atlas', (
    tester,
  ) async {
    final dio = dioSimulado().dio;
    await tester.pumpWidget(conApp(const PantallaDeRegistro(), dio: dio));

    expect(find.text('FASE 1 DE 4'), findsOneWidget);
    expect(find.text('Tu cuenta'), findsOneWidget);
    expect(find.text('CÓMO TE CONTACTAMOS'), findsOneWidget);
    expect(find.text('CÓMO ENTRÁS'), findsOneWidget);
  });

  testWidgets('atrás conserva los campos que viven en fases distintas', (
    tester,
  ) async {
    final dio = dioSimulado().dio;
    final contenedor = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaDeRegistro(),
        ),
      ),
    );

    // Cuenta reúne contacto y contraseña, y escribe de inmediato al estado del alta.
    await tester.enterText(find.byType(TextField).at(0), '71000090');
    await tester.enterText(find.byType(TextField).at(1), 'rosa@example.com');
    await tester.enterText(find.byType(TextField).at(2), 'clave-segura-2026');
    await tester.enterText(find.byType(TextField).at(3), 'clave-segura-2026');
    await tester.pump();
    expect(contenedor.read(altaProvider).datos.telefono, '+59171000090');
    expect(contenedor.read(altaProvider).datos.correo, 'rosa@example.com');
    expect(contenedor.read(altaProvider).contrasena, 'clave-segura-2026');

    final notifier = contenedor.read(altaProvider.notifier);
    notifier.siguiente(); // correo
    await tester.pumpAndSettle();
    expect(find.text('FASE 1 DE 4'), findsOneWidget);
    expect(contenedor.read(altaProvider).paso, PasoAlta.correo);
    expect(find.text('CONFIRMAR CORREO'), findsOneWidget);
    notifier.siguiente(); // documento
    await tester.pumpAndSettle();
    expect(find.text('FASE 2 DE 4'), findsOneWidget);
    await tester.enterText(find.byType(TextField).first, '9876543');
    notifier.actualizarDatos(
      contenedor
          .read(altaProvider)
          .datos
          .copiarCon(
            lugarExpedicion: 'LP',
            fechaExpiracion: DateTime(2030, 4, 12),
            fechaNacimiento: DateTime(1995, 4, 12),
          ),
    );
    await tester.pumpAndSettle();

    notifier.siguiente(); // datos personales y cotejo
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    await tester.enterText(find.byType(TextField).at(0), 'Rosa');
    await tester.enterText(find.byType(TextField).at(2), 'Quispe');
    await tester.enterText(find.byType(TextField).at(3), 'Mamani');
    await tester.pump();
    expect(contenedor.read(altaProvider).datos.primerNombre, 'Rosa');
    notifier.siguiente(); // actividad
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.actividad);
    expect(find.text('FASE 3 DE 4'), findsOneWidget);
    expect(find.text('Tus datos'), findsOneWidget);

    notifier.siguiente(); // contrato
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);
    expect(find.text('FASE 4 DE 4'), findsOneWidget);
    expect(find.text('Contrato'), findsOneWidget);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.actividad);

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller!.text,
      'Rosa',
    );
    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.documento);
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller!.text,
      '9876543',
    );
    expect(contenedor.read(altaProvider).datos.lugarExpedicion, 'LP');

    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(BackButton));
    await tester.pumpAndSettle();
    expect(contenedor.read(altaProvider).paso, PasoAlta.cuenta);
    final camposCuenta = tester
        .widgetList<TextField>(find.byType(TextField))
        .toList();
    expect(camposCuenta[0].controller!.text, '71000090');
    expect(camposCuenta[1].controller!.text, 'rosa@example.com');
    expect(camposCuenta[2].controller!.text, 'clave-segura-2026');
    expect(camposCuenta[3].controller!.text, 'clave-segura-2026');
  });
}
