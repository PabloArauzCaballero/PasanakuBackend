import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_identidad_requerida.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_documento.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Verificación diferida (2026-10-17): el paso del carnet se puede saltar, y la
/// pantalla que se interpone al operar ofrece lo que corresponde a cada estado.
void main() {
  const perfil = '/usuarios/yo/perfil';

  Map<String, Object?> cuenta(String estado) => {
    'usuarioId': 'b8000000-0000-4000-8000-000000000077',
    'correo': 'rosa@example.com',
    'correoVerificado': true,
    'estadoDeCuenta': estado,
  };

  testWidgets(
    'el paso del carnet ofrece «Saltar por ahora» y lleva a los datos',
    (tester) async {
      final (:dio, adaptador: _) = dioSimulado();
      await tester.pumpWidget(
        conApp(
          const Scaffold(body: SingleChildScrollView(child: PasoDocumento())),
          dio: dio,
        ),
      );
      final c = ProviderScope.containerOf(
        tester.element(find.byType(PasoDocumento)),
      );
      c.read(altaProvider.notifier)
        ..siguiente()
        ..siguiente();
      await tester.pump();

      expect(
        find.text('¿Preferís verificar tu identidad después?'),
        findsOneWidget,
      );
      await tester.tap(find.text('Saltar por ahora'));
      await tester.pump();

      expect(c.read(altaProvider).paso, PasoAlta.datos);
      expect(c.read(altaProvider).sinVerificacion, isTrue);
    },
  );

  testWidgets('desde la app (solo verificación) no hay nada que saltar', (
    tester,
  ) async {
    final (:dio, adaptador: _) = dioSimulado();
    await tester.pumpWidget(
      conApp(
        const Scaffold(body: SingleChildScrollView(child: PasoDocumento())),
        dio: dio,
      ),
    );
    ProviderScope.containerOf(
      tester.element(find.byType(PasoDocumento)),
    ).read(altaProvider.notifier).empezarSoloVerificacion();
    await tester.pump();

    expect(find.text('Saltar por ahora'), findsNothing);
  });

  testWidgets(
    'correcto: sin verificar ofrece «Verificar mi identidad» y «Ahora no»',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(perfil, (s) => s.reply(200, cuenta('INCOMPLETO_KYC')));

      await tester.pumpWidget(
        conApp(
          const PantallaIdentidadRequerida(volver: '/billetera/recargar'),
          dio: dio,
        ),
      );
      await asentar(tester);

      expect(find.text('Verificá tu identidad'), findsOneWidget);
      expect(find.text('Verificar mi identidad'), findsOneWidget);
      expect(find.text('Ahora no'), findsOneWidget);
    },
  );

  testWidgets(
    'límite: en revisión no pide el carnet de nuevo, muestra el estado',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        perfil,
        (s) => s.reply(200, cuenta('PENDIENTE_VERIFICACION')),
      );

      await tester.pumpWidget(
        conApp(const PantallaIdentidadRequerida(), dio: dio),
      );
      await asentar(tester);

      expect(find.text('Estamos verificando tu identidad'), findsOneWidget);
      // 2026-10-19: «Consultar estado» pregunta ahí mismo, sin otra pantalla.
      expect(find.text('Consultar estado'), findsOneWidget);
      expect(find.text('Verificar mi identidad'), findsNothing);
    },
  );

  testWidgets(
    'correcto: «Consultar estado» en revisión avisa sin borrar la pantalla',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        perfil,
        (s) => s.reply(200, cuenta('PENDIENTE_VERIFICACION')),
      );

      await tester.pumpWidget(
        conApp(const PantallaIdentidadRequerida(), dio: dio),
      );
      await asentar(tester);
      await tester.tap(find.text('Consultar estado'));
      await tester.pump();
      // Mientras consulta, lo que se veía sigue ahí (antes parpadeaba).
      expect(find.text('Estamos verificando tu identidad'), findsOneWidget);
      await asentar(tester);
      expect(
        find.text(
          'Tu identidad sigue en revisión. Te avisamos apenas la aprueben.',
        ),
        findsOneWidget,
      );
      expect(find.text('Estamos verificando tu identidad'), findsOneWidget);
    },
  );

  testWidgets('límite: ya aprobada (token viejo) ofrece activar la cuenta', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(perfil, (s) => s.reply(200, cuenta('ACTIVO')));

    await tester.pumpWidget(
      conApp(const PantallaIdentidadRequerida(), dio: dio),
    );
    await asentar(tester);

    expect(find.text('Tu identidad ya está verificada'), findsOneWidget);
    expect(find.text('Activar mi cuenta'), findsOneWidget);
  });

  testWidgets('inválido: si el perfil no responde, deja reintentar', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      perfil,
      (s) => s.reply(500, {'codigo': 'AP-X', 'mensaje': 'no', 'trazaId': 't'}),
    );

    await tester.pumpWidget(
      conApp(const PantallaIdentidadRequerida(), dio: dio),
    );
    await asentar(tester);

    expect(find.text('Verificar mi identidad'), findsNothing);
    expect(find.text('Volver a intentar'), findsWidgets);
  });
}
