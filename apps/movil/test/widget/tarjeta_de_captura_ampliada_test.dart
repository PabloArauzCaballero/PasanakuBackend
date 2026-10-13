import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/tarjeta_de_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/visor_ampliado_de_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

const _captura = Captura(
  ruta: 'imagen-inexistente.png',
  bytes: 128,
  ancho: 1600,
  alto: 1000,
  sha256Corto: '123456789abc',
);

void main() {
  testWidgets('imagen local tiene cache/gapless y abre el visor con zoom', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 2;
    tester.view.physicalSize = const Size(720, 1600);
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: const Scaffold(
          body: Column(
            children: [
              Expanded(
                child: TarjetaDeCaptura(
                  cara: CaraDelCarril.anverso,
                  captura: _captura,
                ),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.runAsync(() => Future<void>.delayed(Duration.zero));
    await tester.pumpAndSettle();

    final imagen = tester.widget<Image>(find.byType(Image).first);
    expect(
      anchoDeCacheDeCaptura(tester.element(find.byType(TarjetaDeCaptura))),
      720,
    );
    expect((imagen.image as ResizeImage).width, 720);
    expect(imagen.gaplessPlayback, isTrue);
    // El archivo corrupto invoca errorBuilder y deja un marco vacío accesible.
    expect(imagen.errorBuilder, isNotNull);
    expect(find.byKey(const ValueKey('hueco-de-captura')), findsOneWidget);
    expect(find.bySemanticsLabel('Ampliar Anverso del carnet'), findsOneWidget);
    await tester.tap(find.bySemanticsLabel('Ampliar Anverso del carnet'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(find.byType(InteractiveViewer), findsOneWidget);
    expect(find.byTooltip('Cerrar imagen ampliada'), findsOneWidget);
    final visor = tester.widget<InteractiveViewer>(
      find.byType(InteractiveViewer),
    );
    expect(visor.minScale, 1);
    expect(visor.maxScale, 5);
    final heroes = tester.widgetList<Hero>(find.byType(Hero));
    expect(heroes, hasLength(2));
    expect(heroes.map((hero) => hero.tag).toSet(), {
      etiquetaHeroDeCaptura(_captura),
    });
    await tester.tap(find.byTooltip('Cerrar imagen ampliada'));
    await tester.pumpAndSettle();
    expect(find.byType(InteractiveViewer), findsNothing);
    semantics.dispose();
  });
}
