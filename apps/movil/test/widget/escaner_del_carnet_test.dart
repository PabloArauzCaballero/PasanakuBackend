import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_movil/infraestructura/escaner_de_documentos.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/sacar_captura.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// El anverso y el reverso salen del escáner del sistema; lo que devuelve pasa el
/// chequeo de Atlas (lado largo y proporción ID-1) antes de quedar en el riel.
void main() {
  Future<String> png(WidgetTester tester, int ancho, int alto) async =>
      (await tester.runAsync(() async {
        final g = ui.PictureRecorder();
        ui.Canvas(g).drawRect(
          ui.Rect.fromLTWH(0, 0, ancho.toDouble(), alto.toDouble()),
          ui.Paint()..color = const ui.Color(0xFF406080),
        );
        final img = await g.endRecording().toImage(ancho, alto);
        final datos = await img.toByteData(format: ui.ImageByteFormat.png);
        final dir = await Directory.systemTemp.createTemp('escaneo');
        final f = File('${dir.path}/carnet.png');
        await f.writeAsBytes(datos!.buffer.asUint8List());
        return f.path;
      }))!;

  Future<Object?> escanearCon(
    WidgetTester tester,
    ResultadoDelEscaner escaneo,
  ) async {
    Object? resultado;
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async => resultado = await sacarCaptura(
                context,
                CaraDelCarril.anverso,
                escanear: () async => escaneo,
              ),
              child: const Text('escanear'),
            ),
          ),
        ),
      ),
    );
    await tester.runAsync(() async {
      await tester.tap(find.text('escanear'));
      await Future<void>.delayed(const Duration(milliseconds: 300));
    });
    await tester.pumpAndSettle();
    return resultado;
  }

  testWidgets('un carnet escaneado queda como captura del anverso', (
    tester,
  ) async {
    final ruta = await png(tester, 1600, 1009);
    final r = await escanearCon(tester, EscaneoCapturado(ruta));
    final capturas = r! as Map<CaraDelCarril, Captura>;
    expect(capturas.keys, [CaraDelCarril.anverso]);
    expect(capturas[CaraDelCarril.anverso]!.ancho, 1600);
  });

  testWidgets('un escaneo que no tiene forma de carnet se rechaza y se avisa', (
    tester,
  ) async {
    final ruta = await png(tester, 1600, 1600);
    final r = await escanearCon(tester, EscaneoCapturado(ruta));
    expect(r, isNull);
    expect(
      find.text(
        'No parece el carnet entero. Repetila con los cuatro bordes a la vista.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('el rechazo ofrece usar la foto igual y cierra la hoja', (
    tester,
  ) async {
    final ruta = await png(tester, 1600, 1600);
    Object? resultado;
    await tester.pumpWidget(
      MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: Scaffold(
          body: Builder(
            builder: (context) => TextButton(
              onPressed: () async => resultado = await sacarCaptura(
                context,
                CaraDelCarril.anverso,
                escanear: () async => EscaneoCapturado(ruta),
              ),
              child: const Text('escanear'),
            ),
          ),
        ),
      ),
    );

    await tester.runAsync(() async {
      await tester.tap(find.text('escanear'));
      await Future<void>.delayed(const Duration(milliseconds: 300));
    });
    await tester.pumpAndSettle();
    expect(find.text('Repetí la foto'), findsOneWidget);
    expect(find.text('Repetir foto del anverso'), findsOneWidget);
    expect(find.text('Usar esta foto igual'), findsOneWidget);

    await tester.tap(find.text('Usar esta foto igual'));
    await tester.pumpAndSettle();
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 50)),
    );
    await tester.pump();
    expect(find.text('Usar esta foto igual'), findsNothing);
    final capturas = resultado! as Map<CaraDelCarril, Captura>;
    expect(capturas.keys, [CaraDelCarril.anverso]);
    expect(capturas[CaraDelCarril.anverso]!.ruta, ruta);
  });

  testWidgets(
    'repetir foto del carnet vuelve a escanear y continúa con la nueva',
    (tester) async {
      final rechazada = await png(tester, 1600, 1600);
      final valida = await png(tester, 1600, 1009);
      var intentos = 0;
      Object? resultado;
      await tester.pumpWidget(
        MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: Scaffold(
            body: Builder(
              builder: (context) => TextButton(
                onPressed: () async => resultado = await sacarCaptura(
                  context,
                  CaraDelCarril.reverso,
                  escanear: () async =>
                      EscaneoCapturado(++intentos == 1 ? rechazada : valida),
                ),
                child: const Text('escanear'),
              ),
            ),
          ),
        ),
      );

      await tester.runAsync(() async {
        await tester.tap(find.text('escanear'));
        await Future<void>.delayed(const Duration(milliseconds: 300));
      });
      await tester.pumpAndSettle();
      await tester.tap(find.text('Repetir foto del reverso'));
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 300)),
      );
      await tester.pumpAndSettle();

      expect(intentos, 2);
      final capturas = resultado! as Map<CaraDelCarril, Captura>;
      expect(capturas[CaraDelCarril.reverso]!.ruta, valida);
    },
  );

  testWidgets('cerrar el escáner sin escanear no hace nada', (tester) async {
    final r = await escanearCon(tester, const EscaneoCancelado());
    expect(r, isNull);
    expect(find.byType(SnackBar), findsNothing);
  });
}
