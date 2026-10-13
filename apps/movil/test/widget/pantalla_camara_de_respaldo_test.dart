import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/infraestructura/escaner_de_documentos.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/sacar_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/consejos_para_la_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/guia_de_encuadre.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

void main() {
  Future<String> crearCarnetTemporal(
    WidgetTester tester, {
    int ancho = 1600,
    int alto = 1009,
  }) async => (await tester.runAsync(() async {
    final directorio = await Directory.systemTemp.createTemp('camara-respaldo');
    final grabador = ui.PictureRecorder();
    ui.Canvas(grabador).drawRect(
      ui.Rect.fromLTWH(0, 0, ancho.toDouble(), alto.toDouble()),
      ui.Paint()..color = const ui.Color(0xFF406080),
    );
    final imagen = await grabador.endRecording().toImage(ancho, alto);
    final datos = await imagen.toByteData(format: ui.ImageByteFormat.png);
    final archivo = File('${directorio.path}/carnet.png');
    await archivo.writeAsBytes(datos!.buffer.asUint8List());
    return archivo.path;
  }))!;

  Future<Object?> abrirCamara(
    WidgetTester tester, {
    required CamaraDePrueba camara,
    required String foto,
  }) async {
    Object? resultado;
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: Builder(
              builder: (context) => TextButton(
                onPressed: () async {
                  resultado = await Navigator.of(context).push<Object?>(
                    MaterialPageRoute(
                      builder: (_) => PantallaDeCamara(
                        cara: CaraDelCarril.anverso,
                        camara: camara..foto = foto,
                      ),
                    ),
                  );
                },
                child: const Text('Abrir cámara de respaldo'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Abrir cámara de respaldo'));
    await tester.pumpAndSettle();
    return resultado;
  }

  testWidgets(
    'el carnet usa visor a pantalla completa con marco ID-1 y consejos superpuestos',
    (tester) async {
      final camara = CamaraDePrueba();
      final ruta = await crearCarnetTemporal(tester);
      await abrirCamara(tester, camara: camara, foto: ruta);

      final visor = tester.getRect(find.byKey(CamaraDePrueba.claveVista));
      expect(
        visor.width,
        closeTo(
          tester.view.physicalSize.width / tester.view.devicePixelRatio,
          1,
        ),
      );
      expect(visor.height, greaterThan(500));
      expect(camara.frontal, isFalse);
      expect(find.byType(GuiaDeEncuadre), findsOneWidget);
      expect(find.byType(ConsejosParaLaFoto), findsOneWidget);
      final marcoSuperpuesto = tester.getRect(find.byType(GuiaDeEncuadre));
      final consejosSuperpuestos = tester.getRect(
        find.byType(ConsejosParaLaFoto),
      );
      expect(marcoSuperpuesto.size, visor.size);
      expect(consejosSuperpuestos.overlaps(visor), isTrue);
      expect(find.text('Tomar foto'), findsOneWidget);
      expect(find.text('Cancelar'), findsOneWidget);

      const visorSize = Size(800, 600);
      final marco = marcoDeCarnetId1(visorSize);
      expect(marco.width / marco.height, closeTo(1.586, 0.001));
      expect(marco.width, lessThan(visorSize.width));

      await _borrarTemporal(tester, ruta);
    },
  );

  testWidgets('Tomar foto entrega el carnet capturado', (tester) async {
    final camara = CamaraDePrueba();
    final ruta = await crearCarnetTemporal(tester);
    Object? resultado;
    await _montarFlujo(tester, camara, ruta, (value) => resultado = value);

    await tester.runAsync(() async {
      await tester.tap(find.text('Tomar foto'));
      await Future<void>.delayed(const Duration(milliseconds: 300));
    });
    await tester.pumpAndSettle();

    expect(camara.fotoTomada, isTrue);
    expect(camara.liberada, isTrue);
    expect(resultado, isA<Captura>());
    expect((resultado! as Captura).ancho, 1600);
    await _borrarTemporal(tester, ruta);
  });

  testWidgets(
    'foto rechazada en cámara de respaldo puede usarse sin repetirla',
    (tester) async {
      final camara = CamaraDePrueba();
      final ruta = await crearCarnetTemporal(tester, ancho: 900, alto: 600);
      Object? resultado;
      await _montarFlujo(tester, camara, ruta, (value) => resultado = value);

      await tester.runAsync(() async {
        await tester.tap(find.text('Tomar foto'));
        await Future<void>.delayed(const Duration(milliseconds: 300));
      });
      await tester.pumpAndSettle();

      expect(find.text('Repetí la foto'), findsOneWidget);
      expect(find.text('Usar esta foto igual'), findsOneWidget);
      await tester.tap(find.text('Usar esta foto igual'));
      await tester.pumpAndSettle();

      expect(resultado, isA<Captura>());
      expect((resultado! as Captura).ruta, ruta);
      expect(camara.fotoTomada, isTrue);
      expect(camara.liberada, isTrue);
      await _borrarTemporal(tester, ruta);
    },
  );

  testWidgets(
    'el flujo de captura envuelve la foto aceptada en el mapa del alta',
    (tester) async {
      final camara = CamaraDePrueba();
      final ruta = await crearCarnetTemporal(tester, ancho: 900, alto: 600);
      Object? resultado;
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (context, state) => Scaffold(
              body: Builder(
                builder: (context) => TextButton(
                  onPressed: () async {
                    resultado = await sacarCaptura(
                      context,
                      CaraDelCarril.anverso,
                      escanear: () async => const EscaneoNoDisponible(
                        'el escáner no está disponible',
                      ),
                    );
                  },
                  child: const Text('Capturar anverso'),
                ),
              ),
            ),
          ),
          GoRoute(
            path: '/registro/camara',
            builder: (context, state) => PantallaDeCamara(
              cara: state.extra! as CaraDelCarril,
              camara: camara..foto = ruta,
            ),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(
        MaterialApp.router(
          theme: temaDesde(Tokens.claro, Brightness.light),
          routerConfig: router,
        ),
      );
      await tester.tap(find.text('Capturar anverso'));
      await tester.pumpAndSettle();

      await tester.runAsync(() async {
        await tester.tap(find.text('Tomar foto'));
        await Future<void>.delayed(const Duration(milliseconds: 300));
      });
      await tester.pumpAndSettle();
      await tester.tap(find.text('Usar esta foto igual'));
      await tester.pumpAndSettle();

      expect(resultado, isA<Map<CaraDelCarril, Captura>>());
      final capturas = resultado! as Map<CaraDelCarril, Captura>;
      expect(capturas[CaraDelCarril.anverso]!.ruta, ruta);
      await _borrarTemporal(tester, ruta);
    },
  );

  testWidgets('Cancelar cierra la cámara sin devolver una captura', (
    tester,
  ) async {
    final camara = CamaraDePrueba();
    final ruta = await crearCarnetTemporal(tester);
    Object? resultado = 'no-cerrada';
    await _montarFlujo(tester, camara, ruta, (value) => resultado = value);

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    expect(resultado, isNull);
    expect(camara.fotoTomada, isFalse);
    expect(camara.liberada, isTrue);
    await _borrarTemporal(tester, ruta);
  });
}

Future<void> _montarFlujo(
  WidgetTester tester,
  CamaraDePrueba camara,
  String ruta,
  void Function(Object?) alVolver,
) async {
  await tester.pumpWidget(
    ProviderScope(
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async {
                alVolver(
                  await Navigator.of(context).push<Object?>(
                    MaterialPageRoute(
                      builder: (_) => PantallaDeCamara(
                        cara: CaraDelCarril.anverso,
                        camara: camara..foto = ruta,
                      ),
                    ),
                  ),
                );
              },
              child: const Text('Abrir cámara de respaldo'),
            ),
          ),
        ),
      ),
    ),
  );
  await tester.tap(find.text('Abrir cámara de respaldo'));
  await tester.pumpAndSettle();
}

Future<void> _borrarTemporal(WidgetTester tester, String ruta) async {
  await tester.runAsync(() async {
    final parent = Directory(File(ruta).parent.path);
    if (await parent.exists()) await parent.delete(recursive: true);
  });
}

class CamaraDePrueba implements CamaraEnVivo {
  static const claveVista = Key('vista-camara-de-prueba');
  String foto = '';
  bool? frontal;
  bool fotoTomada = false;
  bool liberada = false;

  @override
  Future<EstadoDePermiso> pedirPermiso() async => EstadoDePermiso.concedido;

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {
    this.frontal = frontal;
  }

  @override
  Widget vista() => const ColoredBox(key: claveVista, color: Color(0xFF101010));

  @override
  Future<String> tomarFoto() async {
    fotoTomada = true;
    return foto;
  }

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async =>
      false;

  @override
  Future<void> dejarDeEscuchar() async {}

  @override
  Future<void> liberar() async {
    liberada = true;
  }
}
