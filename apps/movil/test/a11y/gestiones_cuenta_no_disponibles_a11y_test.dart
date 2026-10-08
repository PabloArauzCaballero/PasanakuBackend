import 'package:aportaya_movil/pantallas/identidad/pantalla_baja.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_contrasena.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_dispositivos.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_perfil.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  for (final (nombre, pantalla) in [
    ('dispositivos', const PantallaDeDispositivos()),
    ('contraseña', const PantallaDeContrasena()),
    ('baja', const PantallaDeBaja()),
    ('perfil', const PantallaDePerfil()),
  ]) {
    for (final brillo in [Brightness.light, Brightness.dark]) {
      testWidgets('$nombre · ${brillo.name} cumple a11y', (tester) async {
        tester.view.physicalSize = const Size(360, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        await tester.pumpWidget(
          conApp(pantalla, dio: dioSimulado().dio, brillo: brillo),
        );
        await tester.pumpAndSettle();

        await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
        await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
        await expectLater(tester, meetsGuideline(textContrastGuideline));
      });
    }
  }
}
