import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/vigilante_de_sesion.dart';
import 'package:aportaya_movil/dominio/puertos/biometria.dart';
import 'package:aportaya_movil/pantallas/identidad/capa_de_bloqueo.dart';
import 'package:aportaya_movil/proveedores/biometria.dart';
import 'package:aportaya_movil/proveedores/bloqueo.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'cargar_fuentes_de_aplicacion.dart';

void main() {
  setUpAll(cargarFuentesDeAplicacion);

  final casos = <({String nombre, Size tamano, ThemeMode tema})>[
    (nombre: 'claro-360', tamano: const Size(360, 780), tema: ThemeMode.light),
    (nombre: 'oscuro-360', tamano: const Size(360, 780), tema: ThemeMode.dark),
    (nombre: 'claro-412', tamano: const Size(412, 850), tema: ThemeMode.light),
    (nombre: 'oscuro-412', tamano: const Size(412, 850), tema: ThemeMode.dark),
    (
      nombre: 'claro-tablet',
      tamano: const Size(600, 960),
      tema: ThemeMode.light,
    ),
    (
      nombre: 'oscuro-desktop',
      tamano: const Size(1000, 900),
      tema: ThemeMode.dark,
    ),
  ];

  for (final caso in casos) {
    testWidgets('candado ${caso.nombre}', (tester) async {
      await tester.binding.setSurfaceSize(caso.tamano);
      addTearDown(() => tester.binding.setSurfaceSize(null));
      final control = ControlDeBloqueo(esWeb: false)
        ..bloquear(MotivoDeBloqueo.arranque);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            bloqueoProvider.overrideWith((_) => control),
            biometriaProvider.overrideWithValue(_BiometriaSinConfirmar()),
          ],
          child: MaterialApp(
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: caso.tema,
            home: MediaQuery(
              data: MediaQueryData(size: caso.tamano, disableAnimations: true),
              child: CapaDeBloqueo(
                hijo: const Scaffold(body: Text('Estado privado')),
                onIngresarDeNuevo: () {},
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Hola de nuevo'), findsOneWidget);
      await expectLater(
        find.byType(CapaDeBloqueo),
        matchesGoldenFile(
          '../../../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/'
          'evidencia/h1-candado-${caso.nombre}.png',
        ),
      );
    });
  }
}

class _BiometriaSinConfirmar implements Biometria {
  @override
  Future<bool> disponible() async => true;

  @override
  Future<bool> confirmar(String motivo) async => false;
}
