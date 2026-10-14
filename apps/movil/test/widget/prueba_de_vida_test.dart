import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/juez_de_pose.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_prueba_de_vida.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/sacar_captura.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

DateTime _reloj = DateTime.utc(2026, 10, 12);

Future<void> _avanzar(WidgetTester tester, Duration duracion) async {
  _reloj = _reloj.add(duracion);
  await tester.pump(duracion);
}

/// Doble de la cámara en vivo: el test le dicta qué cara "ve" el detector.
class CamaraDoble implements CamaraEnVivo {
  CamaraDoble({
    required this.foto,
    this.conDetector = true,
    this.permiso = EstadoDePermiso.concedido,
  });
  final String foto;
  final bool conDetector;
  final EstadoDePermiso permiso;
  void Function(LecturaDeRostro?)? _alLeer;
  var fotosTomadas = 0;
  var conDeteccionPedida = false;

  void ver(LecturaDeRostro? l) => _alLeer?.call(l);

  @override
  Future<EstadoDePermiso> pedirPermiso() async => permiso;
  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async => conDeteccionPedida = conDeteccion;
  @override
  Widget vista() => const SizedBox.expand();
  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async {
    _alLeer = alLeer;
    return conDetector;
  }

  @override
  Future<void> dejarDeEscuchar() async => _alLeer = null;
  @override
  Future<String> tomarFoto() async {
    fotosTomadas++;
    return foto;
  }

  @override
  Future<void> liberar() async {}
}

void main() {
  LecturaDeRostro rostro(double giro) => LecturaDeRostro(
    rostros: 1,
    centroX: 0.5,
    centroY: 0.5,
    ancho: 0.4,
    giro: giro,
  );

  Future<(Directory, String)> foto1080(WidgetTester tester) async =>
      (await tester.runAsync(() async {
        final g = ui.PictureRecorder();
        ui.Canvas(g).drawRect(
          const ui.Rect.fromLTWH(0, 0, 1080, 1920),
          ui.Paint()..color = const ui.Color(0xFF808080),
        );
        final img = await g.endRecording().toImage(1080, 1920);
        final png = await img.toByteData(format: ui.ImageByteFormat.png);
        final dir = await Directory.systemTemp.createTemp('vida');
        final f = File('${dir.path}/selfie.png');
        await f.writeAsBytes(png!.buffer.asUint8List());
        return (dir, f.path);
      }))!;

  /// Abre la pantalla desde un botón para poder leer lo que devuelve al cerrarse.
  Future<(Directory, String)> fotoPequena(WidgetTester tester) async =>
      (await tester.runAsync(() async {
        final recorder = ui.PictureRecorder();
        ui.Canvas(recorder).drawRect(
          const ui.Rect.fromLTWH(0, 0, 64, 64),
          ui.Paint()..color = const ui.Color(0xFF808080),
        );
        final imagen = await recorder.endRecording().toImage(64, 64);
        final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
        final directorio = await Directory.systemTemp.createTemp(
          'vida-rechazo',
        );
        final archivo = File('${directorio.path}/foto.png');
        await archivo.writeAsBytes(png!.buffer.asUint8List());
        return (directorio, archivo.path);
      }))!;

  Future<List<Object?>> abrir(
    WidgetTester tester,
    CamaraDoble camara,
    List<PoseDeVida> poses, {
    Future<Object> Function(String ruta)? validarFoto,
    bool movimientoReducido = false,
  }) async {
    _reloj = DateTime.utc(2026, 10, 12);
    final devuelto = <Object?>[];
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(
              context,
            ).copyWith(disableAnimations: movimientoReducido),
            child: child!,
          ),
          home: Builder(
            builder: (context) => TextButton(
              onPressed: () async => devuelto.add(
                await Navigator.of(context).push<Object?>(
                  MaterialPageRoute(
                    builder: (_) => PantallaDePruebaDeVida(
                      poses: poses,
                      crearCamara: () => camara,
                      ahora: () => _reloj,
                      validarFoto: validarFoto,
                    ),
                  ),
                ),
              ),
              child: const Text('abrir'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('abrir'));
    await tester.pumpAndSettle();
    return devuelto;
  }

  /// El detector "ve" la pose los cuadros necesarios; la foto se procesa de verdad.
  Future<void> sostener(WidgetTester tester, CamaraDoble c, double giro) async {
    await _avanzar(tester, const Duration(milliseconds: 1700));
    for (var i = 0; i < 32; i++) {
      c.ver(rostro(giro));
      await _avanzar(tester, const Duration(milliseconds: 100));
    }
    await _avanzar(tester, const Duration(milliseconds: 1200));
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 250)),
    );
    // `runAsync` deja que termine la lectura del archivo y también la espera
    // real de 1100 ms que el controlador inicia después de medir la captura.
    await tester.pump();
    if (c.fotosTomadas > 0) {
      // La lectura real termina fuera de FakeAsync. Primero bombeamos para que
      // el controlador registre su espera fake de 1100 ms tras publicar la foto.
      await _avanzar(tester, const Duration(milliseconds: 1200));
    }
  }

  testWidgets(
    'las tres poses se capturan solas y el flujo devuelve las fotos',
    (tester) async {
      final (directorio, ruta) = await foto1080(tester);
      final respuestasTactiles = <Object?>[];
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.platform,
        (call) async {
          if (call.method == 'HapticFeedback.vibrate') {
            respuestasTactiles.add(call.arguments);
          }
          return null;
        },
      );
      addTearDown(
        () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
          SystemChannels.platform,
          null,
        ),
      );
      addTearDown(() async {
        await tester.pumpWidget(const SizedBox.shrink());
        PaintingBinding.instance.imageCache.clear();
        PaintingBinding.instance.imageCache.clearLiveImages();
        await tester.runAsync(() async {
          await Future<void>.delayed(const Duration(milliseconds: 100));
          if (await directorio.exists()) {
            await directorio.delete(recursive: true);
          }
        });
      });
      final camara = CamaraDoble(foto: ruta);
      final devuelto = await abrir(
        tester,
        camara,
        PoseDeVida.values,
        validarFoto: (ruta) async => Captura(
          ruta: ruta,
          bytes: 1,
          ancho: 1080,
          alto: 1920,
          sha256Corto: 'sintetico',
        ),
      );
      expect(camara.conDeteccionPedida, isTrue);
      expect(
        tester.widget<Scaffold>(find.byType(Scaffold)).backgroundColor,
        Paleta.ink,
      );
      expect(find.text('Tomar ahora'), findsOneWidget);
      expect(find.text('FOTO 1 DE 3'), findsOneWidget);
      expect(find.text('Mirá de frente'), findsOneWidget);
      expect(find.text('Poné tu cara dentro del óvalo.'), findsOneWidget);
      expect(find.text('Necesitamos tu cámara'), findsNothing);
      await sostener(tester, camara, 0);
      expect(camara.fotosTomadas, 1);
      expect(find.text('FOTO 2 DE 3'), findsOneWidget);
      await sostener(tester, camara, 35);
      expect(find.text('FOTO 3 DE 3'), findsOneWidget);
      await sostener(tester, camara, -35);

      expect(camara.fotosTomadas, 3);
      expect(find.text('¡Listo! Tus tres fotos están'), findsOneWidget);
      await tester.runAsync(() => Future<void>.delayed(Duration.zero));
      expect(respuestasTactiles, [
        'HapticFeedbackType.mediumImpact',
        'HapticFeedbackType.mediumImpact',
        'HapticFeedbackType.mediumImpact',
      ]);
      await tester.tap(find.text('Continuar'));
      await tester.pumpAndSettle();
      final capturas = devuelto.single! as Map<CaraDelCarril, Captura>;
      expect(capturas.keys, [
        CaraDelCarril.selfie,
        CaraDelCarril.perfilIzquierdo,
        CaraDelCarril.perfilDerecho,
      ]);
    },
  );

  testWidgets('la pose equivocada no dispara y la pista dice qué hacer', (
    tester,
  ) async {
    final (directorio, ruta) = await foto1080(tester);
    addTearDown(() async {
      if (await directorio.exists()) await directorio.delete(recursive: true);
    });
    final camara = CamaraDoble(foto: ruta);
    await abrir(tester, camara, [PoseDeVida.izquierda]);
    await sostener(tester, camara, -35);
    expect(camara.fotosTomadas, 0);
    expect(find.text('Girá la cabeza hacia tu izquierda.'), findsOneWidget);
  });

  testWidgets('sin detector el respaldo manual queda visible', (tester) async {
    final camara = CamaraDoble(foto: 'x', conDetector: false);
    await abrir(tester, camara, PoseDeVida.values);
    expect(find.text('Tomar ahora'), findsOneWidget);
  });

  testWidgets('con movimiento reducido una foto no dispara el destello', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: 'fixture', conDetector: false);
    await abrir(
      tester,
      camara,
      [PoseDeVida.frente],
      movimientoReducido: true,
      validarFoto: (_) async => const Captura(
        ruta: 'fixture',
        bytes: 1,
        ancho: 64,
        alto: 64,
        sha256Corto: 'fixture',
      ),
    );

    await tester.tap(find.text('Tomar ahora'));
    await tester.pump();
    final destello = tester.widget<AnimatedBuilder>(
      find.byKey(const ValueKey('destello-de-captura')),
    );
    expect((destello.animation as Animation<double>).value, 0);
    await tester.pump(const Duration(milliseconds: 1200));
  });

  testWidgets('sin detector el ciclo también captura automáticamente', (
    tester,
  ) async {
    final (directorio, ruta) = await foto1080(tester);
    addTearDown(() async {
      await tester.pumpWidget(const SizedBox.shrink());
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
        if (await directorio.exists()) {
          await directorio.delete(recursive: true);
        }
      });
    });
    final camara = CamaraDoble(foto: ruta, conDetector: false);
    await abrir(
      tester,
      camara,
      PoseDeVida.values,
      validarFoto: (ruta) async => Captura(
        ruta: ruta,
        bytes: 1,
        ancho: 1080,
        alto: 1920,
        sha256Corto: 'sintetico',
      ),
    );

    for (var i = 0; i < 50; i++) {
      await _avanzar(tester, const Duration(milliseconds: 100));
    }
    await tester.pump();
    await _avanzar(tester, const Duration(milliseconds: 1200));

    expect(camara.fotosTomadas, 1);
    expect(find.text('FOTO 2 DE 3'), findsOneWidget);
  });

  testWidgets('con detector el respaldo manual también queda visible', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: 'x');
    await abrir(tester, camara, PoseDeVida.values);
    expect(find.text('Tomar ahora'), findsOneWidget);
  });

  testWidgets('permiso denegado da explicación y permite volver a pedir', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: 'x', permiso: EstadoDePermiso.denegado);
    await abrir(tester, camara, [PoseDeVida.frente]);
    expect(find.text('Necesitamos tu cámara'), findsOneWidget);
    expect(find.text('Solo se usa para tu prueba de vida.'), findsOneWidget);
    expect(find.text('Permitir cámara'), findsOneWidget);
  });

  testWidgets('permiso bloqueado indica abrir los ajustes', (tester) async {
    final camara = CamaraDoble(
      foto: 'x',
      permiso: EstadoDePermiso.denegadoParaSiempre,
    );
    await abrir(tester, camara, [PoseDeVida.frente]);
    expect(
      find.text('Abrí los ajustes del teléfono para permitirla.'),
      findsOneWidget,
    );
    expect(find.text('Abrir ajustes'), findsOneWidget);
  });

  testWidgets('foto rechazada se borra y ofrece repetir la miniatura', (
    tester,
  ) async {
    final (directorio, ruta) = await fotoPequena(tester);
    addTearDown(() async {
      if (await directorio.exists()) await directorio.delete(recursive: true);
    });
    final camara = CamaraDoble(foto: ruta);
    await abrir(tester, camara, PoseDeVida.values);
    await tester.runAsync(() async {
      await tester.tap(find.text('Tomar ahora'));
      await Future<void>.delayed(const Duration(milliseconds: 500));
    });
    await tester.pump();

    expect(find.bySemanticsLabel('Frente: Repetir'), findsOneWidget);
    expect(await tester.runAsync(() => File(ruta).exists()), isFalse);
    await tester.tap(find.bySemanticsLabel('Frente: Repetir'));
    await tester.pump();
    expect(find.bySemanticsLabel('Frente: Pendiente'), findsOneWidget);
  });

  testWidgets('salir antes de terminar devuelve capturas parciales', (
    tester,
  ) async {
    final (directorio, ruta) = await foto1080(tester);
    addTearDown(() async {
      await tester.pumpWidget(const SizedBox.shrink());
      PaintingBinding.instance.imageCache.clear();
      PaintingBinding.instance.imageCache.clearLiveImages();
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
        if (await directorio.exists()) {
          await directorio.delete(recursive: true);
        }
      });
    });
    final camara = CamaraDoble(foto: ruta);
    final devuelto = await abrir(tester, camara, PoseDeVida.values);
    await tester.runAsync(() async {
      await tester.tap(find.text('Tomar ahora'));
      await Future<void>.delayed(const Duration(milliseconds: 500));
    });
    await tester.pump();
    expect(find.text('¡Foto tomada!'), findsOneWidget);
    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();

    final parcial = devuelto.single! as Map<CaraDelCarril, Captura>;
    expect(parcial.keys, [CaraDelCarril.selfie]);
    expect(await tester.runAsync(() => File(ruta).exists()), isTrue);
  });

  test('tocar una selfie pendiente pide esa pose y las que faltan después', () {
    expect(posesDesde(CaraDelCarril.selfie, {}), PoseDeVida.values);
    expect(posesDesde(CaraDelCarril.selfie, {CaraDelCarril.perfilIzquierdo}), [
      PoseDeVida.frente,
      PoseDeVida.derecha,
    ]);
    // Repetir una que ya estaba: solo esa.
    expect(
      posesDesde(CaraDelCarril.perfilIzquierdo, {
        CaraDelCarril.perfilIzquierdo,
      }),
      [PoseDeVida.izquierda],
    );
  });
}
