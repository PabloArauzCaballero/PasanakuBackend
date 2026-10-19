import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/boton_consultar_estado.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/consultar_estado_de_cuenta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/verificacion_diferida.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_verificacion.dart';
import 'package:aportaya_movil/proveedores/renovar_acceso.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// «Consultar estado» (2026-10-19): la persona pregunta si ya la aprobaron sin salir
/// de donde está. La consulta al servidor se reemplaza por un doble.
void main() {
  group('la consulta', () {
    Future<ResultadoDeConsulta> consultar(
      EstadoDeCuenta estado, {
      bool renueva = true,
      List<String>? llamadas,
    }) {
      final contenedor = ProviderContainer(
        overrides: [
          estadoDeCuentaProvider.overrideWith((ref) async => estado),
          renovarAccesoProvider.overrideWithValue(() async {
            llamadas?.add('renovar');
            return renueva;
          }),
        ],
      );
      addTearDown(contenedor.dispose);
      return contenedor.read(consultarEstadoDeCuentaProvider)();
    }

    test('correcto: aprobada → renueva el acceso y queda activada', () async {
      final llamadas = <String>[];
      expect(
        await consultar(EstadoDeCuenta.verificada, llamadas: llamadas),
        ResultadoDeConsulta.activada,
      );
      expect(llamadas, ['renovar']);
    });

    test('límite: en revisión no renueva nada', () async {
      final llamadas = <String>[];
      expect(
        await consultar(EstadoDeCuenta.enRevision, llamadas: llamadas),
        ResultadoDeConsulta.enRevision,
      );
      expect(llamadas, isEmpty);
    });

    test('inválido: aprobada pero la renovación falla', () async {
      expect(
        await consultar(EstadoDeCuenta.verificada, renueva: false),
        ResultadoDeConsulta.aprobadaSinActivar,
      );
    });
  });

  group('el botón', () {
    Future<List<String>> montar(
      WidgetTester tester,
      Future<ResultadoDeConsulta> Function() consulta,
    ) async {
      final activaciones = <String>[];
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            consultarEstadoDeCuentaProvider.overrideWithValue(consulta),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            home: Scaffold(
              body: Center(
                child: BotonConsultarEstado(
                  alActivar: () => activaciones.add('activada'),
                ),
              ),
            ),
          ),
        ),
      );
      return activaciones;
    }

    testWidgets('correcto: aprobada → avisa y continúa', (tester) async {
      final activaciones = await montar(
        tester,
        () async => ResultadoDeConsulta.activada,
      );
      await tester.tap(find.text(TextosDeVerificacion.consultarEstado));
      await tester.pumpAndSettle();
      expect(find.text(TextosDeVerificacion.cuentaActivada), findsOneWidget);
      expect(activaciones, ['activada']);
    });

    testWidgets('límite: sigue en revisión → lo dice y no continúa', (
      tester,
    ) async {
      final activaciones = await montar(
        tester,
        () async => ResultadoDeConsulta.enRevision,
      );
      await tester.tap(find.text(TextosDeVerificacion.consultarEstado));
      await tester.pumpAndSettle();
      expect(find.text(TextosDeVerificacion.sigueEnRevision), findsOneWidget);
      expect(activaciones, isEmpty);
    });

    testWidgets('inválido: sin red → mensaje accionable, el botón vuelve', (
      tester,
    ) async {
      await montar(tester, () async => throw StateError('sin red'));
      await tester.tap(find.text(TextosDeVerificacion.consultarEstado));
      await tester.pumpAndSettle();
      expect(find.text(TextosDeVerificacion.noSePudoConsultar), findsOneWidget);
      expect(find.text(TextosDeVerificacion.consultarEstado), findsOneWidget);
    });
  });
}
