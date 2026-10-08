import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_unirse.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _codigo =
    '12345678-1234-4234-8234-123456789abc.'
    '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';

void main() {
  setUpAll(() async {
    final iconos = FontLoader('MaterialIcons')
      ..addFont(rootBundle.load('fonts/MaterialIcons-Regular.otf'));
    final bricolage = FontLoader('Bricolage')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-600.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Bricolage-700.ttf'),
      );
    final instrument = FontLoader('Instrument')
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-400.ttf'),
      )
      ..addFont(
        rootBundle.load('packages/aportaya_diseno/fuentes/Instrument-600.ttf'),
      );
    await Future.wait([iconos.load(), bricolage.load(), instrument.load()]);
  });

  for (final (ancho, alto, brillo, escala) in [
    (320.0, 760.0, Brightness.light, 2.0),
    (320.0, 760.0, Brightness.dark, 2.0),
    (768.0, 1024.0, Brightness.light, 1.0),
  ]) {
    testWidgets(
      'invitación error→carga→éxito $ancho×$alto ${brillo.name}',
      (tester) async {
        tester.view.physicalSize = Size(ancho, alto);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final almacen = AlmacenEnMemoria();
        await Sesion(
          almacen,
        ).guardar(acceso: 'acceso-prueba', refresco: 'refresco-prueba');
        final (:dio, :adaptador) = dioSimulado();
        addTearDown(adaptador.close);
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
              } else {
                respuestaPendiente = handler;
              }
            },
          ),
        );
        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              almacenSeguroProvider.overrideWithValue(almacen),
              dioProvider.overrideWithValue(dio),
            ],
            child: MaterialApp(
              debugShowCheckedModeBanner: false,
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: TextScaler.linear(escala)),
                child: child!,
              ),
              home: const PantallaUnirse(codigo: _codigo),
            ),
          ),
        );
        await asentar(tester);
        final nombreCelda =
            '${ancho.toInt()}_${brillo.name}_texto_${(escala * 100).toInt()}';
        if (ancho == 320) {
          expect(find.text('Reintentar'), findsOneWidget);
          await expectLater(
            find.byType(MaterialApp),
            matchesGoldenFile('imagenes/invitacion_error_$nombreCelda.png'),
          );
        }

        await tester.tap(find.text('Reintentar'));
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 250));
        expect(consultas, 2);
        expect(find.byType(CircularProgressIndicator), findsOneWidget);
        expect(find.text('Reintentar'), findsNothing);
        if (ancho == 320) {
          await expectLater(
            find.byType(MaterialApp),
            matchesGoldenFile('imagenes/invitacion_carga_$nombreCelda.png'),
          );
        }

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
        expect(find.textContaining('No pudimos verificar'), findsNothing);
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile('imagenes/invitacion_exito_$nombreCelda.png'),
        );
        if (ancho == 320) {
          final accion = find.widgetWithText(ElevatedButton, 'Unirme al grupo');
          await tester.ensureVisible(accion);
          await tester.pumpAndSettle();
          expect(tester.getBottomRight(accion).dy, lessThanOrEqualTo(alto));
          await expectLater(
            find.byType(MaterialApp),
            matchesGoldenFile(
              'imagenes/invitacion_exito_abajo_$nombreCelda.png',
            ),
          );
        }
      },
      skip: !Platform.isWindows,
    );
  }

  for (final (estado, codigo, brillo) in [
    (422, 'AP-CU69-05', Brightness.light),
    (422, 'AP-CU69-05', Brightness.dark),
    (403, 'AP-DESCONOCIDO', Brightness.light),
    (403, 'AP-DESCONOCIDO', Brightness.dark),
  ]) {
    testWidgets(
      'invitación rechazada $estado · ${brillo.name} · 320/200',
      (tester) async {
        tester.view.physicalSize = const Size(320, 760);
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.reset);
        final almacen = AlmacenEnMemoria();
        await Sesion(
          almacen,
        ).guardar(acceso: 'acceso-prueba', refresco: 'refresco-prueba');
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
          ProviderScope(
            overrides: [
              almacenSeguroProvider.overrideWithValue(almacen),
              dioProvider.overrideWithValue(dio),
            ],
            child: MaterialApp(
              debugShowCheckedModeBanner: false,
              theme: temaDesde(Tokens.claro, Brightness.light),
              darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
              themeMode: brillo == Brightness.dark
                  ? ThemeMode.dark
                  : ThemeMode.light,
              builder: (context, child) => MediaQuery(
                data: MediaQuery.of(
                  context,
                ).copyWith(textScaler: const TextScaler.linear(2)),
                child: child!,
              ),
              home: const PantallaUnirse(codigo: _codigo),
            ),
          ),
        );
        await asentar(tester);
        expect(find.text('Ir a portada'), findsOneWidget);
        expect(find.text('Reintentar'), findsNothing);
        expect(find.textContaining('mensaje interno'), findsNothing);
        expect(tester.takeException(), isNull);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/invitacion_rechazada_${estado}_320_${brillo.name}_texto_200.png',
          ),
        );
      },
      skip: !Platform.isWindows,
    );
  }
}
