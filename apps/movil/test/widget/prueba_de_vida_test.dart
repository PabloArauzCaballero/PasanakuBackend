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
import 'package:flutter_test/flutter_test.dart';

/// Doble de la cámara en vivo: el test le dicta qué cara "ve" el detector.
class CamaraDoble implements CamaraEnVivo {
  CamaraDoble({required this.foto, this.conDetector = true});
  final String foto;
  final bool conDetector;
  void Function(LecturaDeRostro?)? _alLeer;
  var fotosTomadas = 0;
  var conDeteccionPedida = false;

  void ver(LecturaDeRostro? l) => _alLeer?.call(l);

  @override
  Future<EstadoDePermiso> pedirPermiso() async => EstadoDePermiso.concedido;
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

  Future<String> foto1080(WidgetTester tester) async =>
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
        return f.path;
      }))!;

  /// Abre la pantalla desde un botón para poder leer lo que devuelve al cerrarse.
  Future<List<Object?>> abrir(
    WidgetTester tester,
    CamaraDoble camara,
    List<PoseDeVida> poses,
  ) async {
    final devuelto = <Object?>[];
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Builder(
            builder: (context) => TextButton(
              onPressed: () async => devuelto.add(
                await Navigator.of(context).push<Object?>(
                  MaterialPageRoute(
                    builder: (_) => PantallaDePruebaDeVida(
                      poses: poses,
                      crearCamara: () => camara,
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
    await tester.runAsync(() async {
      for (var i = 0; i < JuezDePose.cuadrosSostenidos; i++) {
        c.ver(rostro(giro));
      }
      await Future<void>.delayed(const Duration(milliseconds: 300));
    });
    await tester.pumpAndSettle();
  }

  testWidgets('las tres poses se capturan solas, sin tocar «Tomar foto»', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: await foto1080(tester));
    final devuelto = await abrir(tester, camara, PoseDeVida.values);
    expect(camara.conDeteccionPedida, isTrue);
    expect(find.text('Tomar foto'), findsNothing);
    expect(find.textContaining('1 de 3'), findsOneWidget);

    await sostener(tester, camara, 0);
    expect(find.textContaining('2 de 3'), findsOneWidget);
    await sostener(tester, camara, 35);
    expect(find.textContaining('3 de 3'), findsOneWidget);
    await sostener(tester, camara, -35);

    expect(camara.fotosTomadas, 3);
    final capturas = devuelto.single! as Map<CaraDelCarril, Captura>;
    expect(capturas.keys, [
      CaraDelCarril.selfie,
      CaraDelCarril.perfilIzquierdo,
      CaraDelCarril.perfilDerecho,
    ]);
  });

  testWidgets('la pose equivocada no dispara y la pista dice qué hacer', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: await foto1080(tester));
    await abrir(tester, camara, [PoseDeVida.izquierda]);
    await sostener(tester, camara, -35);
    expect(camara.fotosTomadas, 0);
    expect(find.text('Girá la cabeza hacia tu izquierda.'), findsOneWidget);
  });

  testWidgets('sin detector en el teléfono, «Tomar foto» aparece enseguida', (
    tester,
  ) async {
    final camara = CamaraDoble(foto: 'x', conDetector: false);
    await abrir(tester, camara, PoseDeVida.values);
    expect(find.text('Tomar foto'), findsOneWidget);
  });

  testWidgets('con detector pero sin lograr la pose en 20 s, aparece el '
      'respaldo manual', (tester) async {
    final camara = CamaraDoble(foto: 'x');
    await abrir(tester, camara, PoseDeVida.values);
    await tester.pump(const Duration(seconds: 19));
    expect(find.text('Tomar foto'), findsNothing);
    await tester.pump(const Duration(seconds: 2));
    expect(find.text('Tomar foto'), findsOneWidget);
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
