import 'package:aportaya_diseno/atomos/barra_de_puntos.dart';
import 'package:aportaya_diseno/atomos/chip_estado.dart';
import 'package:aportaya_diseno/atomos/movimiento.dart';
import 'package:aportaya_diseno/atomos/numero_que_sube.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_capturas.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/riel_de_capturas.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_pautas_de_fotos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import '../goldens/cargar_fuentes_de_aplicacion.dart';

Captura _captura(CaraDelCarril cara) => Captura(
  ruta: '/${cara.name}.png',
  bytes: 128,
  ancho: 1600,
  alto: 1000,
  sha256Corto: '123456789abc',
);

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('cinco láminas, avance tras guardar y puntos tocables', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(360, 800);
    final semantics = tester.ensureSemantics();
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    var caraCapturada = CaraDelCarril.anverso;
    final contenedor = ProviderContainer(
      overrides: [generacionDeSesionProvider.overrideWithValue(0)],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: SingleChildScrollView(
              child: PasoCapturas(
                onContinuar: () {},
                capturador: (_, cara, {required listas}) async {
                  caraCapturada = cara;
                  return {cara: _captura(cara)};
                },
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Tus capturas (0 de 5)'), findsOneWidget);
    expect(
      tester.widget<NumeroQueSube>(find.byType(NumeroQueSube)).duracion,
      Movimiento.popDeCaptura,
    );
    final pageView = tester.widget<PageView>(find.byType(PageView));
    expect(pageView.controller, isA<PageController>());
    expect(pageView.controller!.viewportFraction, closeTo(0.86, 0.001));
    final viewport = tester.getSize(find.byType(PageView));
    expect(viewport.height, greaterThanOrEqualTo(220));
    final tarjeta = find.byType(TarjetaDeCaptura).first;
    expect(tester.getSize(tarjeta).height, 300);
    expect(tester.getSize(tarjeta).width, lessThan(viewport.width));
    await expectLater(
      find.byType(PageView),
      matchesGoldenFile('goldens/capturas_peek_360.png'),
    );
    for (var i = 1; i <= 5; i++) {
      expect(find.bySemanticsLabel('Lámina $i de 5'), findsOneWidget);
    }
    final punto = find.bySemanticsLabel('Lámina 4 de 5');
    expect(tester.getSize(punto), const Size(48, 48));
    await tester.tap(punto);
    await tester.pumpAndSettle();
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual, 3);

    await tester.tap(find.bySemanticsLabel('Lámina 1 de 5'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tomar foto'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Abrir el escáner'));
    await tester.pumpAndSettle();

    expect(caraCapturada, CaraDelCarril.anverso);
    expect(find.text('Tus capturas (1 de 5)'), findsOneWidget);
    expect(
      tester
          .widgetList<AnimatedSwitcher>(
            find.descendant(
              of: find.byType(TarjetaDeCaptura),
              matching: find.byType(AnimatedSwitcher),
            ),
          )
          .any((cambio) => cambio.duration == Movimiento.popDeCaptura),
      isTrue,
    );
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual, 1);
    expect(find.bySemanticsLabel('Lámina 1 de 5, completa'), findsOneWidget);

    for (final cara in CaraDelCarril.values.skip(1)) {
      await contenedor
          .read(altaProvider.notifier)
          .registrarCaptura(cara, _captura(cara));
    }
    await tester.pumpAndSettle();
    expect(find.text('Tus capturas (5 de 5)'), findsOneWidget);
    expect(
      tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).completados,
      {0, 1, 2, 3, 4},
    );
    final tokens = Tokens.of(tester.element(find.byType(BarraDePuntos)));
    final coloresDePuntos = tester
        .widgetList<AnimatedContainer>(
          find.descendant(
            of: find.byType(BarraDePuntos),
            matching: find.byType(AnimatedContainer),
          ),
        )
        .map((punto) => (punto.decoration! as BoxDecoration).color);
    expect(coloresDePuntos, everyElement(tokens.ok));
    final insignias = tester.widgetList<ChipEstado>(find.byType(ChipEstado));
    expect(insignias, isNotEmpty);
    expect(insignias.every((insignia) => insignia.tono == Tono.ok), isTrue);
    semantics.dispose();
  });

  testWidgets('el marco conserva las dos elevaciones tokenizadas', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(360, 800);
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    for (final (tokens, brillo) in [
      (Tokens.claro, Brightness.light),
      (Tokens.oscuro, Brightness.dark),
    ]) {
      await tester.pumpWidget(
        MaterialApp(
          theme: temaDesde(tokens, brillo),
          home: const Scaffold(
            body: TarjetaDeCaptura(cara: CaraDelCarril.anverso, captura: null),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final decoraciones = tester
          .widgetList<DecoratedBox>(
            find.descendant(
              of: find.byType(TarjetaDeCaptura),
              matching: find.byType(DecoratedBox),
            ),
          )
          .map((marco) => marco.decoration)
          .whereType<BoxDecoration>()
          .where(
            (decoracion) =>
                decoracion.borderRadius == BorderRadius.circular(Radios.lg),
          )
          .toList();
      expect(decoraciones, hasLength(1));
      final decoracion = decoraciones.single;
      expect(decoracion.boxShadow, Elevacion.tarjeta(tokens));
      expect(decoracion.borderRadius, BorderRadius.circular(Radios.lg));
      await expectLater(
        find.byType(TarjetaDeCaptura),
        matchesGoldenFile(
          brillo == Brightness.light
              ? 'goldens/capturas_marco_claro_360.png'
              : 'goldens/capturas_marco_oscuro_360.png',
        ),
      );
    }
  });

  testWidgets('el botón de pautas abre el contenido disponible', (
    tester,
  ) async {
    final contenedor = ProviderContainer(
      overrides: [generacionDeSesionProvider.overrideWithValue(0)],
    );
    addTearDown(contenedor.dispose);
    final router = GoRouter(
      initialLocation: '/registro',
      routes: [
        GoRoute(
          path: '/registro',
          builder: (context, state) => Scaffold(
            body: SingleChildScrollView(
              child: PasoCapturas(onContinuar: () {}),
            ),
          ),
        ),
        GoRoute(
          path: '/registro/pautas',
          builder: (context, state) => const PantallaDePautasDeFotos(),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Ver pautas para tus fotos'));
    await tester.pumpAndSettle();
    expect(find.text('Pautas para tus fotos'), findsOneWidget);
    expect(find.text('Buena iluminación'), findsOneWidget);
    expect(find.text('Correcto'), findsWidgets);
    expect(find.text('Incorrecto'), findsWidgets);

    await tester.tap(find.text('Foto de frente'));
    await tester.pumpAndSettle();
    expect(find.text('Pautas para las fotos de rostro'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Sin accesorios'),
      300,
      scrollable: find.descendant(
        of: find.byType(PantallaDePautasDeFotos),
        matching: find.byType(Scrollable),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Sin accesorios'), findsOneWidget);
    expect(find.text('Correcto'), findsWidgets);
  });

  testWidgets('volver de prueba de vida actualiza las capturas y el carrusel', (
    tester,
  ) async {
    final contenedor = ProviderContainer(
      overrides: [generacionDeSesionProvider.overrideWithValue(0)],
    );
    addTearDown(contenedor.dispose);
    final router = GoRouter(
      initialLocation: '/registro',
      routes: [
        GoRoute(
          path: '/registro',
          builder: (context, state) => Scaffold(
            body: SingleChildScrollView(
              child: PasoCapturas(onContinuar: () {}),
            ),
          ),
        ),
        GoRoute(
          path: '/registro/prueba-de-vida',
          builder: (context, state) {
            final poses = state.extra! as List<PoseDeVida>;
            return Scaffold(
              body: Column(
                children: [
                  Text(poses.map((pose) => pose.name).join(',')),
                  TextButton(
                    onPressed: () => context.pop({
                      CaraDelCarril.selfie: _captura(CaraDelCarril.selfie),
                      CaraDelCarril.perfilIzquierdo: _captura(
                        CaraDelCarril.perfilIzquierdo,
                      ),
                      CaraDelCarril.perfilDerecho: _captura(
                        CaraDelCarril.perfilDerecho,
                      ),
                    }),
                    child: const Text('devolver capturas'),
                  ),
                ],
              ),
            );
          },
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      ),
    );
    await tester.pumpAndSettle();

    tester
        .widget<RielDeCapturas>(find.byType(RielDeCapturas))
        .onElegir(CaraDelCarril.selfie);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tomar foto'));
    await tester.pumpAndSettle();

    expect(find.text('frente,izquierda,derecha'), findsOneWidget);
    await tester.tap(find.text('devolver capturas'));
    await tester.pumpAndSettle();

    expect(find.text('Tus capturas (3 de 5)'), findsOneWidget);
    expect(tester.widget<BarraDePuntos>(find.byType(BarraDePuntos)).actual, 0);
  });
}
