import 'dart:io';
import 'dart:ui' as ui;

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
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

  testWidgets('al volver se recuperan datos, no contraseña ni verificación', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 760);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    final almacen = AlmacenEnMemoria();

    ProviderContainer nuevoContenedor() => ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
    );

    const claveCaptura = ValueKey('alta-reanudada');
    Widget app(
      ProviderContainer contenedor, {
      Brightness brillo = Brightness.light,
    }) => UncontrolledProviderScope(
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
          home: const PantallaDeRegistro(),
        ),
      ),
    );

    Future<void> capturar(String nombre) async {
      if (Platform.environment['CAPTURAR_ALTA'] != '1') return;
      final limite = tester.renderObject<RenderRepaintBoundary>(
        find.byKey(claveCaptura),
      );
      await tester.runAsync(() async {
        final imagen = await limite.toImage(pixelRatio: 1);
        final png = await imagen.toByteData(format: ui.ImageByteFormat.png);
        imagen.dispose();
        final directorio =
            Platform.environment['CAPTURA_ALTA_DIR'] ?? 'build/visual';
        final destino = File('$directorio/$nombre.png');
        await destino.parent.create(recursive: true);
        await destino.writeAsBytes(png!.buffer.asUint8List());
      });
    }

    final primero = nuevoContenedor();
    await tester.pumpWidget(app(primero));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull, reason: 'primer paso');
    final alta = primero.read(altaProvider.notifier);
    alta.actualizarDatos(
      const DatosPersonales(nombres: 'Demo', apellidos: 'Prueba'),
    );
    alta.siguiente();
    alta.elegirContrasena('clave-solo-en-memoria');
    alta.siguiente();
    alta.siguiente();
    expect(primero.read(altaProvider).paso, PasoAlta.celular);
    alta.capturarAnverso('/tmp/foto-sintetica.jpg');

    await tester.pump(const Duration(milliseconds: 450));
    expect(tester.takeException(), isNull, reason: 'captura');
    final guardado = await almacen.leer('alta.borrador.v1');
    expect(guardado, contains('Demo'));
    expect(guardado, isNot(contains('clave-solo-en-memoria')));
    expect(guardado, isNot(contains('/tmp/foto-sintetica.jpg')));

    await tester.pumpWidget(const SizedBox.shrink());
    primero.dispose();

    final segundo = nuevoContenedor();
    addTearDown(segundo.dispose);
    await tester.pumpWidget(app(segundo));
    await tester.pumpAndSettle();
    await tester.pump(const Duration(milliseconds: 20));
    expect(tester.takeException(), isNull, reason: 'retorno a celular');

    final restaurado = segundo.read(altaProvider);
    expect(find.byType(PantallaDeRegistro), findsOneWidget);
    expect(restaurado.paso, PasoAlta.celular);
    expect(restaurado.datos.nombres, 'Demo');
    expect(restaurado.contrasena, isEmpty);
    expect(restaurado.codigoConfirmado, isFalse);
    expect(restaurado.rutaAnverso, isNull);
    expect(restaurado.resumido, isTrue);
    expect(find.text(TextosIdentidad.registroReanudado), findsOneWidget);
    await capturar('alta_reanudada_celular_claro');

    await tester.pumpWidget(app(segundo, brillo: Brightness.dark));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull, reason: 'celular oscuro');
    await capturar('alta_reanudada_celular_oscuro');

    segundo.read(altaProvider.notifier).atras();
    segundo.read(altaProvider.notifier).atras();
    await tester.pumpAndSettle();
    await tester.pump(const Duration(milliseconds: 450));
    expect(tester.takeException(), isNull, reason: 'retorno a datos');
    expect(find.byType(TextField), findsWidgets);
    expect(
      tester.widget<TextField>(find.byType(TextField).first).controller?.text,
      'Demo',
    );
    await capturar('alta_reanudada_datos_oscuro');

    await tester.pumpWidget(app(segundo));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull, reason: 'datos claro');
    await capturar('alta_reanudada_datos_claro');
    await tester.ensureVisible(find.text(TextosIdentidad.continuar));
    await tester.pumpAndSettle();
    expect(
      tester.getBottomRight(find.text(TextosIdentidad.continuar)).dy,
      lessThanOrEqualTo(760),
    );
    await capturar('alta_reanudada_datos_accion_claro');
    tester.view.viewInsets = const FakeViewPadding(bottom: 300);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text(TextosIdentidad.continuar));
    await tester.pumpAndSettle();
    expect(
      tester.getBottomRight(find.text(TextosIdentidad.continuar)).dy,
      lessThanOrEqualTo(460),
    );
    expect(tester.takeException(), isNull, reason: 'teclado simulado');
    await capturar('alta_reanudada_datos_teclado_claro');
    tester.view.resetViewInsets();
    await tester.pumpAndSettle();

    await tester.pumpWidget(app(segundo, brillo: Brightness.dark));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text(TextosIdentidad.continuar));
    await tester.pumpAndSettle();
    await capturar('alta_reanudada_datos_accion_oscuro');
    expect(tester.takeException(), isNull);
  });
}
