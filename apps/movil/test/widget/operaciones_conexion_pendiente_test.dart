import 'dart:async';

import 'package:aportaya_movil/dominio/puertos/conectividad.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_retirar.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_transferir.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_aportar.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _ConexionPendiente implements Conectividad {
  final _primera = Completer<bool>();
  final _cambios = StreamController<bool>.broadcast();

  @override
  Future<bool> hayConexion() => _primera.future;

  @override
  Stream<bool> get cambios => _cambios.stream;

  void completar(bool valor) => _primera.complete(valor);
  void establecer(bool valor) => _cambios.add(valor);
  Future<void> cerrar() => _cambios.close();
}

class _SondaFallaPrimero implements Conectividad {
  var llamadas = 0;

  @override
  Future<bool> hayConexion() async {
    llamadas++;
    if (llamadas == 1) throw StateError('sonda');
    return true;
  }

  @override
  Stream<bool> get cambios => const Stream<bool>.empty();
}

void main() {
  const cuenta = '11111111-1111-4111-8111-111111111111';

  testWidgets('saldo pausa acciones durante la primera sonda', (tester) async {
    final conexion = _ConexionPendiente();
    addTearDown(conexion.cerrar);
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      '/billetera/$cuenta/saldo',
      (servidor) => servidor.reply(
        200,
        ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
      ),
    );
    await tester.pumpWidget(
      conApp(
        const PantallaDeSaldo(cuentaId: cuenta),
        dio: dio,
        conectividad: conexion,
      ),
    );
    await asentar(tester);
    expect(find.text('Comprobando conexión'), findsOneWidget);
    final recargar = find.widgetWithText(FilledButton, 'Recargar');
    await tester.ensureVisible(recargar);
    await tester.pumpAndSettle();
    expect(tester.widget<FilledButton>(recargar).onPressed, isNull);

    conexion.completar(true);
    await tester.pumpAndSettle();
    expect(find.text('Comprobando conexión'), findsNothing);
    expect(tester.widget<FilledButton>(recargar).onPressed, isNotNull);
    expect(tester.takeException(), isNull);
  });

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
    testWidgets('$nombre espera red confirmada y callback viejo no envía', (
      tester,
    ) async {
      final conexion = _ConexionPendiente();
      addTearDown(conexion.cerrar);
      final (:dio, adaptador: _) = dioSimulado();
      var envios = 0;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, manejador) {
            if (opciones.method == 'POST') envios++;
            manejador.next(opciones);
          },
        ),
      );
      await tester.pumpWidget(
        conApp(pantalla, dio: dio, conectividad: conexion),
      );
      await tester.pump();
      expect(find.text('Comprobando conexión'), findsOneWidget);
      expect(find.text('Sin conexión'), findsNothing);
      for (final (indice, dato) in datos.indexed) {
        await tester.enterText(find.byType(TextField).at(indice), dato);
      }
      final boton = find.widgetWithText(FilledButton, nombre);
      await tester.ensureVisible(boton);
      await tester.pumpAndSettle();
      expect(tester.widget<FilledButton>(boton).onPressed, isNull);
      expect(envios, 0);

      conexion.completar(true);
      await tester.pumpAndSettle();
      expect(find.text('Comprobando conexión'), findsNothing);
      final callbackAnterior = tester.widget<FilledButton>(boton).onPressed;
      expect(callbackAnterior, isNotNull);

      conexion.establecer(false);
      await tester.pumpAndSettle();
      expect(find.text('Sin conexión'), findsOneWidget);
      expect(tester.widget<FilledButton>(boton).onPressed, isNull);
      callbackAnterior!();
      await tester.pump();
      expect(envios, 0);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('aporte permite revisar, pero no confirmar mientras sondea', (
    tester,
  ) async {
    final conexion = _ConexionPendiente();
    addTearDown(conexion.cerrar);
    final (:dio, adaptador: _) = dioSimulado();
    var envios = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, manejador) {
          if (opciones.method == 'POST') envios++;
          manejador.next(opciones);
        },
      ),
    );
    await tester.pumpWidget(
      conApp(
        const PantallaAportar(
          obligacionId: '68a98585-2854-4b71-80fd-5ef490fe60f6',
          montoDelPeriodo: '125.00',
        ),
        dio: dio,
        conectividad: conexion,
      ),
    );
    await tester.pump();
    expect(find.text('Comprobando conexión'), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'REF-PENDIENTE');
    final revisar = find.widgetWithText(FilledButton, 'Revisar aporte');
    await tester.ensureVisible(revisar);
    await tester.pumpAndSettle();
    await tester.tap(revisar);
    await tester.pumpAndSettle();
    final confirmar = find.widgetWithText(FilledButton, 'Confirmar aporte');
    expect(tester.widget<FilledButton>(confirmar).onPressed, isNull);
    expect(find.text('REF-PENDIENTE'), findsOneWidget);
    expect(envios, 0);

    conexion.completar(true);
    await tester.pumpAndSettle();
    expect(tester.widget<FilledButton>(confirmar).onPressed, isNotNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets('fallo de sonda ofrece reintento y habilita al recuperar', (
    tester,
  ) async {
    final conexion = _SondaFallaPrimero();
    await tester.pumpWidget(
      conApp(
        const PantallaRecargar(cuentaId: cuenta),
        dio: dioSimulado().dio,
        conectividad: conexion,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('No pudimos comprobar la conexión'), findsOneWidget);
    expect(find.text('Volver a comprobar'), findsOneWidget);
    await tester.tap(find.text('Volver a comprobar'));
    await tester.pumpAndSettle();
    expect(conexion.llamadas, 2);
    expect(find.text('No pudimos comprobar la conexión'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
