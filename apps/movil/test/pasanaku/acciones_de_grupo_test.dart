import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/acciones_de_grupo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

/// Hallazgo del carril H (prueba en emulador): las rutas de crear grupo e invitar
/// existían pero ninguna pantalla llevaba hasta ellas. Estas pruebas fijan que desde la
/// pestaña «Grupos» se llega a las dos, y que «invitar» no aparece sin un grupo.
void main() {
  Future<List<String>> montar(WidgetTester tester, {String? grupoId}) async {
    final visitadas = <String>[];
    final enrutador = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (c, s) => Scaffold(body: AccionesDeGrupo(grupoId: grupoId)),
        ),
        GoRoute(
          path: '/pasanaku/grupos/nuevo',
          builder: (c, s) {
            visitadas.add(s.uri.path);
            return const Scaffold(body: Text('destino-crear'));
          },
        ),
        GoRoute(
          path: '/pasanaku/unirse-a-un-grupo',
          builder: (c, s) {
            visitadas.add(s.uri.path);
            return const Scaffold(body: Text('destino-unirse'));
          },
        ),
        GoRoute(
          path: '/pasanaku/grupos/:grupoId/invitar',
          builder: (c, s) {
            visitadas.add(s.uri.path);
            return const Scaffold(body: Text('destino-invitar'));
          },
        ),
      ],
    );
    await tester.pumpWidget(
      MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: enrutador,
      ),
    );
    await tester.pumpAndSettle();
    return visitadas;
  }

  testWidgets('sin grupo solo se ofrece organizar uno, nunca invitar', (
    tester,
  ) async {
    final visitadas = await montar(tester);
    expect(find.text('Invitar a alguien a mi grupo'), findsNothing);
    await tester.tap(find.text('Organizar un grupo nuevo'));
    await tester.pumpAndSettle();
    expect(find.text('destino-crear'), findsOneWidget);
    expect(visitadas, ['/pasanaku/grupos/nuevo']);
  });

  testWidgets('debajo de organizar, «Unirse a un grupo» lleva a subir el QR o '
      'pegar el enlace', (tester) async {
    final visitadas = await montar(tester);
    await tester.tap(find.text('Unirse a un grupo'));
    await tester.pumpAndSettle();
    expect(find.text('destino-unirse'), findsOneWidget);
    expect(visitadas, ['/pasanaku/unirse-a-un-grupo']);
  });

  testWidgets('con grupo, invitar lleva a la pantalla de ese grupo', (
    tester,
  ) async {
    const id = 'aaaaaaaa-0000-4000-8000-000000000001';
    final visitadas = await montar(tester, grupoId: id);
    await tester.tap(find.text('Invitar a alguien a mi grupo'));
    await tester.pumpAndSettle();
    expect(find.text('destino-invitar'), findsOneWidget);
    expect(visitadas, ['/pasanaku/grupos/$id/invitar']);
  });
}
