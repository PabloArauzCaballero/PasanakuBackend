import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/respuesta_aceptacion_invitacion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
const _grupo = '11111111-1111-4111-8111-111111111111';
const _participante = '22222222-2222-4222-8222-222222222222';

void main() {
  test('la confirmación requiere grupo y participante UUID', () {
    expect(
      grupoDeAceptacionConfirmada({
        'grupoId': _grupo,
        'participanteId': _participante,
      }),
      _grupo,
    );
    for (final respuesta in [
      {'grupoId': _grupo},
      {'participanteId': _participante},
      {'grupoId': 'no-uuid', 'participanteId': _participante},
    ]) {
      expect(
        () => grupoDeAceptacionConfirmada(respuesta),
        throwsFormatException,
      );
    }
  });

  testWidgets(
    'respuesta de aceptación incompleta conserva clave y no simula éxito',
    (tester) async {
      final almacen = AlmacenEnMemoria();
      await Sesion(
        almacen,
      ).guardar(acceso: 'acceso-prueba', refresco: 'refresco');
      final (:dio, :adaptador) = dioSimulado();
      addTearDown(adaptador.close);
      var aceptaciones = 0;
      final claves = <Object?>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (opciones, handler) {
            if (opciones.path == '/grupos/invitaciones/enlace/consultar') {
              handler.resolve(
                Response<Map<String, dynamic>>(
                  requestOptions: opciones,
                  statusCode: 200,
                  data: {
                    'nombre': 'Grupo de prueba',
                    'montoAporte': '25.00',
                    'moneda': 'BOB',
                    'periodicidad': 'MENSUAL',
                    'reglamento': 'Reglamento de prueba',
                    'hashReglamento': 'a' * 64,
                  },
                ),
              );
              return;
            }
            if (opciones.path == '/grupos/invitaciones/enlace/aceptar') {
              aceptaciones++;
              claves.add(opciones.headers['Idempotency-Key']);
              handler.resolve(
                Response<Map<String, dynamic>>(
                  requestOptions: opciones,
                  statusCode: 201,
                  data: aceptaciones == 1
                      ? {'participanteId': _participante}
                      : {'grupoId': _grupo, 'participanteId': _participante},
                ),
              );
              return;
            }
            handler.next(opciones);
          },
        ),
      );

      await tester.pumpWidget(
        conApp(
          const PantallaUnirse(codigo: _codigo),
          dio: dio,
          almacenSeguro: almacen,
        ),
      );
      await asentar(tester);
      await tester.tap(find.byType(CheckboxListTile));
      await tester.pump();
      await tester.ensureVisible(find.text('Unirme al grupo'));
      await tester.tap(find.text('Unirme al grupo'));
      await asentar(tester);

      expect(aceptaciones, 1);
      expect(find.text('Ya formás parte del grupo.'), findsNothing);
      expect(find.text('Grupo de prueba'), findsOneWidget);
      expect(
        find.textContaining('No pudimos confirmar tu ingreso'),
        findsOneWidget,
      );
      expect(claves.single, isNotNull);

      await tester.ensureVisible(find.text('Unirme al grupo'));
      await tester.tap(find.text('Unirme al grupo'));
      await asentar(tester);

      expect(aceptaciones, 2);
      expect(claves[1], claves[0]);
      expect(find.text('Ya formás parte del grupo.'), findsOneWidget);
      expect(find.text('Ir a portada'), findsOneWidget);
      expect(find.text('Ver mis grupos'), findsNothing);
      expect(tester.takeException(), isNull);
    },
  );
}
