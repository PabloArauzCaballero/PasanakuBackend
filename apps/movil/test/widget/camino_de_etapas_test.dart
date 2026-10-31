import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:aportaya_diseno/moleculas/barra_de_etapa.dart';
import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/consultar_puntaje.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mi_puntaje.dart';
import 'package:aportaya_movil/pantallas/reputacion/dominio/camino_de_etapas.dart';
import 'package:aportaya_movil/pantallas/reputacion/dominio/cifras_de_etapa.dart';
import 'package:aportaya_movil/pantallas/reputacion/pantalla_camino_de_etapas.dart';
import 'package:aportaya_movil/pantallas/reputacion/textos_de_etapas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

// Datos SINTÉTICOS: los tramos copian el catálogo de etapas del plan del trabajo
// (H1.S6, PROPUESTO 2026-10-30), no un catálogo confirmado. El nombre viaja corto,
// como lo manda el servidor; el «Etapa N · » lo pone la app.
const _tramos = [
  ('RECUPERACION', 'En recuperación', '0.00', '500.00'),
  ('INICIAL', 'Inicial', '500.00', '550.00'),
  ('EN_MARCHA', 'En marcha', '550.00', '600.00'),
  ('CONSTANTE', 'Constante', '600.00', '650.00'),
  ('PUNTUAL', 'Puntual', '650.00', '700.00'),
  ('COMPROMETIDO', 'Comprometido', '700.00', '760.00'),
  ('SOLIDO', 'Sólido', '760.00', '820.00'),
  ('DESTACADO', 'Destacado', '820.00', '880.00'),
  ('EXPERTO', 'Experto', '880.00', '940.00'),
  ('EJEMPLAR', 'Ejemplar', '940.00', '980.00'),
  ('PILAR', 'Pilar', '980.00', '1000.00'),
];

List<EtapaDelCatalogo> _catalogo({
  Map<int, DesbloqueoDeEtapa> extra = const {},
}) => [
  for (var i = 0; i < _tramos.length; i++)
    EtapaDelCatalogo(
      codigo: _tramos[i].$1,
      nombre: _tramos[i].$2,
      orden: i,
      desde: _tramos[i].$3,
      hasta: _tramos[i].$4,
      desbloqueo: extra[i],
    ),
];

EtapaDelPuntaje _etapa(int orden, String progreso) => EtapaDelPuntaje(
  codigo: _tramos[orden].$1,
  nombre: _tramos[orden].$2,
  orden: orden,
  desde: _tramos[orden].$3,
  hasta: _tramos[orden].$4,
  progreso: progreso,
  siguiente: orden + 1 < _tramos.length
      ? EtapaSiguiente(
          codigo: _tramos[orden + 1].$1,
          nombre: _tramos[orden + 1].$2,
          desde: _tramos[orden + 1].$3,
        )
      : null,
);

final _enMarcha = PuntajeDelUsuario(
  tieneHistorial: true,
  puntaje: '575',
  nivelDeConfianza: 'EN_CONSTRUCCION',
  etapa: _etapa(2, '0.5000'),
);

final _pilar = PuntajeDelUsuario(
  tieneHistorial: true,
  puntaje: '1000',
  nivelDeConfianza: 'ALTO',
  etapa: _etapa(10, '1.0000'),
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
  Future<List<EtapaDelCatalogo>> Function()? catalogo,
}) => ProviderScope(
  // Igual que `main.dart`: sin reintentos automáticos, un error queda a la vista.
  retry: (_, _) => null,
  overrides: [
    puntajeProvider('u1').overrideWith((ref) async => puntaje),
    catalogoDeEtapasProvider.overrideWith(
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
      (w) => w is CustomPaint && w.painter is PintorDeAvanceDeEtapa,
    ),
  );
  return (pintura.painter! as PintorDeAvanceDeEtapa).avance;
}

void main() {
  for (final brillo in Brightness.values) {
    testWidgets(
      'puntaje 575 → Etapa 2 · En marcha, 50 % hacia Constante (${brillo.name})',
      (tester) async {
        _telefono(tester);
        final semantica = tester.ensureSemantics();
        await tester.pumpWidget(
          _app(
            const PantallaMiPuntaje(usuarioId: 'u1'),
            puntaje: _enMarcha,
            brillo: brillo,
          ),
        );
        await tester.pumpAndSettle();
        expect(find.text('Etapa 2 · En marcha'), findsOneWidget);
        expect(find.text('50 %'), findsOneWidget);
        expect(
          find.text('Te faltan 25 puntos para Etapa 3 · Constante'),
          findsOneWidget,
        );
        expect(_avancePintado(tester), 0.5);
        expect(
          find.bySemanticsLabel('Avance hacia Etapa 3, Constante'),
          findsOneWidget,
        );
        expect(
          find.textContaining(RegExp('arena', caseSensitive: false)),
          findsNothing,
        );
        expect(tester.takeException(), isNull);
        semantica.dispose();
      },
    );
  }

  testWidgets(
    'puntaje 1000 → Etapa 10 · Pilar, sin siguiente y con la barra llena',
    (tester) async {
      _telefono(tester);
      await tester.pumpWidget(
        _app(const PantallaMiPuntaje(usuarioId: 'u1'), puntaje: _pilar),
      );
      await tester.pumpAndSettle();
      expect(find.text('Etapa 10 · Pilar'), findsOneWidget);
      expect(find.text('100 %'), findsOneWidget);
      expect(find.textContaining('Te faltan'), findsNothing);
      expect(find.textContaining('etapa más alta'), findsOneWidget);
      expect(_avancePintado(tester), 1.0);
    },
  );

  testWidgets('con movimiento reducido la barra aparece ya en su valor', (
    tester,
  ) async {
    Widget barra({required bool reducir}) => MaterialApp(
      theme: temaDesde(Tokens.claro, Brightness.light),
      home: MediaQuery(
        data: MediaQueryData(disableAnimations: reducir),
        child: const Scaffold(
          body: BarraDeEtapa(
            nombre: 'Etapa 2 · En marcha',
            orden: 2,
            avance: 5000,
            etiquetaDeAvance: 'Avance hacia Etapa 3, Constante',
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
      'el camino muestra todas las etapas con su estado (${brillo.name})',
      (tester) async {
        _telefono(tester);
        final semantica = tester.ensureSemantics();
        await tester.pumpWidget(
          _app(
            const PantallaCaminoDeEtapas(usuarioId: 'u1'),
            puntaje: _enMarcha,
            brillo: brillo,
          ),
        );
        await tester.pumpAndSettle();
        expect(
          find.bySemanticsLabel(
            'Etapa 2, En marcha, la tuya, desde 550 puntos',
          ),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'Etapa 1, Inicial, ya la pasaste, desde 500 puntos',
          ),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'En recuperación, ya la pasaste, desde 0 puntos',
          ),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'Etapa 3, Constante, todavía por delante, llegás con 600 puntos',
          ),
          findsOneWidget,
        );
        expect(
          find.bySemanticsLabel(
            'Etapa 10, Pilar, todavía por delante, llegás con 980 puntos',
          ),
          findsOneWidget,
        );
        expect(find.text('Etapa 2 · En marcha'), findsWidgets);
        expect(find.text('En recuperación'), findsOneWidget);
        expect(find.textContaining('No se compra'), findsOneWidget);
        expect(tester.takeException(), isNull);
        semantica.dispose();
      },
    );
  }

  testWidgets('sin historial: Etapa 1 · Inicial, nunca un cero ni un vacío', (
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
          etapa: _etapa(1, '0.0000'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Etapa 1 · Inicial'), findsOneWidget);
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
        const PantallaCaminoDeEtapas(usuarioId: 'u1'),
        puntaje: _enMarcha,
        catalogo: () async => const [],
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Todavía no hay etapas'), findsOneWidget);
  });

  testWidgets('si el catálogo falla se ve el error con reintento', (
    tester,
  ) async {
    _telefono(tester);
    await tester.pumpWidget(
      _app(
        const PantallaCaminoDeEtapas(usuarioId: 'u1'),
        puntaje: _enMarcha,
        catalogo: () async => throw Exception('sin red'),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(EstadoError), findsOneWidget);
  });

  test('solo un desbloqueo VIGENTE se anuncia', () {
    DesbloqueoDeEtapa d(DesbloqueoDeEtapaEstadoEnum e) => DesbloqueoDeEtapa(
      cuposMax: 12,
      aporteMax: '500.00',
      moneda: DesbloqueoDeEtapaMonedaEnum.BOB,
      estado: e,
    );
    expect(beneficioDe(d(DesbloqueoDeEtapaEstadoEnum.PROPUESTO)), isNull);
    expect(
      beneficioDe(d(DesbloqueoDeEtapaEstadoEnum.VIGENTE)),
      contains('hasta 12 cupos'),
    );
    expect(beneficioDe(null), isNull);
  });

  test('el rótulo numera las etapas y deja sin número la de recuperación', () {
    expect(TextosDeEtapas.rotulo(2, 'En marcha'), 'Etapa 2 · En marcha');
    expect(TextosDeEtapas.rotulo(10, 'Pilar'), 'Etapa 10 · Pilar');
    expect(TextosDeEtapas.rotulo(0, 'En recuperación'), 'En recuperación');
    expect(
      TextosDeEtapas.rotulo(2, 'Etapa 2 · En marcha'),
      'Etapa 2 · En marcha',
    );
    expect(
      TextosDeEtapas.rotuloAccesible(2, 'En marcha'),
      'Etapa 2, En marcha',
    );
  });

  test('las cifras de la etapa son enteras y no inventan valores', () {
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
