import 'dart:async';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/billetera/acciones_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:aportaya_movil/pantallas/billetera/textos.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class ConexionControlada implements Conectividad {
  final _cambios = StreamController<bool>.broadcast();

  @override
  Stream<bool> get cambios => _cambios.stream;

  @override
  Future<bool> hayConexion() async => true;

  void establecer(bool valor) => _cambios.add(valor);

  Future<void> cerrar() => _cambios.close();
}

Widget _app(
  Widget pantalla,
  Dio dio,
  ConexionControlada conexion, {
  Brightness brillo = Brightness.light,
  double escala = 1,
}) => ProviderScope(
  retry: (retryCount, error) => null,
  overrides: [
    almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
    dioProvider.overrideWithValue(dio),
    conectividadProvider.overrideWithValue(conexion),
  ],
  child: MaterialApp(
    theme: temaDesde(Tokens.claro, Brightness.light),
    darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
    themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
    builder: (context, child) => MediaQuery(
      data: MediaQuery.of(
        context,
      ).copyWith(textScaler: TextScaler.linear(escala)),
      child: child!,
    ),
    home: pantalla,
  ),
);

void main() {
  const cuenta = '11111111-1111-4111-8111-111111111111';

  testWidgets(
    'saldo cargado se marca offline y pausa acciones hasta reconectar',
    (tester) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);
      final conexion = ConexionControlada();
      addTearDown(conexion.cerrar);
      final simulado = dioSimulado();
      simulado.adaptador.onGet(
        '/billetera/$cuenta/saldo',
        (s) => s.reply(
          200,
          ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
        ),
      );
      await tester.pumpWidget(
        _app(
          const PantallaDeSaldo(cuentaId: cuenta),
          simulado.dio,
          conexion,
          brillo: Brightness.dark,
          escala: 2,
        ),
      );
      await asentar(tester);
      expect(find.text('Sin conexión'), findsNothing);
      expect(
        tester
            .widget<FilledButton>(find.widgetWithText(FilledButton, 'Recargar'))
            .onPressed,
        isNotNull,
      );

      conexion.establecer(false);
      await tester.pumpAndSettle();
      expect(find.text('Sin conexión'), findsOneWidget);
      expect(find.text(TextosBilletera.saldoSinConexion), findsOneWidget);
      await tester.scrollUntilVisible(
        find.text('Recargar'),
        220,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.pumpAndSettle();
      final botones = tester.widgetList<FilledButton>(
        find.descendant(
          of: find.byType(AccionesDeSaldo, skipOffstage: false),
          matching: find.byType(FilledButton, skipOffstage: false),
          skipOffstage: false,
        ),
      );
      expect(botones.length, 3);
      expect(botones.every((boton) => boton.onPressed == null), isTrue);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      expect(tester.takeException(), isNull);

      conexion.establecer(true);
      await tester.pumpAndSettle();
      expect(find.text('Sin conexión'), findsNothing);
      expect(
        tester
            .widget<FilledButton>(find.widgetWithText(FilledButton, 'Recargar'))
            .onPressed,
        isNotNull,
      );
    },
  );

  for (final (nombre, pantalla, datos) in [
    ('Recargar', const PantallaRecargar(cuentaId: cuenta), ['10,00']),
    (
      'Retirar',
      const PantallaRetirar(cuentaId: cuenta),
      ['10,00', '22222222-2222-4222-8222-222222222222', '123456'],
    ),
    (
      'Transferir',
      const PantallaTransferir(cuentaOrigenId: cuenta),
      ['destinatario', '10,00', 'aporte'],
    ),
  ]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('$nombre no envía offline a 200 % ($brillo)', (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final conexion = ConexionControlada();
        addTearDown(conexion.cerrar);
        final simulado = dioSimulado();
        var peticiones = 0;
        simulado.dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (opciones, manejador) {
              if (opciones.method == 'POST') peticiones++;
              manejador.next(opciones);
            },
          ),
        );
        await tester.pumpWidget(
          _app(pantalla, simulado.dio, conexion, brillo: brillo, escala: 2),
        );
        await asentar(tester);
        final campos = find.byType(TextField);
        for (var i = 0; i < datos.length; i++) {
          await tester.enterText(campos.at(i), datos[i]);
        }
        await tester.pumpAndSettle();
        final boton = find.widgetWithText(FilledButton, nombre);
        expect(tester.widget<FilledButton>(boton).onPressed, isNotNull);

        conexion.establecer(false);
        await tester.pumpAndSettle();
        expect(find.text('Sin conexión'), findsOneWidget);
        expect(find.text(TextosBilletera.operacionSinConexion), findsOneWidget);
        expect(tester.widget<FilledButton>(boton).onPressed, isNull);
        await tester.ensureVisible(boton);
        await tester.pumpAndSettle();
        expect(tester.getBottomRight(boton).dy, lessThanOrEqualTo(760));
        expect(peticiones, 0);
        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
        expect(tester.takeException(), isNull);
      });
    }
  }
}
