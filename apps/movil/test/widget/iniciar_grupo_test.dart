import 'dart:ui' show Tristate;

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/pantallas/pasanaku/seccion_del_grupo.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Inicio al 75 % (H1.S3.M2, 2026-10-30): el bloque «Para iniciar el grupo» con su
/// progreso, el botón Iniciar bloqueado con motivo, la confirmación que explica el
/// achique y los 422 traducidos. El servidor es un doble de `grupos.yaml`
/// (`consultarDetalleDelGrupo`, `iniciarGrupo`); nombres e ids son sintéticos.
const _grupo = 'c6900000-0000-4000-8000-0000000000f2';
const _detalle = '/grupos/$_grupo/detalle';
const _inicio = '/grupos/$_grupo/inicio';

Map<String, dynamic> _detalleDe({
  required int ocupados,
  required bool puedoIniciar,
  int sinFirmar = 0,
  bool organizador = true,
  String estado = 'ABIERTO_A_INSCRIPCION',
}) => {
  'grupoId': _grupo,
  'nombre': 'Pasanaku de prueba',
  'estado': estado,
  'montoAporte': '500.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': 10,
  'cuposOcupados': ocupados,
  'soyOrganizador': organizador,
  'debeFirmar': false,
  'puedeSortear': false,
  'minimoParaIniciar': 8,
  'puedoIniciar': puedoIniciar,
  'integrantes': [
    for (var n = 1; n <= 10; n++)
      {
        'numero': n,
        'cupoId':
            'c6900000-0000-4000-8000-0000000000${(n + 10).toRadixString(16).padLeft(2, '0')}',
        'nombre': n <= ocupados ? 'Persona $n' : null,
        'estado': n > ocupados
            ? 'LIBRE'
            : n > ocupados - sinFirmar
            ? 'PENDIENTE_FIRMA'
            : 'FIRMADO',
        'esOrganizador': n == 1,
        'soyYo': n == 1,
      },
  ],
};

Map<String, dynamic> _error(String codigo) => {
  'codigo': codigo,
  'mensaje': 'texto del backend que la app no muestra',
  'trazaId': 'traza-inicio-de-prueba',
};

Future<void> _montar(
  WidgetTester tester,
  Dio dio, {
  Brightness brillo = Brightness.light,
}) async {
  tester.view.physicalSize = const Size(412, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    conApp(
      const Scaffold(
        body: SingleChildScrollView(child: SeccionDelGrupo(grupoId: _grupo)),
      ),
      dio: dio,
      brillo: brillo,
    ),
  );
  await asentar(tester);
}

Boton _iniciar(WidgetTester tester) =>
    tester.widget<Boton>(find.byKey(const ValueKey('iniciar-grupo')));

void main() {
  testWidgets('antes del 75 % el botón está bloqueado y dice cuánto falta', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _detalle,
      (s) => s.reply(200, _detalleDe(ocupados: 7, puedoIniciar: false)),
    );
    await _montar(tester, dio);

    expect(find.text('Para iniciar el grupo'), findsOneWidget);
    expect(find.text('7 de 10 · falta 1 para poder iniciar'), findsOneWidget);
    expect(
      find.text('Se inicia con el 75 % de los cupos ocupados: 8 de 10.'),
      findsOneWidget,
    );
    expect(_iniciar(tester).onPressed, isNull);
    expect(
      find.text('Falta 1 cupo ocupado para poder iniciar.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'con el mínimo pero firmas pendientes sigue bloqueado con motivo',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        _detalle,
        (s) => s.reply(
          200,
          _detalleDe(ocupados: 8, puedoIniciar: false, sinFirmar: 2),
        ),
      );
      await _montar(tester, dio);

      expect(_iniciar(tester).onPressed, isNull);
      expect(
        find.textContaining('2 personas todavía no firmaron el reglamento'),
        findsOneWidget,
      );
    },
  );

  testWidgets('quien no organiza ve el progreso pero no el botón', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _detalle,
      (s) => s.reply(
        200,
        _detalleDe(ocupados: 8, puedoIniciar: false, organizador: false),
      ),
    );
    await _montar(tester, dio);

    expect(find.text('8 de 10 · ya se puede iniciar'), findsOneWidget);
    expect(find.byKey(const ValueKey('iniciar-grupo')), findsNothing);
  });

  for (final brillo in Brightness.values) {
    testWidgets('tema ${brillo.name}: área táctil y contraste del bloque', (
      tester,
    ) async {
      final semantica = tester.ensureSemantics();
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onGet(
        _detalle,
        (s) => s.reply(200, _detalleDe(ocupados: 8, puedoIniciar: true)),
      );
      await _montar(tester, dio, brillo: brillo);

      expect(tester.takeException(), isNull);
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(textContrastGuideline));
      // El botón se anuncia como botón habilitado, con su texto como nombre.
      final nodo = tester.getSemantics(
        find.descendant(
          of: find.byKey(const ValueKey('iniciar-grupo')),
          matching: find.text('Iniciar el grupo'),
        ),
      );
      expect(nodo.getSemanticsData().label, contains('Iniciar el grupo'));
      expect(nodo.flagsCollection.isButton, isTrue);
      expect(nodo.flagsCollection.isEnabled, Tristate.isTrue);
      semantica.dispose();
    });
  }

  testWidgets('un grupo ya iniciado no muestra el bloque', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _detalle,
      (s) => s.reply(
        200,
        _detalleDe(ocupados: 8, puedoIniciar: false, estado: 'CONFORMADO'),
      ),
    );
    await _montar(tester, dio);

    expect(find.text('Para iniciar el grupo'), findsNothing);
  });

  testWidgets(
    'después del 75 % se habilita, confirma el achique, inicia con clave y vuelve a preguntar',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      var consultas = 0;
      final claves = <String?>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (o, handler) {
            if (o.method == 'GET' && o.path == _detalle) consultas++;
            if (o.method == 'POST' && o.path == _inicio) {
              claves.add(o.headers['Idempotency-Key'] as String?);
            }
            handler.next(o);
          },
        ),
      );
      adaptador
        ..onGet(
          _detalle,
          (s) => s.reply(200, _detalleDe(ocupados: 8, puedoIniciar: true)),
        )
        ..onPost(
          _inicio,
          (s) => s.reply(200, {
            'grupoId': _grupo,
            'estado': 'CONFORMADO',
            'cuposTotales': 8,
            'numPeriodos': 8,
          }),
        );
      await _montar(tester, dio);

      expect(find.text('8 de 10 · ya se puede iniciar'), findsOneWidget);
      expect(_iniciar(tester).onPressed, isNotNull);
      await tester.tap(find.byKey(const ValueKey('iniciar-grupo')));
      await tester.pumpAndSettle();

      // La confirmación dice qué cambia y que no tiene vuelta atrás.
      expect(find.text('Iniciar el grupo con 8 personas'), findsOneWidget);
      expect(find.textContaining('se achica a los 8 cupos'), findsOneWidget);
      expect(
        find.textContaining('Los 2 cupos libres se cierran'),
        findsOneWidget,
      );
      expect(
        find.textContaining('nadie más va a poder entrar'),
        findsOneWidget,
      );
      expect(claves, isEmpty, reason: 'nada se manda antes de confirmar');

      await tester.tap(find.text('Sí, iniciar'));
      await asentar(tester);

      expect(claves, hasLength(1));
      expect(claves.single, matches(RegExp(r'^[0-9a-f-]{36}$')));
      expect(consultas, 2, reason: 'al iniciar se invalida el detalle');
      expect(find.text('Iniciaste el grupo con 8 personas.'), findsOneWidget);
    },
  );

  for (final (codigo, texto) in [
    (
      'AP-CU69-16',
      'Todavía no se llega al 75 % de cupos ocupados. Sumá a más gente e '
          'intentá de nuevo.',
    ),
    ('AP-CU69-17', 'Este grupo ya se inició: ya no entra nadie más.'),
  ]) {
    testWidgets(
      'el 422 $codigo del servidor se traduce y no se muestra crudo',
      (tester) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador
          ..onGet(
            _detalle,
            (s) => s.reply(200, _detalleDe(ocupados: 8, puedoIniciar: true)),
          )
          ..onPost(_inicio, (s) => s.reply(422, _error(codigo)));
        await _montar(tester, dio);

        await tester.tap(find.byKey(const ValueKey('iniciar-grupo')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Sí, iniciar'));
        await asentar(tester);

        expect(find.text(texto), findsOneWidget);
        expect(
          find.text('Código de seguimiento: traza-inicio-de-prueba'),
          findsOneWidget,
        );
        expect(find.textContaining('texto del backend'), findsNothing);
        expect(find.textContaining('Iniciaste'), findsNothing);
      },
    );
  }
}
