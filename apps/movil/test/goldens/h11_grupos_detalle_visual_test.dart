import 'dart:async';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/detalle_del_grupo.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_del_grupo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'cargar_fuentes_de_aplicacion.dart';

/// H8.S4.M4 / H10.S3.M1 (PENDIENTES): la matriz visual de grupos/detalle. La pantalla SÍ existe
/// (`/pasanaku/grupos/:grupoId`, `rutas.dart`, contrato `consultarDetalleDelGrupo`): lo que faltaba
/// eran las capturas. Tres anchos × dos temas × {con datos, cargando, error}. Datos sintéticos.
const _evidencia =
    '../../../../docs/trabajo/2026-11-25-gu-h/evidencia/grupos-detalle/';
const _grupo = 'c6900000-0000-4000-8000-0000000000f1';
const _hash =
    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc';

Map<String, dynamic> _integrante(
  int n,
  String? nombre,
  String estado, {
  bool organiza = false,
  bool yo = false,
}) => {
  'numero': n,
  'cupoId': 'c6900000-0000-4000-8000-0000000000a$n',
  'nombre': nombre,
  'estado': estado,
  'esOrganizador': organiza,
  'soyYo': yo,
};

final _detalle = {
  'grupoId': _grupo,
  'nombre': 'Pasanaku del barrio',
  'estado': 'ABIERTO_A_INSCRIPCION',
  'montoAporte': '500.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': 3,
  'cuposOcupados': 2,
  'soyOrganizador': true,
  'debeFirmar': false,
  'puedeSortear': false,
  'reglamento': {'texto': 'Reglamento de prueba.', 'hash': _hash},
  'integrantes': [
    _integrante(1, 'Rosa Quispe', 'FIRMADO', organiza: true, yo: true),
    _integrante(2, 'Juan Mamani', 'PENDIENTE_FIRMA'),
    _integrante(3, null, 'LIBRE'),
  ],
};

enum _Estado { datos, cargando, error }

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  for (final ancho in [360, 412, 600]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      for (final estado in _Estado.values) {
        final tema = brillo == Brightness.light ? 'claro' : 'oscuro';
        testWidgets('grupo · ${estado.name} · $tema · $ancho dp · texto 130 %', (
          tester,
        ) async {
          tester.view.physicalSize = Size(ancho.toDouble(), switch (ancho) {
            360 => 800,
            412 => 900,
            _ => 960,
          });
          tester.view.devicePixelRatio = 1;
          tester.platformDispatcher.textScaleFactorTestValue = 1.3;
          tester.platformDispatcher.accessibilityFeaturesTestValue =
              const FakeAccessibilityFeatures(disableAnimations: true);
          addTearDown(tester.view.reset);
          addTearDown(tester.platformDispatcher.clearAllTestValues);
          addTearDown(
            tester.platformDispatcher.clearAccessibilityFeaturesTestValue,
          );

          final (:dio, :adaptador) = dioSimulado();
          if (estado == _Estado.datos) {
            adaptador.onGet(
              '/grupos/$_grupo/detalle',
              (s) => s.reply(200, _detalle),
            );
          }
          await tester.pumpWidget(
            ProviderScope(
              retry: (_, _) => null,
              overrides: [
                almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
                dioProvider.overrideWithValue(dio),
                if (estado == _Estado.cargando)
                  detalleDelGrupoProvider(
                    _grupo,
                  ).overrideWith((ref) => Completer<DetalleDelGrupo>().future),
                if (estado == _Estado.error)
                  detalleDelGrupoProvider(
                    _grupo,
                  ).overrideWith((ref) => Future.error(StateError('fallo'))),
              ],
              child: MaterialApp(
                theme: temaDesde(Tokens.claro, Brightness.light),
                darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
                themeMode: brillo == Brightness.light
                    ? ThemeMode.light
                    : ThemeMode.dark,
                home: const PantallaDelGrupo(
                  grupoId: _grupo,
                  nombre: 'Pasanaku del barrio',
                ),
              ),
            ),
          );
          if (estado == _Estado.cargando) {
            await tester.pump();
          } else {
            await asentar(tester);
          }

          final contexto = tester.element(find.byType(PantallaDelGrupo));
          expect(Theme.of(contexto).brightness, brillo);
          switch (estado) {
            case _Estado.datos:
              expect(find.text('Rosa Quispe'), findsOneWidget);
              expect(find.text('Cupo libre'), findsOneWidget);
            case _Estado.cargando:
              expect(find.text('Rosa Quispe'), findsNothing);
            case _Estado.error:
              expect(find.text('Rosa Quispe'), findsNothing);
              expect(
                find.widgetWithText(FilledButton, 'Volver a intentar'),
                findsOneWidget,
              );
          }
          expect(tester.takeException(), isNull);

          await expectLater(
            find.byType(PantallaDelGrupo),
            matchesGoldenFile(
              '${_evidencia}grupo-detalle-${estado.name}-$tema-$ancho-130.png',
            ),
          );
        });
      }
    }
  }
}
