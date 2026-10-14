import 'dart:async';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/errores.dart';
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
    'evidencia/h10-visual/';
const _h8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-subida-';
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
  _SubidaFija() : super();

  @override
  EstadoDeLaSubida build() => EstadoDeLaSubida(
    porCara: {
      CaraDelCarril.anverso: SubidaDeUnaCara(
        estado: EstadoDeUnaSubida.fallida,
        error: ErrorDeRed().mensaje,
      ),
    },
  );

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
  }) async {}

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

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (final estado in ['en-curso', 'error-red']) {
        testWidgets('subida · $estado · $tema · $ancho dp · texto 130 %', (
          tester,
        ) async {
          final alto = switch (ancho) {
            360 => 740,
            412 => 850,
            _ => 960,
          };
          tester.view.physicalSize = Size(ancho.toDouble(), alto.toDouble());
          tester.view.devicePixelRatio = 1;
          tester.platformDispatcher.textScaleFactorTestValue = 1.3;
          tester.platformDispatcher.accessibilityFeaturesTestValue =
              const FakeAccessibilityFeatures(disableAnimations: true);
          addTearDown(tester.view.reset);
          addTearDown(tester.platformDispatcher.clearAllTestValues);
          addTearDown(
            tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
          );

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

          _SubidaPendiente? servicioFalso;
          final contenedor = ProviderContainer(
            overrides: [
              generacionDeSesionProvider.overrideWith((ref) => 0),
              seguimientoDelAltaProvider.overrideWith(
                () => _SeguimientoSintetico(capturas),
              ),
              if (estado == 'en-curso')
                subidaDeFotosProvider.overrideWith((ref) {
                  final falsa = _SubidaPendiente(ref);
                  servicioFalso = falsa;
                  return falsa;
                })
              else
                subidaProvider.overrideWith(() => _SubidaFija()),
            ],
          );
          addTearDown(contenedor.dispose);

          await tester.pumpWidget(
            UncontrolledProviderScope(
              container: contenedor,
              child: MaterialApp(
                theme: temaDesde(Tokens.claro, Brightness.light),
                darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
                themeMode: brillo == Brightness.light
                    ? ThemeMode.light
                    : ThemeMode.dark,
                home: const PantallaDeSubidaDelExpediente(
                  usuarioId: _usuarioSintetico,
                ),
              ),
            ),
          );
          await tester.pump();

          final pantalla = find.byType(PantallaDeSubidaDelExpediente);
          final contexto = tester.element(pantalla);
          expect(Theme.of(contexto).brightness, brillo);
          expect(
            MediaQuery.textScalerOf(contexto).scale(10),
            closeTo(13, 0.001),
          );
          expect(find.text(TextosDeSubida.tituloSubiendo), findsOneWidget);
          for (final cara in CapturasDelExpediente.orden) {
            expect(find.text(tituloDeCaptura(cara)), findsOneWidget);
          }

          if (estado == 'en-curso') {
            expect(servicioFalso, isNotNull);
            expect(servicioFalso!.inicio.isCompleted, isTrue);
            expect(
              find.text(TextosDeSubida.subiendo(CaraDelCarril.anverso)),
              findsOneWidget,
            );
            expect(find.text(TextosDeSubida.detalle), findsOneWidget);
            expect(find.text(TextosDeSubida.cancelarLaSubida), findsOneWidget);
            expect(find.text('—'), findsNWidgets(4));
            final continuar = tester.widget<FilledButton>(
              find.ancestor(
                of: find.text(TextosDeSubida.continuar),
                matching: find.byType(FilledButton),
              ),
            );
            expect(continuar.onPressed, isNull);
          } else {
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
          }
          expect(find.text('/synthetic/anverso.jpg'), findsNothing);
          expect(tester.takeException(), isNull);

          final ruta =
              estado == 'en-curso' && ancho == 360 && brillo == Brightness.dark
              ? '${_h8}oscura-360-130.png'
              : '$_evidencia'
                    'h10-subida-$estado-$tema-$ancho-130.png';
          await expectLater(pantalla, matchesGoldenFile(ruta));

          if (estado == 'en-curso') {
            await tester.pumpWidget(
              UncontrolledProviderScope(
                container: contenedor,
                child: const SizedBox.shrink(),
              ),
            );
            await tester.pump();
            expect(servicioFalso!.cancelada.isCompleted, isTrue);
            expect(tester.takeException(), isNull);
          }
        });
      }
    }
  }
}
