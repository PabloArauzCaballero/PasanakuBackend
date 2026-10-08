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

  for (final brillo in [Brightness.light, Brightness.dark]) {
    testWidgets(
      'aceptación incierta y confirmada 320/200 ${brillo.name}',
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
        var aceptaciones = 0;
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
                handler.resolve(
                  Response<Map<String, dynamic>>(
                    requestOptions: opciones,
                    statusCode: 201,
                    data: aceptaciones == 1
                        ? {
                            'participanteId':
                                '22222222-2222-4222-8222-222222222222',
                          }
                        : {
                            'grupoId': '11111111-1111-4111-8111-111111111111',
                            'participanteId':
                                '22222222-2222-4222-8222-222222222222',
                          },
                  ),
                );
                return;
              }
              handler.next(opciones);
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
                ).copyWith(textScaler: const TextScaler.linear(2)),
                child: child!,
              ),
              home: const PantallaUnirse(codigo: _codigo),
            ),
          ),
        );
        await asentar(tester);
        await tester.tap(find.byType(CheckboxListTile));
        await tester.pump();
        await tester.ensureVisible(find.text('Unirme al grupo'));
        await tester.tap(find.text('Unirme al grupo'));
        await asentar(tester);
        await tester.ensureVisible(find.textContaining('No pudimos confirmar'));
        await tester.pumpAndSettle();
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/invitacion_aceptacion_incierta_320_${brillo.name}_texto_200.png',
          ),
        );

        await tester.ensureVisible(find.text('Unirme al grupo'));
        await tester.tap(find.text('Unirme al grupo'));
        await asentar(tester);
        expect(find.text('Ir a portada'), findsOneWidget);
        await expectLater(
          find.byType(MaterialApp),
          matchesGoldenFile(
            'imagenes/invitacion_aceptacion_confirmada_320_${brillo.name}_texto_200.png',
          ),
        );
        expect(tester.takeException(), isNull);
      },
      skip: !Platform.isWindows,
    );
  }
}
