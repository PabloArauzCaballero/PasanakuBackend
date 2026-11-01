import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/navegacion/rutas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'ayudas_de_captura.dart';

/// Evidencia (no golden de comparación) del login con «Continuar mi registro» y del
/// paso 1 en modo continuar. `flutter test test/capturas/continuar_alta_captura_test.dart --update-goldens`.
const _salida =
    '../../../../docs/trabajo/2026-10-30-continuar-alta-desde-login/evidencia';
void main() {
  setUpAll(cargarFuentesReales);
  for (final (ruta, nombre) in [
    ('/ingreso', 'login'),
    ('/registro?continuar=1', 'paso1-continuar'),
  ]) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      testWidgets('$nombre · $tema', (t) async {
        t.view.physicalSize = const Size(412 * 3, 915 * 3);
        t.view.devicePixelRatio = 3;
        addTearDown(t.view.reset);
        final router = crearEnrutador(sesion: await sesionAnonimaDePrueba());
        await t.pumpWidget(
          ProviderScope(
            child: MaterialApp.router(
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              routerConfig: router,
            ),
          ),
        );
        router.go(ruta);
        await t.pumpAndSettle();
        await fotoSinExigir(t, '$_salida/$nombre-$tema.png');
      });
    }
  }
}
