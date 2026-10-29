import 'package:dio/dio.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_estado.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mis_grupos.dart';
import 'package:aportaya_movil/pantallas/pasanaku/tarjeta_de_mi_grupo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _ruta = '/grupos/participaciones';

Map<String, Object?> _grupo(
  String n, {
  String? nombre,
  bool organiza = false,
  String estado = 'ACTIVO',
}) => {
  'grupoId': 'c6900000-0000-4000-8000-00000000000$n',
  'participanteId': 'c6900000-0000-4000-8000-00000000010$n',
  'estado': estado,
  'nombreDelGrupo': ?nombre,
  if (nombre != null) 'montoAporte': '500.00',
  if (nombre != null) 'moneda': 'BOB',
  if (nombre != null) 'esOrganizador': organiza,
};

void main() {
  testWidgets('correcto: dos grupos son dos tarjetas, cada una con su nombre, '
      'su rol y su aporte', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, [
        _grupo('1', nombre: 'Pasanaku de la oficina', organiza: true),
        _grupo('2', nombre: 'Ahorro familiar'),
      ]),
    );

    await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
    await asentar(tester);

    expect(find.byType(TarjetaDeMiGrupo), findsNWidgets(2));
    expect(find.text('Pasanaku de la oficina'), findsOneWidget);
    expect(find.text('Ahorro familiar'), findsOneWidget);
    expect(find.text('Organizás este grupo'), findsOneWidget);
    expect(find.text('Participás en este grupo'), findsOneWidget);
    expect(find.textContaining('por período'), findsNWidgets(2));
    expect(find.text('Organizar un grupo nuevo'), findsOneWidget);
  });

  testWidgets(
    'correcto: los grupos se separan en «Organizás» y «Participás», cada uno con su tope',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet(
          _ruta,
          (s) => s.reply(200, [
            _grupo('1', nombre: 'Pasanaku de la oficina', organiza: true),
            _grupo('2', nombre: 'Ahorro familiar'),
          ]),
        )
        ..onGet(
          '/grupos/mis-limites',
          (s) => s.reply(200, {
            'organizados': 1,
            'limiteDeOrganizar': 2,
            'participaciones': 1,
            'limiteDeParticipar': 4,
          }),
        );
      await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
      await asentar(tester);
      expect(find.text('Organizás · 1 de 2'), findsOneWidget);
      expect(find.text('Participás · 1 de 4'), findsOneWidget);
      expect(find.textContaining('el máximo'), findsNothing);
    },
  );

  testWidgets('límite: al tope de cada uno, el botón se apaga y dice por qué', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador
      ..onGet(_ruta, (s) => s.reply(200, <Object>[]))
      ..onGet(
        '/grupos/mis-limites',
        (s) => s.reply(200, {
          'organizados': 2,
          'limiteDeOrganizar': 2,
          'participaciones': 4,
          'limiteDeParticipar': 4,
        }),
      );
    await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
    await asentar(tester);
    expect(
      find.textContaining('Ya organizás 2 grupos, el máximo'),
      findsOneWidget,
    );
    expect(
      find.textContaining('Ya participás en 4 grupos, el máximo'),
      findsOneWidget,
    );
    await tester.tap(find.text('Organizar un grupo nuevo'));
    await asentar(tester);
    expect(find.text('Organizar un grupo nuevo'), findsOneWidget);
  });

  testWidgets('límite: si el servidor todavía no manda el nombre, la tarjeta '
      'igual aparece y no inventa uno', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, [_grupo('1', estado: 'ACEPTADO_PENDIENTE_FIRMA')]),
    );

    await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
    await asentar(tester);

    expect(find.byType(TarjetaDeMiGrupo), findsOneWidget);
    expect(find.text('Grupo'), findsOneWidget);
    expect(find.text('Falta firmar'), findsOneWidget);
    expect(find.textContaining('por período'), findsNothing);
  });

  testWidgets('deslizar hacia abajo vuelve a pedir los grupos y muestra el '
      'nuevo, sin salir de la pestaña', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    var pedidos = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          if (o.path.endsWith(_ruta)) pedidos++;
          h.next(o);
        },
      ),
    );
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, [_grupo('1', nombre: 'Pasanaku de la oficina')]),
    );

    await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
    await asentar(tester);
    expect(pedidos, 1);
    expect(find.text('Grupo recién creado'), findsNothing);

    // Mientras tanto se creó un grupo: el servidor ya lo devuelve.
    adaptador.onGet(
      _ruta,
      (s) => s.reply(200, [
        _grupo('1', nombre: 'Pasanaku de la oficina'),
        _grupo('2', nombre: 'Grupo recién creado'),
      ]),
    );
    await tester.fling(find.byType(ListView), const Offset(0, 400), 1000);
    await asentar(tester);

    expect(pedidos, 2);
    expect(find.text('Grupo recién creado'), findsOneWidget);
  });

  testWidgets('vacío: sin grupos lo dice y deja organizar uno', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(_ruta, (s) => s.reply(200, <Object>[]));

    await tester.pumpWidget(conApp(const PantallaMisGrupos(), dio: dio));
    await asentar(tester);

    expect(find.byType(TarjetaDeMiGrupo), findsNothing);
    expect(
      find.textContaining('Todavía no participás en ningún grupo'),
      findsOneWidget,
    );
    expect(find.text('Organizar un grupo nuevo'), findsOneWidget);
  });

  testWidgets('el estado de un grupo se titula con el nombre de ese grupo', (
    tester,
  ) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onGet(
      RegExp(r'.*'),
      (s) => s.reply(404, {'codigo': 'AP-X', 'mensaje': 'no', 'trazaId': 't'}),
    );

    await tester.pumpWidget(
      conApp(
        const PantallaMiEstadoDeLaSesion(
          participanteId: 'c6900000-0000-4000-8000-000000000101',
          usuarioId: 'b8000000-0000-4000-8000-000000000077',
          grupoId: 'c6900000-0000-4000-8000-000000000001',
          nombreDelGrupo: 'Pasanaku de la oficina',
        ),
        dio: dio,
      ),
    );
    await asentar(tester);

    // La cabecera pinta el título dos veces (grande y compacto) pero hay un solo
    // encabezado para el lector: ese es el título de la pantalla.
    expect(
      find.ancestor(
        of: find.text('Pasanaku de la oficina'),
        matching: find.byWidgetPredicate(
          (w) => w is Semantics && (w.properties.header ?? false),
        ),
      ),
      findsOneWidget,
    );
    expect(find.text('Estado de tu aporte'), findsNothing);
  });
}
