import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_estado_de_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/textos_de_verificacion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

const _evidencia =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h10-visual/';
const _h8 =
    '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
    'evidencia/h8-verificacion-';
const _usuarioSintetico = 'uuuuuuuu-0000-4000-8000-000000000099';
const _verificacionSintetica = 'a11ce001-0000-4000-8000-000000000099';

class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref, this.estado);

  final EstadoDeVerificacionEstadoEnum estado;

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async => EstadoDeVerificacion(
    verificacionId: _verificacionSintetica,
    estado: estado,
    fotos: const [],
    motivoRechazo: estado == EstadoDeVerificacionEstadoEnum.RECHAZADA
        ? 'IDENTIDAD_NO_COINCIDE'
        : null,
  );
}

const _casos = [
  (
    clave: 'pendiente',
    estado: EstadoDeVerificacionEstadoEnum.PENDIENTE,
    titulo: TextosDeVerificacion.pendienteTitulo,
    accion: TextosDeVerificacion.seguirConElRegistro,
    sesionActiva: false,
  ),
  (
    clave: 'en-revision',
    estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
    titulo: TextosDeVerificacion.enRevisionTitulo,
    accion: TextosDeVerificacion.seguirConElRegistro,
    sesionActiva: false,
  ),
  (
    clave: 'sesion-en-revision',
    estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
    titulo: TextosDeVerificacion.cuentaEnVerificacionTitulo,
    // Con sesión y la identidad en revisión se entra a la app (2026-10-17): ya no
    // queda «Salir de esta cuenta» como única salida.
    accion: TextosDeVerificacion.irALaApp,
    sesionActiva: true,
  ),
  (
    clave: 'aprobada',
    estado: EstadoDeVerificacionEstadoEnum.APROBADA,
    titulo: TextosDeVerificacion.aprobadaTitulo,
    accion: TextosDeVerificacion.entrar,
    sesionActiva: false,
  ),
  (
    clave: 'rechazada',
    estado: EstadoDeVerificacionEstadoEnum.RECHAZADA,
    titulo: TextosDeVerificacion.rechazadaTitulo,
    accion: TextosDeVerificacion.intentarDeNuevo,
    sesionActiva: false,
  ),
];

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
      for (final caso in _casos) {
        testWidgets(
          'verificación · ${caso.clave} · $tema · $ancho dp · texto 130 %',
          (tester) async {
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

            await tester.pumpWidget(
              ProviderScope(
                overrides: [
                  consultarVerificacionProvider.overrideWith(
                    (ref) => _ConsultaFalsa(ref, caso.estado),
                  ),
                ],
                child: MaterialApp(
                  theme: temaDesde(Tokens.claro, Brightness.light),
                  darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
                  themeMode: brillo == Brightness.light
                      ? ThemeMode.light
                      : ThemeMode.dark,
                  home: PantallaDeEstadoDeVerificacion(
                    usuarioId: _usuarioSintetico,
                    sesionActiva: caso.sesionActiva,
                  ),
                ),
              ),
            );
            await tester.pumpAndSettle();

            final pantalla = find.byType(PantallaDeEstadoDeVerificacion);
            final contexto = tester.element(pantalla);
            expect(Theme.of(contexto).brightness, brillo);
            expect(
              MediaQuery.textScalerOf(contexto).scale(10),
              closeTo(13, 0.001),
            );
            expect(find.text(caso.titulo), findsOneWidget);
            expect(
              find.text(TextosDeVerificacion.caso(_verificacionSintetica)),
              findsOneWidget,
            );
            expect(find.text(caso.accion), findsOneWidget);
            expect(
              find.text(TextosDeVerificacion.actualizarEstado),
              findsOneWidget,
            );
            if (caso.estado == EstadoDeVerificacionEstadoEnum.RECHAZADA) {
              expect(
                find.text('La cara no coincide con el documento.'),
                findsOneWidget,
              );
            }
            expect(tester.takeException(), isNull);

            final ruta =
                ancho == 360 && brillo == Brightness.dark && !caso.sesionActiva
                ? '$_h8${caso.clave}-oscura-360-130.png'
                : '$_evidencia'
                      'h10-verificacion-${caso.clave}-$tema-$ancho-130.png';
            await expectLater(pantalla, matchesGoldenFile(ruta));
          },
        );
      }
    }
  }
}
