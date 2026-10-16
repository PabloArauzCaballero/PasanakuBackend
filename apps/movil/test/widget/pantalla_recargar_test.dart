import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/billetera/pantalla_recargar.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';
const ruta = '/billetera/recargas';

/// CU-10 — el gate propio del carril: toda operación de dinero envía clave de
/// idempotencia y bloquea el botón, probado con doble toque real. Se escribe un
/// monto válido y se toca «Recargar» DOS veces seguidas sin esperar entre medio; el
/// botón queda `cargando` desde el primer toque (`Boton.cargando`), así que solo
/// UNA petición sale al servidor y con la MISMA clave de idempotencia.
void main() {
  testWidgets(
    'doble toque en la recarga: una sola petición, la misma clave de idempotencia',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();

      var llamadas = 0;
      final claves = <String?>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == ruta) {
              llamadas++;
              claves.add(options.headers['Idempotency-Key'] as String?);
            }
            handler.next(options);
          },
        ),
      );

      adaptador.onPost(
        ruta,
        (s) => s.reply(
          201,
          ejemplo('nucleo-financiero', 'solicitarRecarga', 'ok')['cuerpo'],
          delay: const Duration(milliseconds: 200),
        ),
      );

      await tester.pumpWidget(
        conApp(const PantallaRecargar(cuentaId: cuenta), dio: dio),
      );
      await tester.pump();

      // Un importe válido: sin él el botón queda deshabilitado.
      await tester.enterText(find.byType(TextField).first, '1.240,00');
      await tester.pump();

      final boton = find.widgetWithText(FilledButton, 'Recargar');
      await tester.tap(boton);
      await tester
          .pump(); // primer toque: arranca el envío, el botón queda cargando
      await tester.tap(boton); // segundo toque, inmediato: ya está bloqueado
      await tester.pump();

      await asentar(tester);

      expect(
        llamadas,
        1,
        reason: 'el doble toque no puede crear dos órdenes de recarga',
      );
      expect(claves, hasLength(1));
      expect(claves.single, isNotNull);
      expect(
        find.text(
          'Tu recarga quedó en camino. Te avisamos cuando el banco la confirme.',
        ),
        findsOneWidget,
      );
    },
  );
}
