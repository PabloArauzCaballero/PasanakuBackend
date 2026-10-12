import 'dart:io';

import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/localizacion.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// H17 · capturas de las pantallas que tocó el carril T (D4 billetera, segundo factor, idioma del selector de fecha):
/// 3 tamaños × 2 temas, texto al 130 %, sin desborde. Se generan con `flutter test test/capturas --update-goldens` (el CI no corre esta carpeta) a `evidencia/carril-H17/capturas-app/`;
/// NO son goldens de comparacion (el CI de macOS es la autoridad de los goldens).
const _salida =
    '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-H17/capturas-app';
const _propia = '334cc0fa-e284-49fa-82e3-7570015d12af';
const _tamanos = {
  'movil': Size(360, 740),
  'tablet': Size(768, 1024),
  'escritorio': Size(1280, 900),
};

Future<void> _montar(
  WidgetTester t,
  Widget pantalla,
  Dio dio,
  Brightness brillo,
  Size tam, {
  bool conIdioma = false,
}) async {
  t.view.physicalSize = tam;
  t.view.devicePixelRatio = 1;
  t.platformDispatcher.textScaleFactorTestValue = 1.3;
  addTearDown(t.view.reset);
  addTearDown(t.platformDispatcher.clearAllTestValues);
  await t.pumpWidget(
    ProviderScope(
      retry: (r, e) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        theme: temaDesde(Tokens.claro, Brightness.light),
        darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
        themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
        supportedLocales: conIdioma ? idiomasDeLaApp : const [Locale('en')],
        localizationsDelegates: conIdioma ? delegadosDeIdioma : null,
        home: pantalla,
      ),
    ),
  );
}

Future<void> _foto(WidgetTester t, String nombre) async {
  expect(t.takeException(), isNull, reason: '$nombre: sin desborde ni error');
  await expectLater(
    find.byType(MaterialApp),
    matchesGoldenFile('$_salida/$nombre.png'),
  );
}

/// En `flutter test` el texto sale con la fuente de prueba (bloques) salvo que se carguen las reales: las del paquete de diseno
/// y los iconos de Material del SDK. Sin esto la captura no se puede leer.
Future<void> _cargarFuentes() async {
  final repo = raizDelRepositorio();
  Future<void> familia(String nombre, List<String> rutas) async {
    final cargador = FontLoader(nombre);
    for (final r in rutas) {
      cargador.addFont(
        File(r).readAsBytes().then((b) => ByteData.view(b.buffer)),
      );
    }
    await cargador.load();
  }

  const dir = 'packages/diseno_flutter/lib/fuentes';
  await familia('Bricolage', [
    for (final w in [600, 700, 800]) '$repo/$dir/Bricolage-$w.ttf',
  ]);
  await familia('Instrument', [
    for (final w in [400, 500, 600]) '$repo/$dir/Instrument-$w.ttf',
  ]);
  final sdk =
      Platform.environment['FLUTTER_ROOT'] ?? '/tmp/claude-0/flutter-3448';
  await familia('MaterialIcons', [
    '$sdk/bin/cache/artifacts/material_fonts/MaterialIcons-Regular.otf',
  ]);
}

void main() {
  setUpAll(_cargarFuentes);
  for (final MapEntry(key: nombreTam, value: tam) in _tamanos.entries) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      final etq = '$nombreTam-$tema';

      testWidgets('billetera con saldo · $etq', (t) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/billetera/cuentas/propia',
          (s) => s.reply(
            200,
            ejemplo(
              'nucleo-financiero',
              'consultarCuentaPropia',
              'ok',
            )['cuerpo'],
          ),
          queryParameters: {'moneda': 'BOB'},
        );
        adaptador.onGet(
          '/billetera/$_propia/saldo',
          (s) => s.reply(200, {
            ...(ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo']
                as Map<String, dynamic>),
            'cuentaId': _propia,
          }),
        );
        await _montar(t, const PantallaDeSaldo(), dio, brillo, tam);
        await asentar(t);
        await _foto(t, 'billetera-con-saldo-$etq');
      });

      testWidgets('billetera todavia no activa (AP-CU13-01) · $etq', (t) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onGet(
          '/billetera/cuentas/propia',
          (s) => s.reply(422, {
            'codigo': 'AP-CU13-01',
            'mensaje': 'Esa cuenta no existe',
            'trazaId': 'traza-h17',
          }),
          queryParameters: {'moneda': 'BOB'},
        );
        await _montar(t, const PantallaDeSaldo(), dio, brillo, tam);
        await asentar(t);
        await _foto(t, 'billetera-no-activa-$etq');
      });

      testWidgets('segundo factor · inicial y rechazo · $etq', (t) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost(
          '/sesiones',
          (s) => s.reply(422, {
            'codigo': 'AP-CU04-04',
            'mensaje': 'x',
            'detalle': <String, dynamic>{},
            'trazaId': 'traza-mfa',
          }),
          data: Matchers.any,
        );
        await _montar(t, const PantallaDeMfa(), dio, brillo, tam);
        await t.pump();
        await _foto(t, 'mfa-inicial-$etq');
        await t.enterText(find.byType(TextField).first, '123456');
        await asentar(t);
        await _foto(t, 'mfa-rechazo-$etq');
      });

      testWidgets('selector de fecha en español (telefono en ingles) · $etq', (
        t,
      ) async {
        t.platformDispatcher.localesTestValue = [const Locale('en', 'US')];
        t.platformDispatcher.localeTestValue = const Locale('en', 'US');
        final (:dio, adaptador: _) = dioSimulado();
        await _montar(
          t,
          Scaffold(
            body: CampoDeFecha(
              etiqueta: 'Fecha de nacimiento',
              valor: null,
              inicial: DateTime(1990, 10, 11),
              onElegida: (_) {},
            ),
          ),
          dio,
          brillo,
          tam,
          conIdioma: true,
        );
        await t.tap(find.byIcon(Icons.calendar_month_outlined));
        await t.pumpAndSettle();
        await _foto(t, 'selector-de-fecha-$etq');
      });
    }
  }
}
