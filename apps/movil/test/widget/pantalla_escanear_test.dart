import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/ajustes_del_sistema.dart';
import 'package:aportaya_movil/dominio/puertos/visor_qr.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_escanear.dart';
import 'package:aportaya_movil/pantallas/pasanaku/textos.dart';
import 'package:aportaya_movil/proveedores/ajustes_del_sistema.dart';
import 'package:aportaya_movil/proveedores/visor_qr.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

const _uuid = '3f2a9c1e-8b47-4d21-9a66-0c5e7d1b2a34';
final _hash = 'ab' * 32;
final _invitacion = 'aportaya://unirse/$_uuid.$_hash';

class _Ajustes implements AjustesDelSistema {
  _Ajustes({this.resultado = true});
  final bool resultado;
  int aperturas = 0;

  @override
  Future<bool> abrir() async {
    aperturas++;
    return resultado;
  }
}

/// Un visor de mentira: expone las lecturas como botones, sin cámara.
class _Banco {
  _Banco({this.problema, _Ajustes? ajustes}) : ajustes = ajustes ?? _Ajustes();
  final ProblemaDeCamara? problema;
  final _Ajustes ajustes;
  late void Function(String) leer;
  late final GoRouter enrutador;

  Widget montar() {
    enrutador = GoRouter(
      initialLocation: '/escanear',
      routes: [
        GoRoute(path: '/escanear', builder: (_, _) => const PantallaEscanear()),
        GoRoute(
          path: '/pasanaku/unirse/:codigo',
          builder: (_, s) => Text('unirse ${s.pathParameters['codigo']}'),
        ),
      ],
    );
    return ProviderScope(
      overrides: [
        ajustesDelSistemaProvider.overrideWithValue(ajustes),
        visorQrProvider.overrideWithValue(({
          required alLeer,
          required problema,
        }) {
          leer = alLeer;
          return this.problema != null
              ? problema(this.problema!)
              : const Text('visor');
        }),
      ],
      child: MaterialApp.router(
        theme: temaDesde(Tokens.claro, Brightness.light),
        routerConfig: enrutador,
      ),
    );
  }
}

void main() {
  testWidgets(
    'correcto: un QR de invitación abre unirse y reemplaza el visor',
    (tester) async {
      final b = _Banco();
      await tester.pumpWidget(b.montar());
      await tester.pump();
      b.leer(_invitacion);
      await tester.pumpAndSettle();
      expect(find.text('unirse $_uuid.$_hash'), findsOneWidget);
      expect(find.text('visor'), findsNothing);
    },
  );

  testWidgets('límite: la misma lectura repetida navega una sola vez', (
    tester,
  ) async {
    final b = _Banco();
    await tester.pumpWidget(b.montar());
    await tester.pump();
    b.leer(_invitacion);
    b.leer(_invitacion);
    b.leer(_invitacion);
    await tester.pumpAndSettle();
    expect(find.text('unirse $_uuid.$_hash'), findsOneWidget);
    expect(
      b.enrutador.routerDelegate.currentConfiguration.matches,
      hasLength(1),
    );
  });

  testWidgets('inválido: un QR que no es invitación se avisa y no navega', (
    tester,
  ) async {
    final b = _Banco();
    await tester.pumpWidget(b.montar());
    await tester.pump();
    b.leer('000201010212-un-qr-de-cobro');
    await tester.pump();
    expect(find.text(TextosPasanaku.escanearRechazo), findsOneWidget);
    expect(find.text('visor'), findsOneWidget);
    await tester.pump(const Duration(seconds: 3));
  });

  testWidgets(
    'inválido: cámara denegada → mensaje accionable y entrada manual',
    (tester) async {
      final b = _Banco(problema: ProblemaDeCamara.denegado);
      await tester.pumpWidget(b.montar());
      await tester.pump();
      expect(find.text(TextosPasanaku.camaraDenegadaTitulo), findsOneWidget);
      expect(find.text(TextosPasanaku.camaraDenegadaDetalle), findsOneWidget);
      expect(find.text(TextosPasanaku.usarEnlace), findsOneWidget);
    },
  );

  testWidgets('correcto: pegar el enlace a mano lleva a unirse', (
    tester,
  ) async {
    final b = _Banco(problema: ProblemaDeCamara.noDisponible);
    await tester.pumpWidget(b.montar());
    await tester.pump();
    await tester.enterText(find.byType(TextField), _invitacion);
    await tester.tap(find.text(TextosPasanaku.usarEnlace));
    await tester.pumpAndSettle();
    expect(find.text('unirse $_uuid.$_hash'), findsOneWidget);
  });

  testWidgets('inválido: pegar un enlace cualquiera se rechaza en su lugar', (
    tester,
  ) async {
    final b = _Banco(problema: ProblemaDeCamara.otro);
    await tester.pumpWidget(b.montar());
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'https://ejemplo.com');
    await tester.tap(find.text(TextosPasanaku.usarEnlace));
    await tester.pump();
    expect(find.text(TextosPasanaku.enlaceInvalido), findsOneWidget);
  });

  testWidgets('correcto: cámara denegada ofrece Abrir ajustes y lo abre', (
    tester,
  ) async {
    final b = _Banco(problema: ProblemaDeCamara.denegado);
    await tester.pumpWidget(b.montar());
    await tester.pump();
    await tester.tap(find.text(TextosPasanaku.abrirAjustes));
    await tester.pump();
    expect(b.ajustes.aperturas, 1);
  });

  testWidgets(
    'límite: si el sistema no abre los ajustes, la pantalla sigue en pie',
    (tester) async {
      final b = _Banco(
        problema: ProblemaDeCamara.denegado,
        ajustes: _Ajustes(resultado: false),
      );
      await tester.pumpWidget(b.montar());
      await tester.pump();
      await tester.tap(find.text(TextosPasanaku.abrirAjustes));
      await tester.pump();
      expect(b.ajustes.aperturas, 1);
      expect(find.text(TextosPasanaku.camaraDenegadaTitulo), findsOneWidget);
      expect(find.text(TextosPasanaku.usarEnlace), findsOneWidget);
    },
  );

  for (final p in [ProblemaDeCamara.noDisponible, ProblemaDeCamara.otro]) {
    testWidgets('inválido: $p no ofrece Abrir ajustes (no ayudaría)', (
      tester,
    ) async {
      final b = _Banco(problema: p);
      await tester.pumpWidget(b.montar());
      await tester.pump();
      expect(find.text(TextosPasanaku.abrirAjustes), findsNothing);
    });
  }
}
