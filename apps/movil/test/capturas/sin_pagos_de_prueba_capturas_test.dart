import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'ayudas_de_captura.dart';

/// Evidencia de FLUJO-QR-001 (2026-11-24): la misma recarga con un QR de origen PRUEBA, en una
/// build SIN `--dart-define=PAGOS_DE_PRUEBA=true`. No aparece «Simular pago».
/// Se generan con `--update-goldens`; no son goldens de comparación.
const _salida =
    '../../../../docs/trabajo/2026-11-23-continuar-traspaso/evidencia/movil';
const _cuenta = '11111111-1111-4111-8111-111111111111';

const _anchos = {
  '360': Size(360, 740),
  '412': Size(412, 915),
  '768': Size(768, 1024),
};

void main() {
  setUpAll(cargarFuentesReales);
  for (final MapEntry(key: ancho, value: tam) in _anchos.entries) {
    for (final (brillo, tema) in [
      (Brightness.light, 'claro'),
      (Brightness.dark, 'oscuro'),
    ]) {
      testWidgets('recarga sin pagos de prueba · $ancho dp · $tema', (t) async {
        final (:dio, :adaptador) = dioSimulado();
        adaptador.onPost(
          '/billetera/recargas',
          (s) => s.reply(
            201,
            ejemplo(
              'nucleo-financiero',
              'solicitarRecarga',
              'qrDePrueba',
            )['cuerpo'],
          ),
        );
        adaptador.onGet(
          '/billetera/$_cuenta/saldo',
          (s) => s.reply(200, {
            ...ejemplo('nucleo-financiero', 'consultarSaldo', 'ok')['cuerpo'],
            'disponible': {'monto': '500.00', 'moneda': 'BOB'},
          }),
        );

        await montarParaCaptura(
          t,
          const PantallaRecargar(cuentaId: _cuenta),
          dio,
          brillo,
          tam,
          escalaDeTexto: 1.0,
          pagosDePrueba: false,
        );
        await t.pump();
        await t.enterText(find.byType(TextField).first, '150,00');
        await t.pump();
        await t.tap(find.widgetWithText(FilledButton, 'Recargar'));
        await asentar(t);

        expect(find.text('Simular pago'), findsNothing);
        await fotoDeCaptura(
          t,
          '$_salida/recarga-sin-pagos-de-prueba-$ancho-$tema.png',
          'sin pagos de prueba $ancho $tema',
        );
      });
    }
  }
}
