import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_pedir_cupo.dart';

const grupoId = '11111111-1111-4111-8111-111111111111';
const ruta = '/grupos/$grupoId/postulaciones';

/// CU-68 / D-15 — el botón dice "Pedir mi cupo" (nunca "Unirme"): pedir el cupo no
/// lo ocupa. Y el gate común de dinero/efectos también aplica acá: doble toque, una
/// sola postulación con la misma clave de idempotencia.
void main() {
  testWidgets('el botón para postular dice "Pedir mi cupo"', (tester) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      ruta,
      (s) => s.reply(201, ejemplo('grupos', 'postularAlGrupo', 'ok')['cuerpo']),
    );

    await tester.pumpWidget(
      conApp(const PantallaPedirCupo(grupoId: grupoId), dio: dio),
    );
    await tester.pump();

    expect(find.text('Pedir mi cupo'), findsWidgets);
    expect(find.text('Unirme'), findsNothing);
  });

  testWidgets(
    'doble toque al pedir el cupo: una sola postulación, la misma clave',
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
          ejemplo('grupos', 'postularAlGrupo', 'ok')['cuerpo'],
          delay: const Duration(milliseconds: 200),
        ),
      );

      await tester.pumpWidget(
        conApp(const PantallaPedirCupo(grupoId: grupoId), dio: dio),
      );
      await tester.pump();

      final boton = find.widgetWithText(FilledButton, 'Pedir mi cupo');
      await tester.tap(boton);
      await tester.pump();
      await tester.tap(boton);
      await tester.pump();

      await asentar(tester);

      expect(llamadas, 1, reason: 'el doble toque no puede crear dos pedidos');
      expect(claves, hasLength(1));
      expect(find.text('Tu pedido de cupo'), findsOneWidget);
      expect(
        find.textContaining('no lo va a mostrar hasta que te acepten'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'el cupo y el mensaje elegidos son los que viajan; el mensaje vacío no',
    (tester) async {
      final (:dio, :adaptador) = dioSimulado();
      Object? cuerpo;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == ruta) {
              cuerpo = options.data;
            }
            handler.next(options);
          },
        ),
      );
      adaptador.onPost(
        ruta,
        (s) =>
            s.reply(201, ejemplo('grupos', 'postularAlGrupo', 'ok')['cuerpo']),
      );
      await tester.pumpWidget(
        conApp(const PantallaPedirCupo(grupoId: grupoId), dio: dio),
      );
      await tester.pump();

      await tester.tap(find.text('2'));
      await tester.pump();
      await tester.tap(find.widgetWithText(FilledButton, 'Pedir mi cupo'));
      await tester.pump();
      await asentar(tester);

      // En el interceptor el cuerpo ya viene serializado.
      final enviado = jsonDecode(cuerpo! as String) as Map<String, dynamic>;
      expect(enviado['cuposSolicitados'], 2);
      expect(enviado.containsKey('mensaje'), isFalse);
      // La confirmación muestra las etapas del pedido.
      expect(find.text('Decide el organizador'), findsOneWidget);
    },
  );
}
