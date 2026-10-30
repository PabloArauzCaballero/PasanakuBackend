import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/moleculas/barra_de_arena.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/consultar_puntaje.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_puntaje.dart';
import 'package:aportaya_movil/pantallas/reputacion/dominio/camino_de_arenas.dart';
import 'package:aportaya_movil/pantallas/reputacion/dominio/cifras_de_arena.dart';
import 'package:aportaya_movil/pantallas/reputacion/pantalla_camino_de_arenas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

// Datos SINTÉTICOS: los tramos copian el catálogo PROPUESTO del plan
// (docs/trabajo/2026-10-30-arenas-e-inicio-75/PLAN.md), no un catálogo confirmado.
const _tramos = [
  ('CAMPAMENTO', 'Campamento', '0.00', '500.00'),
  ('SEMILLA', 'Semilla', '500.00', '550.00'),
  ('BROTE', 'Brote', '550.00', '600.00'),
  ('AGUAYO', 'Aguayo', '600.00', '650.00'),
  ('FERIA', 'Feria', '650.00', '700.00'),
  ('MERCADO', 'Mercado', '700.00', '760.00'),
  ('ALTIPLANO', 'Altiplano', '760.00', '820.00'),
  ('SALAR', 'Salar', '820.00', '880.00'),
  ('ILLIMANI', 'Illimani', '880.00', '940.00'),
  ('CONDOR', 'Cóndor', '940.00', '980.00'),
  ('INTI', 'Inti', '980.00', '1000.00'),
];

List<ArenaDelCatalogo> _catalogo({
  Map<int, DesbloqueoDeArena> extra = const {},
}) => [
  for (var i = 0; i < _tramos.length; i++)
    ArenaDelCatalogo(
      codigo: _tramos[i].$1,
      nombre: _tramos[i].$2,
      orden: i,
      desde: _tramos[i].$3,
      hasta: _tramos[i].$4,
      desbloqueo: extra[i],
    ),
];

ArenaDelPuntaje _arena(int orden, String progreso) => ArenaDelPuntaje(
  codigo: _tramos[orden].$1,
  nombre: _tramos[orden].$2,
  orden: orden,
  desde: _tramos[orden].$3,
  hasta: _tramos[orden].$4,
  progreso: progreso,
  siguiente: orden + 1 < _tramos.length
      ? ArenaSiguiente(
          codigo: _tramos[orden + 1].$1,
          nombre: _tramos[orden + 1].$2,
          desde: _tramos[orden + 1].$3,
        )
      : null,
);

final _brote = PuntajeDelUsuario(
  tieneHistorial: true,
  puntaje: '575',
  nivelDeConfianza: 'EN_CONSTRUCCION',
  arena: _arena(2, '0.5000'),
);

final _inti = PuntajeDelUsuario(
  tieneHistorial: true,
  puntaje: '1000',
  nivelDeConfianza: 'ALTO',
  arena: _arena(10, '1.0000'),
);

void _telefono(WidgetTester tester) {
  tester.view.physicalSize = const Size(412 * 3, 1400 * 3);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
}

Widget _app(
  Widget pantalla, {
  required PuntajeDelUsuario puntaje,
  Brightness brillo = Brightness.light,
  Future<List<ArenaDelCatalogo>> Function()? catalogo,
}) => ProviderScope(
  // Igual que `main.dart`: sin reintentos automáticos, un error queda a la vista.
  retry: (_, _) => null,
  overrides: [
    puntajeProvider('u1').overrideWith((ref) async => puntaje),
    catalogoDeArenasProvider.overrideWith(
      (ref) => (catalogo ?? () async => _catalogo())(),
    ),
  ],
  child: MaterialApp(
    theme: temaDesde(
      brillo == Brightness.dark ? Tokens.oscuro : Tokens.claro,
      brillo,
    ),
    home: pantalla,
  ),
);

double _avancePintado(WidgetTester tester) {
  final pintura = tester.widget<CustomPaint>(
    find.byWidgetPredicate(
      (w) => w is CustomPaint && w.painter is PintorDeAvanceDeArena,
    ),
  );
  return (pintura.painter! as PintorDeAvanceDeArena).avance;
}

void main() {
  for (final brillo in Brightness.values) {
    testWidgets('puntaje 575 → Brote, 50 % hacia Aguayo (${brillo.name})', (
      tester,
    ) async {
      _telefono(tester);
      final semantica = tester.ensureSemantics();
      await tester.pumpWidget(
        _app(
          const PantallaMiPuntaje(usuarioId: 'u1'),
          puntaje: _brote,
          brillo: brillo,
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Brote'), findsOneWidget);
      expect(find.text('50 %'), findsOneWidget);
      expect(find.text('Te faltan 25 puntos para Aguayo'), findsOneWidget);
      expect(_avancePintado(tester), 0.5);
      expect(
        find.bySemanticsLabel('Avance hacia la arena Aguayo'),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      semantica.dispose();
    });
  }

  testWidgets('puntaje 1000 → Inti, sin siguiente y con la barra llena', (
    tester,
  ) async {
    _telefono(tester);
    await tester.pumpWidget(
      _app(const PantallaMiPuntaje(usuarioId: 'u1'), puntaje: _inti),
    );
    await tester.pumpAndSettle();
    expect(find.text('Inti'), findsOneWidget);
    expect(find.text('100 %'), findsOneWidget);
    expect(find.textContaining('Te faltan'), findsNothing);
    expect(find.textContaining('arena más alta'), findsOneWidget);
    expect(_avancePintado(tester), 1.0);
  });

  testWidgets('con movimiento reducido la barra aparece ya en su valor', (
    tester,
  ) async {
    Widget barra({required bool reducir}) => MaterialApp(
      theme: temaDesde(Tokens.claro, Brightness.light),
      home: MediaQuery(
        data: MediaQueryData(disableAnimations: reducir),
        child: const Scaffold(
          body: BarraDeArena(
            nombre: 'Brote',
            orden: 2,
            avance: 5000,
            etiquetaDeAvance: 'Avance hacia la arena Aguayo',
            ordenSiguiente: 3,
          ),
        ),
      ),
    );

    await tester.pumpWidget(barra(reducir: true));
    expect(_avancePintado(tester), 0.5);
    expect(tester.hasRunningAnimations, isFalse);

    // Contraste: sin reducir, el primer cuadro arranca vacío y se llena animando.
    await tester.pumpWidget(const SizedBox());
    await tester.pumpWidget(barra(reducir: false));
    expect(_avancePintado(tester), 0.0);
    expect(tester.hasRunningAnimations, isTrue);
    await tester.pumpAndSettle();
    expect(_avancePintado(tester), 0.5);
  });

  for (final brillo in Brightness.values) {
    testWidgets(
      'el camino muestra todas las arenas con su estado (${brillo.name})',
      (tester) async {
        _telefono(tester);
        final semantica = tester.ensureSemantics();
        await tester.pumpWidget(
          _app(
            const PantallaCaminoDeArenas(usuarioId: 'u1'),
            puntaje: _brote,
            brillo: brillo,
          ),
        );
        await tester.pumpAndSettle();
        expect(
          find.bySemanticsLabel('Arena Brote, la tuya, desde 550 puntos'),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel('Arena Semilla, alcanzada, desde 500 puntos'),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'Arena Aguayo, bloqueada, se abre con 600 puntos',
          ),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'Arena Inti, bloqueada, se abre con 980 puntos',
          ),
          findsOneWidget,
        );
        expect(find.textContaining('No se compra'), findsOneWidget);
        expect(tester.takeException(), isNull);
        semantica.dispose();
      },
    );
  }

  testWidgets('sin historial: arena Semilla, nunca un cero ni un vacío', (
    tester,
  ) async {
    _telefono(tester);
    await tester.pumpWidget(
      _app(
        const PantallaMiPuntaje(usuarioId: 'u1'),
        puntaje: PuntajeDelUsuario(
          tieneHistorial: false,
          puntaje: '0',
          nivelDeConfianza: 'SIN_HISTORIAL',
          arena: _arena(1, '0.0000'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Semilla'), findsOneWidget);
    expect(find.text('Recién empezás'), findsOneWidget);
    expect(find.text('0'), findsNothing);
    expect(find.textContaining('Te faltan'), findsNothing);
  });

  testWidgets('catálogo vacío: orienta en vez de dibujar un camino mudo', (
    tester,
  ) async {
    _telefono(tester);
    await tester.pumpWidget(
      _app(
        const PantallaCaminoDeArenas(usuarioId: 'u1'),
        puntaje: _brote,
        catalogo: () async => const [],
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Todavía no hay arenas'), findsOneWidget);
  });

  testWidgets('si el catálogo falla se ve el error con reintento', (
    tester,
  ) async {
    _telefono(tester);
    await tester.pumpWidget(
      _app(
        const PantallaCaminoDeArenas(usuarioId: 'u1'),
        puntaje: _brote,
        catalogo: () async => throw Exception('sin red'),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(EstadoError), findsOneWidget);
  });

  test('solo un desbloqueo VIGENTE se anuncia', () {
    DesbloqueoDeArena d(DesbloqueoDeArenaEstadoEnum e) => DesbloqueoDeArena(
      cuposMax: 12,
      aporteMax: '500.00',
      moneda: DesbloqueoDeArenaMonedaEnum.BOB,
      estado: e,
    );
    expect(beneficioDe(d(DesbloqueoDeArenaEstadoEnum.PROPUESTO)), isNull);
    expect(
      beneficioDe(d(DesbloqueoDeArenaEstadoEnum.VIGENTE)),
      contains('hasta 12 cupos'),
    );
    expect(beneficioDe(null), isNull);
  });

  test('las cifras de la arena son enteras y no inventan valores', () {
    expect(diezmilesimosDe('0.5000'), 5000);
    expect(diezmilesimosDe('1.0000'), 10000);
    expect(diezmilesimosDe('0.5'), isNull);
    expect(diezmilesimosDe('1.5000'), isNull);
    expect(centesimosDe('575.00'), 57500);
    expect(centesimosDe('575'), 57500);
    expect(centesimosDe('549.99'), 54999);
    expect(centesimosDe('n/d'), isNull);
    expect(centesimosQueFaltan(puntaje: '575', desde: '600.00'), 2500);
    expect(centesimosQueFaltan(puntaje: '620', desde: '600.00'), 0);
    expect(puntosLegibles(60000), '600');
    expect(puntosLegibles(60050), '600,50');
  });
}
