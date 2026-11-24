import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'ayudas_de_captura.dart';

/// Evidencia visual del QR dinámico de la recarga (2026-10-30, rediseñada el 2026-11-24): pantalla completa, por
/// ancho y tema, en los tres estados — QR de prueba esperando el pago, recarga acreditada (saldo subido) y error al
/// simular el pago. La pantalla se apila sobre otra, como llega desde la billetera, para que se vea su botón de volver.
/// Se generan con `--update-goldens`; no son goldens de comparación.
const _salida =
    '../../../../docs/trabajo/2026-11-23-continuar-traspaso/evidencia/movil-rediseno';
const _cuenta = '11111111-1111-4111-8111-111111111111';
const _orden = '9b3e8dff-d7e2-4c41-8cdc-b1be7320ca40';

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
      for (final estado in ['pendiente', 'acreditada', 'error']) {
        testWidgets('recarga con QR · $estado · $ancho dp · $tema', (t) async {
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
          adaptador.onPost(
            '/recargas-de-prueba/$_orden/pago',
            (s) => estado == 'error'
                ? s.reply(422, {
                    'codigo': 'AP-CU10-05',
                    'mensaje': 'Esa orden de recarga ya vencio.',
                    'trazaId': 'traza-captura',
                  })
                : s.reply(200, {
                    'ordenRecargaId': _orden,
                    'transaccionId': 'b77459fb-a680-4709-81f4-1ef25df4a104',
                    'saldoDespues': {'monto': '650.00', 'moneda': 'BOB'},
                  }),
          );

          await montarParaCaptura(
            t,
            const ComoRutaApilada(
              pantalla: PantallaRecargar(cuentaId: _cuenta),
            ),
            dio,
            brillo,
            tam,
            escalaDeTexto: 1.0,
          );
          await asentar(t);
          await t.enterText(find.byType(TextField).first, '150,00');
          await t.pump();
          await t.tap(find.widgetWithText(FilledButton, 'Recargar'));
          await asentar(t);
          if (estado != 'pendiente') {
            await t.ensureVisible(find.text('Simular pago'));
            await t.tap(find.text('Simular pago'));
            await asentar(t);
            if (estado == 'error') {
              // Sin desplazar: el aviso queda arriba de todo, donde lo ve quien tocó el botón.
              expect(find.text('Volver a intentar'), findsOneWidget);
            } else {
              await t.scrollUntilVisible(
                find.text('Tu recarga se acreditó.'),
                -200,
              );
              await t.pumpAndSettle();
            }
          }

          await fotoDeCaptura(
            t,
            '$_salida/recarga-qr-$estado-$ancho-$tema.png',
            '$estado $ancho $tema',
          );
        });
      }
    }
  }
}
