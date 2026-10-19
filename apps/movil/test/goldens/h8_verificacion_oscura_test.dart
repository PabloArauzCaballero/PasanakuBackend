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
    'evidencia/';
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

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  testWidgets('estado de verificación · oscuro · 360 px · texto 130 %', (
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

    const casos = [
      (
        clave: 'pendiente',
        estado: EstadoDeVerificacionEstadoEnum.PENDIENTE,
        // 2026-10-19: PENDIENTE se dice igual que EN_REVISION (sin «No cierres la app»).
        titulo: TextosDeVerificacion.enRevisionTitulo,
        accion: TextosDeVerificacion.seguirConElRegistro,
      ),
      (
        clave: 'en-revision',
        estado: EstadoDeVerificacionEstadoEnum.EN_REVISION,
        titulo: TextosDeVerificacion.enRevisionTitulo,
        accion: TextosDeVerificacion.seguirConElRegistro,
      ),
      (
        clave: 'aprobada',
        estado: EstadoDeVerificacionEstadoEnum.APROBADA,
        titulo: TextosDeVerificacion.aprobadaTitulo,
        accion: TextosDeVerificacion.entrar,
      ),
      (
        clave: 'rechazada',
        estado: EstadoDeVerificacionEstadoEnum.RECHAZADA,
        titulo: TextosDeVerificacion.rechazadaTitulo,
        accion: TextosDeVerificacion.intentarDeNuevo,
      ),
    ];

    for (final caso in casos) {
      await tester.pumpWidget(
        ProviderScope(
          key: ValueKey(caso.clave),
          overrides: [
            consultarVerificacionProvider.overrideWith(
              (ref) => _ConsultaFalsa(ref, caso.estado),
            ),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: ThemeMode.dark,
            home: const PantallaDeEstadoDeVerificacion(
              usuarioId: _usuarioSintetico,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final pantalla = find.byType(PantallaDeEstadoDeVerificacion);
      final contexto = tester.element(pantalla);
      expect(Theme.of(contexto).brightness, Brightness.dark);
      expect(MediaQuery.textScalerOf(contexto).scale(10), closeTo(13, 0.001));
      expect(find.text(caso.titulo), findsOneWidget);
      expect(
        find.text(TextosDeVerificacion.caso(_verificacionSintetica)),
        findsOneWidget,
      );
      expect(find.text(caso.accion), findsOneWidget);
      expect(find.text(TextosDeVerificacion.actualizarEstado), findsOneWidget);
      if (caso.estado == EstadoDeVerificacionEstadoEnum.RECHAZADA) {
        expect(
          find.text('La cara no coincide con el documento.'),
          findsOneWidget,
        );
      }
      expect(tester.takeException(), isNull);
      await expectLater(
        pantalla,
        matchesGoldenFile(
          '${_evidencia}h8-verificacion-${caso.clave}-oscura-360-130.png',
        ),
      );
    }
  });
}
