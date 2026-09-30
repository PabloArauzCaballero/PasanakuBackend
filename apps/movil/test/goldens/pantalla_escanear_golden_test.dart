// Goldens de la pantalla de escaneo de invitaciones: estado «cámara denegada» (el que
// lleva más piezas: alerta, botón de ajustes y entrada manual) en tres viewports y en
// claro y oscuro. Como el resto de goldens, se corren con `yarn test:goldens`, no en el
// CI, y se comparan con los ojos.

import 'package:aportaya_movil/dominio/puertos/ajustes_del_sistema.dart';
import 'package:aportaya_movil/dominio/puertos/visor_qr.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_escanear.dart';
import 'package:aportaya_movil/proveedores/ajustes_del_sistema.dart';
import 'package:aportaya_movil/proveedores/visor_qr.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class _Ajustes implements AjustesDelSistema {
  @override
  Future<bool> abrir() async => true;
}

void main() {
  for (final (brillo, tema) in [
    (Brightness.light, 'claro'),
    (Brightness.dark, 'oscuro'),
  ]) {
    for (final (ancho, alto, viewport) in [
      (360.0, 640.0, 'movil'),
      (768.0, 1024.0, 'tableta'),
      (1280.0, 800.0, 'escritorio'),
    ]) {
      testWidgets('escanear · cámara denegada · $viewport · $tema', (
        tester,
      ) async {
        tester.view.physicalSize = Size(ancho, alto);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              ajustesDelSistemaProvider.overrideWithValue(_Ajustes()),
              visorQrProvider.overrideWithValue(
                ({required alLeer, required problema}) =>
                    problema(ProblemaDeCamara.denegado),
              ),
            ],
            child: MaterialApp(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              home: const PantallaEscanear(),
            ),
          ),
        );
        await tester.pump();
        await expectLater(
          find.byType(PantallaEscanear),
          matchesGoldenFile('imagenes/escanear_denegado_${viewport}_$tema.png'),
        );
      });
    }
  }
}
