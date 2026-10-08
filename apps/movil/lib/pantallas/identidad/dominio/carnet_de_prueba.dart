import 'dart:io';
import 'dart:math';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:path_provider/path_provider.dart';

import 'capturas_del_expediente.dart';

/// El atajo «Usar el carnet de prueba» de Atlas, solo en desarrollo
/// (`!kReleaseMode`, mismo gate que `CAMARA_DEV`): pinta cinco imágenes sintéticas,
/// rotuladas como tales, para completar el alta en un emulador que no tiene cámara.
///
/// Nada de archivos binarios en el repo: se dibujan en el momento con
/// `dart:ui.PictureRecorder`, del mismo tamaño y proporción que exige
/// `calidad_de_captura.dart` (1600×1009 para el carnet, 1200×1600 para las
/// selfies), así que pasan el chequeo igual que una foto real.
class DatosDelCarnetDePrueba {
  const DatosDelCarnetDePrueba({
    this.numeroDocumento = '1234567',
    this.lugarExpedicion = 'SC',
  });
  final String numeroDocumento;
  final String lugarExpedicion;
}

const datosDelCarnetDePrueba = DatosDelCarnetDePrueba();

/// Lo que «lee» el carnet de prueba, como haría el OCR de Atlas: el número y el lugar
/// que la persona ya escribió en el paso 1, así el cotejo coincide. Si todavía no
/// escribió nada, un número sintético al azar —nunca uno fijo: con `1234567` fijo la
/// segunda alta de prueba contra la misma base chocaba con `AP-CU01-03`.
DatosDelCarnetDePrueba leerCarnetDePrueba({
  String numeroEscrito = '',
  String lugarEscrito = '',
  Random? azar,
}) {
  final numero = numeroEscrito.trim().isNotEmpty
      ? numeroEscrito.trim()
      : '${(azar ?? Random()).nextInt(9000000) + 1000000}';
  final lugar = lugarEscrito.trim().isNotEmpty
      ? lugarEscrito.trim()
      : datosDelCarnetDePrueba.lugarExpedicion;
  return DatosDelCarnetDePrueba(
    numeroDocumento: numero,
    lugarExpedicion: lugar,
  );
}

/// Genera las cinco capturas sintéticas y las guarda en el directorio temporal.
/// Nunca se llama en un build de release: el botón que la dispara ya está detrás
/// de `!kReleaseMode` en la pantalla.
Future<CapturasDelExpediente> generarCapturasDePrueba() async {
  if (kReleaseMode) {
    throw StateError(
      'El carnet de prueba no existe en un build de producción.',
    );
  }
  final carpeta = await getTemporaryDirectory();
  var capturas = const CapturasDelExpediente();
  for (final cara in CapturasDelExpediente.orden) {
    final esDocumento = cara.esDocumento;
    final ancho = esDocumento ? 1600 : 1200;
    final alto = esDocumento ? 1009 : 1600;
    final bytes = await _pintar(tituloDeCaptura(cara), ancho, alto);
    final ruta = '${carpeta.path}/prueba-${cara.name}.png';
    await File(ruta).writeAsBytes(bytes);
    capturas = capturas.conCaptura(
      cara,
      Captura(
        ruta: ruta,
        bytes: bytes.length,
        ancho: ancho,
        alto: alto,
        sha256Corto: 'sintetico',
      ),
    );
  }
  return capturas;
}

Future<Uint8List> _pintar(String etiqueta, int ancho, int alto) async {
  final grabadora = ui.PictureRecorder();
  final lienzo = ui.Canvas(grabadora);
  final fondo = ui.Paint()..color = Paleta.line;
  lienzo.drawRect(
    ui.Rect.fromLTWH(0, 0, ancho.toDouble(), alto.toDouble()),
    fondo,
  );
  final borde = ui.Paint()
    ..color = Paleta.g600
    ..style = ui.PaintingStyle.stroke
    ..strokeWidth = 12;
  lienzo.drawRect(ui.Rect.fromLTWH(6, 6, ancho - 12, alto - 12), borde);
  final parrafo =
      ui.ParagraphBuilder(
          ui.ParagraphStyle(
            textAlign: ui.TextAlign.center,
            fontSize: ancho / 22,
          ),
        )
        ..pushStyle(ui.TextStyle(color: Paleta.ink))
        ..addText('CARNET DE PRUEBA\n$etiqueta\ndatos sintéticos');
  final texto = parrafo.build()
    ..layout(ui.ParagraphConstraints(width: ancho.toDouble() - 80));
  lienzo.drawParagraph(texto, const ui.Offset(40, 40));
  final imagen = await grabadora.endRecording().toImage(ancho, alto);
  final datos = await imagen.toByteData(format: ui.ImageByteFormat.png);
  imagen.dispose();
  return datos!.buffer.asUint8List();
}
