import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

void main() {
  testWidgets('reintentar consulta exitosa quita el error anterior', (
    tester,
  ) async {
    final almacen = AlmacenEnMemoria();
    await Sesion(
      almacen,
    ).guardar(acceso: 'acceso-prueba', refresco: 'refresco');
    final (:dio, :adaptador) = dioSimulado();
    var consultas = 0;
    RequestInterceptorHandler? respuestaPendiente;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, handler) {
          if (opciones.path != '/grupos/invitaciones/enlace/consultar') {
            handler.next(opciones);
            return;
          }
          consultas++;
          if (consultas == 1) {
            handler.reject(
              DioException(
                requestOptions: opciones,
                type: DioExceptionType.connectionTimeout,
              ),
            );
            return;
          }
          respuestaPendiente = handler;
        },
      ),
    );
    addTearDown(adaptador.close);

    await tester.pumpWidget(
      conApp(
        const PantallaUnirse(codigo: _codigo),
        dio: dio,
        almacenSeguro: almacen,
      ),
    );
    await asentar(tester);
    expect(
      find.textContaining('No pudimos verificar la invitación'),
      findsOneWidget,
    );
    expect(find.text('Reintentar'), findsOneWidget);

    await tester.tap(find.text('Reintentar'));
    await tester.tap(find.text('Reintentar'));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 250));
    expect(consultas, 2);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    expect(find.text('Reintentar'), findsNothing);
    expect(
      find.textContaining('No pudimos verificar la invitación'),
      findsNothing,
    );

    respuestaPendiente!.resolve(
      Response<Map<String, dynamic>>(
        requestOptions: RequestOptions(
          path: '/grupos/invitaciones/enlace/consultar',
        ),
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
    await asentar(tester);
    expect(find.text('Grupo de prueba'), findsOneWidget);
    expect(find.text('Aporte por período: Bs 25,00'), findsOneWidget);
    expect(find.text('Frecuencia: Mensual'), findsOneWidget);
    expect(
      find.textContaining('No pudimos verificar la invitación'),
      findsNothing,
    );
    expect(find.text('Reintentar'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('detalle monetario fuera de contrato no se muestra', (
    tester,
  ) async {
    final almacen = AlmacenEnMemoria();
    await Sesion(
      almacen,
    ).guardar(acceso: 'acceso-prueba', refresco: 'refresco');
    final (:dio, :adaptador) = dioSimulado();
    addTearDown(adaptador.close);
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, handler) => handler.resolve(
          Response<Map<String, dynamic>>(
            requestOptions: opciones,
            statusCode: 200,
            data: {
              'nombre': 'Grupo no verificable',
              'montoAporte': 25,
              'moneda': 'BOB',
              'periodicidad': 'MENSUAL',
              'reglamento': 'Reglamento de prueba',
              'hashReglamento': 'a' * 64,
            },
          ),
        ),
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
    expect(find.text('Grupo no verificable'), findsNothing);
    expect(
      find.textContaining('No pudimos verificar la invitación'),
      findsOneWidget,
    );
    expect(find.text('Reintentar'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  for (final (estado, codigo, texto) in [
    (422, 'AP-CU69-05', 'Esta invitación ya no es válida.'),
    (403, 'AP-DESCONOCIDO', 'No tenés acceso a esta invitación.'),
  ]) {
    testWidgets('error $estado ofrece salida, no reintento', (tester) async {
      final almacen = AlmacenEnMemoria();
      await Sesion(
        almacen,
      ).guardar(acceso: 'acceso-prueba', refresco: 'refresco');
      final (:dio, :adaptador) = dioSimulado();
      addTearDown(adaptador.close);
      adaptador.onPost(
        '/grupos/invitaciones/enlace/consultar',
        (servidor) => servidor.reply(estado, {
          'codigo': codigo,
          'mensaje': 'mensaje interno no publicable',
          'trazaId': 'sintetica',
        }),
      );
      await tester.pumpWidget(
        conApp(
          const PantallaUnirse(codigo: _codigo),
          dio: dio,
          almacenSeguro: almacen,
        ),
      );
      await asentar(tester);
      expect(find.textContaining(texto), findsOneWidget);
      expect(find.text('Ir a portada'), findsOneWidget);
      expect(find.text('Reintentar'), findsNothing);
      expect(find.textContaining('mensaje interno'), findsNothing);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('sesión vencida vuelve a mostrar ingreso', (tester) async {
    final almacen = AlmacenEnMemoria();
    await Sesion(
      almacen,
    ).guardar(acceso: 'acceso-prueba', refresco: 'refresco');
    final dio = Dio();
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (opciones, handler) => handler.reject(
          DioException(
            requestOptions: opciones,
            error: ErrorDeApi(
              codigo: 'AP-DESCONOCIDO',
              trazaId: 'sintetica',
              estado: 401,
            ),
          ),
        ),
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
    expect(find.text('Ingresar'), findsOneWidget);
    expect(find.text('Crear cuenta'), findsOneWidget);
    expect(find.text('Reintentar'), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
