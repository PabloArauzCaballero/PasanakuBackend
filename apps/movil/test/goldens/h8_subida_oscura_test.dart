import 'dart:async';
import 'dart:io';

import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_subir_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/seguimiento_del_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/subida_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_subida_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_subida.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/';
const _usuarioSintetico = 'uuuuuuuu-0000-4000-8000-000000000099';

class _SubidaPendiente extends SubidaDeFotos {
  _SubidaPendiente(super.ref);

  final inicio = Completer<void>();
  final cancelada = Completer<void>();

  @override
  Future<FotoDelExpediente> subir({
    required String usuarioId,
    required CaraDelCarril cara,
    required String rutaLocal,
    required String formularioId,
    CancelToken? cancelToken,
    String? tokenDeAlta,
  }) async {
    if (!inicio.isCompleted) inicio.complete();
    await Future.any<void>([
      Completer<void>().future,
      cancelToken!.whenCancel.then<void>((error) {
        if (!cancelada.isCompleted) cancelada.complete();
        throw error;
      }),
    ]);
    return const FotoDelExpediente(
      claveObjeto: 'synthetic',
      hashArchivo: 'test',
    );
  }
}

class _SubidaFija extends SubidaNotifier {
  _SubidaFija(this.estadoFijo);

  final EstadoDeLaSubida estadoFijo;
  var reintentos = 0;

  @override
  EstadoDeLaSubida build() => estadoFijo;

  @override
  Future<void> subirPendientes({
    required String usuarioId,
    required CapturasDelExpediente capturas,
    String? tokenDeAlta,
  }) async {}

  @override
  Future<void> reintentar({
    required String usuarioId,
    required CaraDelCarril cara,
    required String ruta,
    String? tokenDeAlta,
  }) async {
    reintentos++;
  }

  @override
  void cancelarPendientes() {}
}

class _SeguimientoSintetico extends SeguimientoDelAlta {
  _SeguimientoSintetico(this.capturas);

  final CapturasDelExpediente capturas;

  @override
  AltaEnSeguimiento build() =>
      AltaEnSeguimiento(usuarioId: _usuarioSintetico, capturas: capturas);

  @override
  Future<void> limpiar() async => state = null;
}

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('subida del expediente · oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    final raizSintetica =
        '${Directory.systemTemp.path}${Platform.pathSeparator}'
        'pasanaku-golden-sintetico-${DateTime.now().microsecondsSinceEpoch}';
    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      capturas = capturas.conCaptura(
        cara,
        Captura(
          ruta: '$raizSintetica${Platform.pathSeparator}${cara.name}.jpg',
          bytes: 120000,
          ancho: 1600,
          alto: 1000,
          sha256Corto: 'a11ce001',
        ),
      );
    }

    _SubidaPendiente? subidaFalsa;
    final contenedor = ProviderContainer(
      overrides: [
        generacionDeSesionProvider.overrideWith((ref) => 0),
        seguimientoDelAltaProvider.overrideWith(
          () => _SeguimientoSintetico(capturas),
        ),
        subidaDeFotosProvider.overrideWith((ref) {
          final falsa = _SubidaPendiente(ref);
          subidaFalsa = falsa;
          return falsa;
        }),
      ],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaDeSubidaDelExpediente(
            usuarioId: _usuarioSintetico,
          ),
        ),
      ),
    );
    await tester.pump();
    expect(tester.takeException(), isNull);
    expect(subidaFalsa, isNotNull);
    expect(subidaFalsa!.inicio.isCompleted, isTrue);

    final pantalla = find.byType(PantallaDeSubidaDelExpediente);
    final contexto = tester.element(pantalla);
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text(TextosDeSubida.tituloSubiendo), findsOneWidget);
    for (final cara in CapturasDelExpediente.orden) {
      expect(find.text(tituloDeCaptura(cara)), findsOneWidget);
    }
    expect(
      find.text(TextosDeSubida.subiendo(CaraDelCarril.anverso)),
      findsOneWidget,
    );
    expect(find.text(TextosDeSubida.detalle), findsOneWidget);
    expect(find.text(TextosDeSubida.cancelarLaSubida), findsOneWidget);
    expect(find.text('—'), findsNWidgets(4));
    expect(find.text(TextosDeSubida.continuar), findsOneWidget);
    final continuar = tester.widget<FilledButton>(
      find.ancestor(
        of: find.text(TextosDeSubida.continuar),
        matching: find.byType(FilledButton),
      ),
    );
    expect(continuar.onPressed, isNull);
    expect(find.text(raizSintetica), findsNothing);
    expect(tester.takeException(), isNull);
    await expectLater(
      pantalla,
      matchesGoldenFile('${_evidencia}h8-subida-oscura-360-130.png'),
    );

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: const SizedBox.shrink(),
      ),
    );
    await tester.pump();
    expect(subidaFalsa!.cancelada.isCompleted, isTrue);
    expect(
      contenedor.read(subidaProvider).porCara[CaraDelCarril.anverso]?.estado,
      EstadoDeUnaSubida.pendiente,
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('fallo de red y reintento · oscuro · 360 px · texto 130 %', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 740);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 1.3;
    tester.platformDispatcher.accessibilityFeaturesTestValue =
        const FakeAccessibilityFeatures(disableAnimations: true);
    addTearDown(tester.view.reset);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    addTearDown(tester.platformDispatcher.clearAccessibilityFeaturesTestValue);

    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      capturas = capturas.conCaptura(
        cara,
        Captura(
          ruta: '/synthetic/${cara.name}.jpg',
          bytes: 120000,
          ancho: 1600,
          alto: 1000,
          sha256Corto: 'a11ce001',
        ),
      );
    }
    final subidaFalsa = _SubidaFija(
      EstadoDeLaSubida(
        porCara: {
          CaraDelCarril.anverso: SubidaDeUnaCara(
            estado: EstadoDeUnaSubida.fallida,
            error: ErrorDeRed().mensaje,
          ),
        },
      ),
    );
    final contenedor = ProviderContainer(
      overrides: [
        generacionDeSesionProvider.overrideWith((ref) => 0),
        seguimientoDelAltaProvider.overrideWith(
          () => _SeguimientoSintetico(capturas),
        ),
        subidaProvider.overrideWith(() => subidaFalsa),
      ],
    );
    addTearDown(contenedor.dispose);
    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
          themeMode: ThemeMode.dark,
          home: const PantallaDeSubidaDelExpediente(
            usuarioId: _usuarioSintetico,
          ),
        ),
      ),
    );
    await tester.pump();

    final pantalla = find.byType(PantallaDeSubidaDelExpediente);
    final contexto = tester.element(pantalla);
    expect(Theme.of(contexto).brightness, Brightness.dark);
    expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
    expect(find.text(TextosDeSubida.tituloSubiendo), findsOneWidget);
    expect(find.text(ErrorDeRed().mensaje), findsOneWidget);
    expect(find.text(TextosDeSubida.reintentar), findsOneWidget);
    expect(
      find.text(TextosDeSubida.repetirFotoDe(CaraDelCarril.anverso)),
      findsOneWidget,
    );
    expect(find.text('—'), findsNWidgets(4));
    final continuar = tester.widget<FilledButton>(
      find.ancestor(
        of: find.text(TextosDeSubida.continuar),
        matching: find.byType(FilledButton),
      ),
    );
    expect(continuar.onPressed, isNotNull);
    expect(find.text('/synthetic/anverso.jpg'), findsNothing);
    expect(tester.takeException(), isNull);
    await expectLater(
      pantalla,
      matchesGoldenFile('${_evidencia}h8-subida-fallida-oscura-360-130.png'),
    );
    await tester.tap(find.text(TextosDeSubida.reintentar));
    await tester.pumpAndSettle();
    expect(subidaFalsa.reintentos, 1);

    tester.view.physicalSize = const Size(320, 640);
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    await tester.pumpAndSettle();
    final contextoCompacto = tester.element(pantalla);
    expect(Theme.of(contextoCompacto).brightness, Brightness.dark);
    expect(
      MediaQuery.textScalerOf(contextoCompacto).scale(10),
      closeTo(20, 0.001),
    );
    expect(find.text('Enviando tus\ndocumentos'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await expectLater(
      pantalla,
      matchesGoldenFile('${_evidencia}h8-subida-error-oscura-320-200.png'),
    );

    final repetirFoto = find.text(
      TextosDeSubida.repetirFotoDe(CaraDelCarril.anverso),
    );
    await tester.ensureVisible(repetirFoto);
    await tester.pumpAndSettle();
    expect(
      tester
          .getRect(repetirFoto)
          .overlaps(tester.getRect(find.byType(ListView))),
      isTrue,
    );
    expect(tester.takeException(), isNull);

    await tester.pumpWidget(
      UncontrolledProviderScope(
        container: contenedor,
        child: const SizedBox.shrink(),
      ),
    );
    await tester.pump();
  });
}
