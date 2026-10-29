import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/consultar_puntaje.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_puntaje.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

Widget _app(PuntajeDelUsuario p, Brightness b) => ProviderScope(
  overrides: [puntajeProvider('u1').overrideWith((ref) async => p)],
  child: MaterialApp(
    theme: temaDesde(b == Brightness.dark ? Tokens.oscuro : Tokens.claro, b),
    home: const PantallaMiPuntaje(usuarioId: 'u1'),
  ),
);

void main() {
  for (final b in Brightness.values) {
    testWidgets(
      'con historial muestra el número y el nivel en claro (${b.name})',
      (tester) async {
        await tester.pumpWidget(
          _app(
            PuntajeDelUsuario(
              tieneHistorial: true,
              puntaje: '720',
              nivelDeConfianza: 'EN_CONSTRUCCION',
            ),
            b,
          ),
        );
        await tester.pumpAndSettle();
        expect(find.text('720'), findsOneWidget);
        expect(find.text('En construccion'), findsOneWidget);
        expect(
          find.textContaining('servicio de transparencia'),
          findsOneWidget,
        );
      },
    );
  }

  testWidgets('sin historial no es un error ni un cero: orienta', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(
        PuntajeDelUsuario(
          tieneHistorial: false,
          puntaje: '0',
          nivelDeConfianza: 'SIN_HISTORIAL',
        ),
        Brightness.light,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Recién empezás'), findsOneWidget);
    expect(find.textContaining('no es una sanción'), findsOneWidget);
    expect(find.text('0'), findsNothing);
  });

  testWidgets('un puntaje que no es entero se muestra tal cual', (
    tester,
  ) async {
    await tester.pumpWidget(
      _app(
        PuntajeDelUsuario(
          tieneHistorial: true,
          puntaje: 'n/d',
          nivelDeConfianza: 'ALTO',
        ),
        Brightness.light,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('n/d'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
