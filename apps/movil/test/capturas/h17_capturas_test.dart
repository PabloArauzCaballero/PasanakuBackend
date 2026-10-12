import 'package:aportaya_diseno/moleculas/campo_de_fecha.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_mfa.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';
import 'ayudas_de_captura.dart';

/// H17 · capturas de las pantallas que tocó el carril T (D4 billetera, segundo factor, idioma del selector de fecha):
/// 3 tamaños × 2 temas, texto al 130 %, sin desborde. Se generan con `flutter test test/capturas --update-goldens` (el CI no corre esta carpeta) a `evidencia/carril-H17/capturas-app/`;
/// NO son goldens de comparacion (el CI de macOS es la autoridad de los goldens).
const _salida =
    '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-H17/capturas-app';
const _propia = '334cc0fa-e284-49fa-82e3-7570015d12af';
void main() {
  setUpAll(cargarFuentesReales);
  for (final MapEntry(key: nombreTam, value: tam) in tamanosDeCaptura.entries) {
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
        await montarParaCaptura(t, const PantallaDeSaldo(), dio, brillo, tam);
        await asentar(t);
        await fotoDeCaptura(
          t,
          '$_salida/billetera-con-saldo-$etq.png',
          'billetera-con-saldo-$etq',
        );
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
        await montarParaCaptura(t, const PantallaDeSaldo(), dio, brillo, tam);
        await asentar(t);
        await fotoDeCaptura(
          t,
          '$_salida/billetera-no-activa-$etq.png',
          'billetera-no-activa-$etq',
        );
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
        await montarParaCaptura(t, const PantallaDeMfa(), dio, brillo, tam);
        await t.pump();
        await fotoDeCaptura(
          t,
          '$_salida/mfa-inicial-$etq.png',
          'mfa-inicial-$etq',
        );
        await t.enterText(find.byType(TextField).first, '123456');
        await asentar(t);
        await fotoDeCaptura(
          t,
          '$_salida/mfa-rechazo-$etq.png',
          'mfa-rechazo-$etq',
        );
      });

      testWidgets('selector de fecha en español (telefono en ingles) · $etq', (
        t,
      ) async {
        t.platformDispatcher.localesTestValue = [const Locale('en', 'US')];
        t.platformDispatcher.localeTestValue = const Locale('en', 'US');
        final (:dio, adaptador: _) = dioSimulado();
        await montarParaCaptura(
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
        await fotoDeCaptura(
          t,
          '$_salida/selector-de-fecha-$etq.png',
          'selector-de-fecha-$etq',
        );
      });
    }
  }
}
