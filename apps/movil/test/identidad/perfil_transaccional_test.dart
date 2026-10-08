import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  setUpAll(() async {
    final bricolage = FontLoader('Bricolage')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-600.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-700.ttf'),
      );
    final instrument = FontLoader('Instrument')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-400.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-600.ttf'),
      );
    await Future.wait([bricolage.load(), instrument.load()]);
  });

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets('el perfil conserva centavos al 200 % en ${brillo.name}', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(360, 760);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.reset);

      final contenedor = ProviderContainer(
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        ],
      );
      addTearDown(contenedor.dispose);
      contenedor
          .read(altaProvider.notifier)
          .actualizarPerfilTransaccional(
            origen: 'SALARIO',
            detalleDelOrigen: '',
            actividad: 'T',
            monto: '125.05',
          );

      const claveCaptura = ValueKey('perfil-transaccional');
      await tester.pumpWidget(
        UncontrolledProviderScope(
          container: contenedor,
          child: RepaintBoundary(
            key: claveCaptura,
            child: MaterialApp(
              debugShowCheckedModeBanner: false,
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: const TextScaler.linear(2)),
                child: child!,
              ),
              home: const Scaffold(
                body: SafeArea(
                  child: SingleChildScrollView(
                    child: PasoPerfilTransaccional(),
                  ),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pump(const Duration(milliseconds: 500));
      await tester.pumpAndSettle();

      expect(find.byType(TextField), findsOneWidget);
      expect(
        tester.widget<TextField>(find.byType(TextField)).controller?.text,
        '125.05',
      );
      expect(find.text('De dónde viene la plata.'), findsOneWidget);
      expect(find.text('Opcional; no es un límite.'), findsOneWidget);
      expect(tester.takeException(), isNull);

      // Captura sintética opt-in para inspección local; no es golden de CI.
      Future<void> capturar(String nombre) async {
        if (Platform.environment['CAPTURAR_PERFIL'] != '1') return;
        final limite = tester.renderObject<RenderRepaintBoundary>(
          find.byKey(claveCaptura),
        );
        await tester.runAsync(() async {
          final imagen = await limite.toImage(pixelRatio: 1);
          final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
          imagen.dispose();
          final directorio =
              Platform.environment['CAPTURA_PERFIL_DIR'] ?? 'build/visual';
          final destino = File('$directorio/$nombre.png');
          await destino.parent.create(recursive: true);
          await destino.writeAsBytes(png!.buffer.asUint8List());
        });
      }

      await capturar('perfil_${brillo.name}');

      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      expect(
        tester
            .widgetList<Text>(find.text('Empleo en relación de dependencia'))
            .any((texto) => texto.overflow == null),
        isTrue,
      );
      expect(tester.takeException(), isNull);
      await capturar('perfil_menu_${brillo.name}');
    });
  }
}
