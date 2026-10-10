import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Lado de quien recibe la invitación (CU-69), con el rediseño del carril H: tarjetas,
/// importe formateado, casilla del sistema de diseño y barra de acción. Cubre aceptar
/// (con la casilla como condición) y el rechazo del servidor.
const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

void main() {
  Future<void> montar(
    WidgetTester tester, {
    required dynamic dio,
    bool conSesion = true,
  }) async {
    final almacen = AlmacenEnMemoria();
    if (conSesion) await almacen.guardar('sesion.acceso', 'a.b.c');
    await tester.pumpWidget(
      ProviderScope(
        retry: (r, e) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          dioProvider.overrideWithValue(dio),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaUnirse(codigo: _codigo),
        ),
      ),
    );
    await asentar(tester);
  }

  testWidgets(
    'con sesión muestra el grupo con importe formateado y exige la casilla',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/grupos/invitaciones/enlace/consultar',
        (s) => s.reply(200, {
          'grupoId': '11111111-1111-4111-8111-111111111111',
          'nombre': 'Pasanaku de prueba',
          'montoAporte': '100.00',
          'moneda': 'BOB',
          'periodicidad': 'MENSUAL',
          'reglamento': 'Aportar a tiempo.',
          'hashReglamento': 'a' * 64,
        }),
        data: Matchers.any,
      );
      await montar(tester, dio: dio);

      expect(find.text('Pasanaku de prueba'), findsOneWidget);
      expect(find.text('Bs 100,00'), findsOneWidget);
      expect(find.text('por mes'), findsOneWidget);
      expect(find.textContaining('MENSUAL'), findsNothing);
      final unirme = find.widgetWithText(Boton, 'Unirme al grupo');
      expect(tester.widget<Boton>(unirme).onPressed, isNull);

      await tester.tap(find.text('Leí y acepto este reglamento'));
      await tester.pump();
      expect(tester.widget<Boton>(unirme).onPressed, isNotNull);
    },
  );

  testWidgets(
    'rechazo del servidor (invitación revocada): error visible y reintentar',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/grupos/invitaciones/enlace/consultar',
        (s) => s.reply(422, {
          'codigo': 'AP-CU69-05',
          'mensaje': 'invitación revocada',
          'trazaId': 't-1',
        }),
        data: Matchers.any,
      );
      await montar(tester, dio: dio);

      expect(
        find.textContaining('No pudimos verificar la invitación'),
        findsOneWidget,
      );
      expect(find.text('Reintentar'), findsOneWidget);
      expect(find.text('Unirme al grupo'), findsNothing);
    },
  );

  testWidgets('sin sesión pide ingresar con el teléfono invitado', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    await montar(tester, dio: dio, conSesion: false);
    expect(find.textContaining('Ingresá con el teléfono'), findsOneWidget);
    expect(find.text('Ingresar'), findsOneWidget);
    expect(find.text('Crear cuenta'), findsOneWidget);
  });
}
