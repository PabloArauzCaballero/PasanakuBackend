import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';

/// Almacén en memoria: las pruebas no tocan el Keystore.
class AlmacenEnMemoria implements AlmacenSeguro {
  final _datos = <String, String>{};
  @override
  Future<String?> leer(String clave) async => _datos[clave];
  @override
  Future<void> guardar(String clave, String valor) async =>
      _datos[clave] = valor;
  @override
  Future<void> borrar(String clave) async => _datos.remove(clave);
}

/// Un Dio con adaptador simulado y los MISMOS interceptores que la app real.
({Dio dio, DioAdapter adaptador}) dioSimulado() {
  final dio = Dio(
    BaseOptions(
      baseUrl: baseDelGateway,
      headers: {'Accept': 'application/json'},
    ),
  );
  final adaptador = DioAdapter(dio: dio, matcher: const UrlRequestMatcher());
  instalarInterceptores(dio, Sesion(AlmacenEnMemoria()));
  return (dio: dio, adaptador: adaptador);
}

/// El ejemplo del contrato, leído del MISMO archivo que Prism sirve y que Vitest usa.
Map<String, dynamic> ejemplo(
  String servicio,
  String operacion,
  String escenario,
) {
  final archivo = File(
    '${raizDelRepositorio()}/packages/simulado/ejemplos/$servicio/$operacion.json',
  );
  final json = jsonDecode(archivo.readAsStringSync()) as Map<String, dynamic>;
  return (json['escenarios'] as Map<String, dynamic>)[escenario]
      as Map<String, dynamic>;
}

String raizDelRepositorio() {
  var dir = Directory.current;
  while (!File('${dir.path}/settings.gradle.kts').existsSync()) {
    final padre = dir.parent;
    if (padre.path == dir.path) {
      throw StateError('no encuentro la raíz del repositorio');
    }
    dir = padre;
  }
  return dir.path;
}

/// Monta un widget con el tema real y los proveedores sobreescritos.
Widget conApp(
  Widget hijo, {
  required Dio dio,
  Brightness brillo = Brightness.light,
}) => ProviderScope(
  // Igual que en main.dart: sin reintentos automáticos.
  retry: (retryCount, error) => null,
  overrides: [
    almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
    dioProvider.overrideWithValue(dio),
  ],
  child: MaterialApp(
    theme: temaDesde(Tokens.claro, Brightness.light),
    darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
    themeMode: brillo == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
    home: hijo,
  ),
);

/// Deja que el adaptador simulado responda (usa temporizadores) y que Riverpod entregue
/// el estado: un `pumpAndSettle` solo avanza el reloj mientras haya cuadros programados,
/// y una respuesta de error llega por un temporizador sin cuadro.
Future<void> asentar(WidgetTester tester) async {
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 250));
  await tester.pumpAndSettle();
}
