import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/solicitudes_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_solicitudes_del_grupo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
// `Override` solo sale de `riverpod` (dependencia directa de flutter_riverpod).
// ignore: depend_on_referenced_packages
import 'package:riverpod/misc.dart' show Override;

SolicitudDeIngreso _s({
  String? nombre,
  String? puntaje,
  String? mensaje,
  int cupos = 1,
  bool? porEnlace,
}) => SolicitudDeIngreso(
  solicitudId: 's-${nombre ?? 'x'}',
  usuarioId: 'u',
  cuposSolicitados: cupos,
  estado: SolicitudDeIngresoEstadoEnum.PENDIENTE,
  fechaSolicitud: DateTime(2026, 10, 3, 12),
  nombreVisible: nombre,
  puntaje: puntaje,
  mensaje: mensaje,
  porEnlace: porEnlace,
);

Future<void> _montar(WidgetTester tester, List<SolicitudDeIngreso> l) async {
  tester.view.physicalSize = const Size(1080, 2280);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  final List<Override> overrides = [
    solicitudesDelGrupoProvider('g1').overrideWith((ref) async => l),
  ];
  await tester.pumpWidget(
    ProviderScope(
      retry: (_, _) => null,
      overrides: overrides,
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const PantallaSolicitudesDelGrupo(grupoId: 'g1'),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('correcto: nombre, puntaje, cupos, mensaje y dos acciones', (
    tester,
  ) async {
    await _montar(tester, [
      _s(nombre: 'Ana', puntaje: '720', mensaje: 'Soy vecina', cupos: 2),
    ]);
    expect(find.text('Ana'), findsOneWidget);
    expect(find.text('Puntaje 720'), findsOneWidget);
    expect(find.text('2 cupos'), findsOneWidget);
    expect(find.text('«Soy vecina»'), findsOneWidget);
    expect(find.text('Aceptar'), findsOneWidget);
    expect(find.text('Rechazar'), findsOneWidget);
    expect(find.textContaining('3 oct 2026'), findsOneWidget);
  });

  testWidgets('límite: sin puntaje ni mensaje no se inventan', (tester) async {
    await _montar(tester, [_s(nombre: 'Beto', mensaje: '   ')]);
    expect(find.textContaining('Puntaje'), findsNothing);
    expect(find.textContaining('«'), findsNothing);
    expect(find.text('1 cupo'), findsOneWidget);
  });

  testWidgets('vacío: dice por qué y qué pasa después', (tester) async {
    await _montar(tester, []);
    expect(find.text('Nadie pidió unirse todavía'), findsOneWidget);
    expect(
      find.textContaining('Cuando alguien abra el enlace'),
      findsOneWidget,
    );
  });
}
