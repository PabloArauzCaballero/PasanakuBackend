import 'package:aportaya_movil/pantallas/billetera/acciones_de_saldo.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_de_saldo.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'ayudas_de_captura.dart';

/// H17.S9.M7 · las tres acciones de la billetera no pueden quedar cortadas ("Apor…") a 360 px con texto grande.
/// Se corre dos veces: `--dart-define=FASE=antes` (documenta el defecto) y sin define (exige que no haya texto cortado).
const _fase = String.fromEnvironment('FASE', defaultValue: 'despues');
const _salida =
    '../../../../docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-H17/capturas-s9';
const _propia = '334cc0fa-e284-49fa-82e3-7570015d12af';

bool _hayTextoCortado(WidgetTester t) => t
    .renderObjectList<RenderParagraph>(
      find.descendant(
        of: find.byType(AccionesDeSaldo),
        matching: find.byType(RichText),
      ),
    )
    .any((p) => p.didExceedMaxLines);

void main() {
  setUpAll(cargarFuentesReales);
  for (final escala in [1.0, 1.3, 2.0]) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      testWidgets('acciones del saldo · 360 px · texto ${escala}x · $tema', (
        t,
      ) async {
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
        await montarParaCaptura(
          t,
          const PantallaDeSaldo(),
          dio,
          brillo,
          const Size(360, 740),
          escalaDeTexto: escala,
        );
        await asentar(t);
        final cortado = _hayTextoCortado(t);
        await fotoSinExigir(
          t,
          '$_salida/m7-$_fase-movil-texto${escala}x-$tema.png',
        );
        if (_fase != 'antes') {
          expect(t.takeException(), isNull);
          expect(
            cortado,
            isFalse,
            reason: 'una accion quedo con el texto cortado',
          );
          expect(find.text('Aportar'), findsOneWidget);
          expect(find.text('Recargar'), findsOneWidget);
          expect(find.text('Retirar'), findsOneWidget);
        }
        // ignore: avoid_print
        print('M7 $_fase texto ${escala}x $tema -> textoCortado=$cortado');
      });
    }
  }
}
