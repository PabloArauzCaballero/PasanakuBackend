import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/acceso_al_traspaso.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// La puerta a CU-64 aparece solo a quien ocupa un cupo. Datos SINTÉTICOS.
DetalleDelGrupo _detalle(List<IntegranteDelGrupo> integrantes) =>
    DetalleDelGrupo(
      grupoId: 'g1',
      nombre: 'Vecinos',
      estado: 'CONFORMADO',
      montoAporte: '100.00',
      moneda: 'BOB',
      periodicidad: 'MENSUAL',
      cuposTotales: 2,
      cuposOcupados: 2,
      soyOrganizador: false,
      debeFirmar: false,
      puedeSortear: false,
      integrantes: integrantes,
    );

IntegranteDelGrupo _i({
  required bool yo,
  String? cupo,
  IntegranteDelGrupoEstadoEnum estado = IntegranteDelGrupoEstadoEnum.FIRMADO,
}) => IntegranteDelGrupo(
  numero: 1,
  cupoId: cupo,
  usuarioId: 'u',
  nombre: 'Ana',
  estado: estado,
  esOrganizador: false,
  soyYo: yo,
);

Future<GoRouter> _montar(WidgetTester t, DetalleDelGrupo d) async {
  final enrutador = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => Scaffold(body: AccesoAlTraspaso(detalle: d)),
      ),
      GoRoute(
        path: '/pasanaku/grupos/:g/cupos/:c/traspaso',
        builder: (_, s) => Text('traspaso ${s.pathParameters['c']}'),
      ),
    ],
  );
  await t.pumpWidget(
    MaterialApp.router(
      theme: temaDesde(Tokens.claro, Brightness.light),
      routerConfig: enrutador,
    ),
  );
  await t.pumpAndSettle();
  return enrutador;
}

void main() {
  testWidgets('mi cupo ocupado: aparece y abre el traspaso de ESE cupo', (
    t,
  ) async {
    await _montar(
      t,
      _detalle([_i(yo: false, cupo: 'c2'), _i(yo: true, cupo: 'c1')]),
    );
    expect(find.text('Traspasar mi cupo'), findsOneWidget);
    await t.tap(find.byKey(const ValueKey('traspasar-mi-cupo')));
    await t.pumpAndSettle();
    expect(find.text('traspaso c1'), findsOneWidget);
  });

  testWidgets('sin cupo propio, o con el cupo libre: no aparece', (t) async {
    await _montar(t, _detalle([_i(yo: false, cupo: 'c2')]));
    expect(find.text('Traspasar mi cupo'), findsNothing);
    await _montar(
      t,
      _detalle([
        _i(yo: true, cupo: 'c1', estado: IntegranteDelGrupoEstadoEnum.LIBRE),
      ]),
    );
    expect(find.text('Traspasar mi cupo'), findsNothing);
  });
}
