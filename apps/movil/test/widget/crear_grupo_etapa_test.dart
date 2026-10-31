import 'dart:convert';

import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_crear_grupo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Etapa mínima al crear un grupo (H1.S3.M1, 2026-10-30): por omisión la propia, sin
/// opciones por encima, y lo elegido viaja en `crearGrupo.etapaMinima`. El servidor es
/// un doble de `transparencia.yaml` (`listarEtapas`, `consultarPuntaje`) y de
/// `grupos.yaml` (`crearGrupo`). Las etapas son las PROPUESTAS del plan del trabajo
/// (H1.S6, 2026-10-30), sintéticas: no son un
/// catálogo confirmado.
const _usuario = 'b8000000-0000-4000-8000-000000000077';

Map<String, dynamic> _etapa(
  String codigo,
  String nombre,
  int orden,
  int desde,
) => {
  'codigo': codigo,
  'nombre': nombre,
  'orden': orden,
  'desde': '$desde.00',
  'hasta': '${desde + 50}.00',
};

final _catalogo = [
  _etapa('CONSTANTE', 'Constante', 3, 600),
  _etapa('RECUPERACION', 'En recuperación', 0, 0),
  _etapa('EN_MARCHA', 'En marcha', 2, 550),
  _etapa('INICIAL', 'Inicial', 1, 500),
];

Map<String, dynamic> _puntaje() => {
  'tieneHistorial': true,
  'puntaje': '575.00',
  'nivelDeConfianza': 'MEDIO',
  'etapa': {
    'codigo': 'EN_MARCHA',
    'nombre': 'En marcha',
    'orden': 2,
    'desde': '550.00',
    'hasta': '600.00',
    'progreso': '0.5000',
    'siguiente': {
      'codigo': 'CONSTANTE',
      'nombre': 'Constante',
      'desde': '600.00',
    },
  },
};

String _token() {
  String parte(Object v) =>
      base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({'sub': _usuario})}.';
}

/// Monta el formulario con sesión, catálogo y puntaje; devuelve los cuerpos de los
/// `POST /grupos` que salieron.
Future<List<Map<String, dynamic>>> _montar(
  WidgetTester tester, {
  bool catalogoFalla = false,
  Map<String, dynamic>? rechazo,
}) async {
  final (:dio, :adaptador) = dioSimulado();
  organizadorHabilitado(adaptador);
  final sesion = Sesion(AlmacenEnMemoria());
  await sesion.abrir(acceso: _token());
  addTearDown(sesion.dispose);
  adaptador
    ..onGet(
      '/reputacion/etapas',
      (s) => catalogoFalla
          ? s.reply(503, {'codigo': 'AP-GEN-503', 'trazaId': 'traza-etapas'})
          : s.reply(200, _catalogo),
    )
    ..onGet('/reputacion/$_usuario/puntaje', (s) => s.reply(200, _puntaje()))
    ..onPost(
      '/grupos',
      (s) => rechazo == null
          ? s.reply(201, ejemplo('grupos', 'crearGrupo', 'ok')['cuerpo'])
          : s.reply(422, rechazo),
      data: Matchers.any,
    );
  final cuerpos = <Map<String, dynamic>>[];
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (o, handler) {
        if (o.method == 'POST' && o.path == '/grupos') {
          final d = o.data;
          cuerpos.add(
            Map<String, dynamic>.from((d is String ? jsonDecode(d) : d) as Map),
          );
        }
        handler.next(o);
      },
    ),
  );
  tester.view.physicalSize = const Size(412, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    conApp(
      const PantallaCrearGrupo(),
      dio: dio,
      extra: [sesionProvider.overrideWithValue(sesion)],
    ),
  );
  await asentar(tester);
  await tester.pump();
  return cuerpos;
}

Future<void> _completarYCrear(WidgetTester tester) async {
  final campos = find.byType(EditableText);
  await tester.enterText(campos.at(0), 'Grupo de prueba');
  await tester.enterText(campos.at(1), '100,00');
  await tester.pump();
  await tester.tap(find.widgetWithText(Boton, 'Crear grupo'));
  await asentar(tester);
}

void main() {
  testWidgets(
    'por omisión muestra la etapa propia y no ofrece ninguna más alta',
    (tester) async {
      await _montar(tester);

      expect(find.text('Etapa mínima para entrar'), findsOneWidget);
      expect(find.text('Etapa 2 · En marcha'), findsOneWidget);
      expect(
        find.textContaining(
          'Solo pueden entrar personas desde Etapa 2 · En marcha',
        ),
        findsOneWidget,
      );

      await tester.tap(find.text('Etapa 2 · En marcha'));
      await tester.pumpAndSettle();
      // Las filas de la hoja son RichText (resaltan la búsqueda).
      expect(
        find.text('Etapa 1 · Inicial', findRichText: true),
        findsOneWidget,
      );
      expect(find.text('En recuperación', findRichText: true), findsOneWidget);
      expect(
        find.text('Tuya · desde 550 puntos', findRichText: true),
        findsOneWidget,
      );
      expect(
        find.text('Etapa 3 · Constante', findRichText: true),
        findsNothing,
        reason: 'está por encima de la propia',
      );
    },
  );

  testWidgets(
    'sin tocar el selector, crear no manda etapa: vale la del servidor',
    (tester) async {
      final cuerpos = await _montar(tester);
      await _completarYCrear(tester);

      expect(cuerpos, hasLength(1));
      expect(cuerpos.single.containsKey('etapaMinima'), isFalse);
    },
  );

  testWidgets('una etapa más baja elegida viaja en etapaMinima', (
    tester,
  ) async {
    final cuerpos = await _montar(tester);
    await tester.tap(find.text('Etapa 2 · En marcha'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Etapa 1 · Inicial', findRichText: true));
    await tester.pumpAndSettle();
    expect(
      find.textContaining(
        'Solo pueden entrar personas desde Etapa 1 · Inicial',
      ),
      findsOneWidget,
    );

    await _completarYCrear(tester);
    expect(cuerpos.single['etapaMinima'], 'INICIAL');
  });

  // Revisión 2026-10-30: en un teléfono (360 x 760) el selector vive en un ListView
  // perezoso; al volver arriba se desmonta. La elección no puede perderse con él, o el
  // grupo nace con la etapa propia (más exigente que la elegida) sin aviso.
  testWidgets(
    'la etapa elegida sobrevive a que el selector salga de pantalla',
    (tester) async {
      final cuerpos = await _montar(tester);
      tester.view.physicalSize = const Size(360, 760);
      await tester.pumpAndSettle();
      final lista = find.byType(Scrollable).first;
      await tester.scrollUntilVisible(
        find.text('Etapa 2 · En marcha'),
        200,
        scrollable: lista,
      );
      await tester.tap(find.text('Etapa 2 · En marcha'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Etapa 1 · Inicial', findRichText: true));
      await tester.pumpAndSettle();

      await tester.drag(lista, const Offset(0, 3000));
      await tester.pumpAndSettle();
      expect(
        find.textContaining('Solo pueden entrar', skipOffstage: false),
        findsNothing,
        reason: 'el caso exige que el selector se haya desmontado',
      );

      await _completarYCrear(tester);
      expect(cuerpos.single['etapaMinima'], 'INICIAL');
    },
  );

  testWidgets(
    'si el catálogo falla, lo dice, ofrece reintentar y deja crear igual',
    (tester) async {
      final cuerpos = await _montar(tester, catalogoFalla: true);

      expect(
        find.textContaining('No pudimos traer las etapas'),
        findsOneWidget,
      );
      expect(find.widgetWithText(Boton, 'Reintentar'), findsOneWidget);
      await _completarYCrear(tester);
      expect(cuerpos, hasLength(1));
      expect(cuerpos.single.containsKey('etapaMinima'), isFalse);
    },
  );

  testWidgets('el 422 AP-CU20-08 se traduce a qué cambiar', (tester) async {
    await _montar(
      tester,
      rechazo: {
        'codigo': 'AP-CU20-08',
        'mensaje': 'texto del backend que la app no muestra',
        'trazaId': 'traza-crear-de-prueba',
      },
    );
    await _completarYCrear(tester);

    expect(
      find.text('Esa etapa no se puede pedir: elegí la tuya o una más baja.'),
      findsOneWidget,
    );
    expect(find.textContaining('texto del backend'), findsNothing);
  });
}
