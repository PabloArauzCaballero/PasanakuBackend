import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_movil/pantallas/identidad/dominio/calidad_de_captura.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/captura_desde_archivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:flutter_test/flutter_test.dart';

/// Las fotos REALES de la cámara: lo que la E2E anterior nunca ejercitó, porque
/// usaba el carnet de prueba sintético (1600×1009).
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<String> imagen(int ancho, int alto) async {
    final grabadora = ui.PictureRecorder();
    ui.Canvas(grabadora).drawRect(
      ui.Rect.fromLTWH(0, 0, ancho.toDouble(), alto.toDouble()),
      ui.Paint()..color = const ui.Color(0xFF808080),
    );
    final img = await grabadora.endRecording().toImage(ancho, alto);
    final png = await img.toByteData(format: ui.ImageByteFormat.png);
    final dir = await Directory.systemTemp.createTemp('captura');
    addTearDown(() => dir.delete(recursive: true));
    final archivo = File('${dir.path}/foto.png');
    await archivo.writeAsBytes(png!.buffer.asUint8List());
    return archivo.path;
  }

  group('por qué la cámara va en 1080p', () {
    test('una foto de 720p (1280×720) se rechaza por chica', () {
      expect(
        evaluarCalidad(ancho: 1280, alto: 720, esDocumento: false),
        contains('muy pequeña'),
      );
    });

    test('una de 1080p (1920×1080 o 1080×1920) pasa', () {
      expect(
        evaluarCalidad(ancho: 1920, alto: 1080, esDocumento: false),
        isNull,
      );
      expect(
        evaluarCalidad(ancho: 1080, alto: 1920, esDocumento: false),
        isNull,
      );
    });
  });

  group('por qué la proporción del carnet no se le pide a la cámara', () {
    test('un cuadro entero 16:9 no tiene la proporción ID-1', () {
      expect(
        evaluarCalidad(ancho: 1920, alto: 1080, esDocumento: true),
        contains('el carnet entero'),
      );
    });

    test('un cuadro entero 4:3 tampoco', () {
      expect(
        evaluarCalidad(ancho: 4032, alto: 3024, esDocumento: true),
        contains('el carnet entero'),
      );
    });
  });

  test('una foto de la cámara de 1920×1080 se acepta como captura', () async {
    final ruta = await imagen(1920, 1080);
    final r = await capturaDesdeArchivo(ruta, recortadaAlCarnet: false);
    expect(r, isA<Captura>());
    expect((r as Captura).ancho, 1920);
    expect(r.sha256Corto, hasLength(12));
  });

  test(
    'lo que devuelve el escáner sí tiene que tener forma de carnet',
    () async {
      final carnet = await capturaDesdeArchivo(
        await imagen(1600, 1009),
        recortadaAlCarnet: true,
      );
      expect(carnet, isA<Captura>());
      final cuadrado = await capturaDesdeArchivo(
        await imagen(1600, 1600),
        recortadaAlCarnet: true,
      );
      expect(cuadrado, contains('el carnet entero'));
    },
  );
}
