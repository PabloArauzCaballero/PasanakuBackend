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

/// La base para el Dio de pruebas — nunca `baseDelGateway`: en `flutter test` no hay
/// `--dart-define=API=...`, así que `baseDelGateway` es `null` a propósito (H3.S3.M2:
/// sin el define, no hay URL por omisión). El adaptador de acá abajo es simulado y
/// nunca hace una petición real, así que el valor exacto no importa — solo que sea
/// una URL válida para que `Dio` no se queje al construirse.
const String _baseDePrueba = 'https://pruebas.aportaya.bo/api/v1';

/// Un Dio con adaptador simulado y los MISMOS interceptores que la app real.
({Dio dio, DioAdapter adaptador}) dioSimulado() {
  final dio = Dio(
    BaseOptions(
      baseUrl: _baseDePrueba,
      headers: {'Accept': 'application/json'},
    ),
  );
  final adaptador = DioAdapter(dio: dio, matcher: const UrlRequestMatcher());
  instalarInterceptores(dio, Sesion(AlmacenEnMemoria()));
  return (dio: dio, adaptador: adaptador);
}

/// Los tres contratos vigentes que devuelve `GET /cumplimiento/contratos/vigentes`.
///
/// Toda pantalla del alta que llegue al contrato los necesita: sin ellos las tres
/// casillas no tienen ids que mandar y «Aceptar y continuar» queda apagado con razón.
List<Map<String, dynamic>> contratosVigentesDePrueba() => [
  for (final (tipo, codigo) in [
    ('BILLETERA', 'CTO-BILLETERA'),
    ('TARIFAS', 'CTO-TARIFAS'),
    ('TRATAMIENTO_DATOS', 'CTO-DATOS'),
  ])
    {
      'id':
          '00000000-0000-4000-8000-0000000000${tipo.length.toString().padLeft(2, '0')}',
      'codigo': codigo,
      'version': 1,
      'tipo': tipo,
      'urlDocumento': 'https://aportaya.bo/legal/$codigo.pdf',
      'hashDocumento': 'a' * 64,
      'vigenteDesde': '2026-01-01T00:00:00Z',
    },
];

/// Deja el adaptador listo para responder esa consulta.
void conContratosVigentes(DioAdapter adaptador) => adaptador.onGet(
  '/cumplimiento/contratos/vigentes',
  (s) => s.reply(200, contratosVigentesDePrueba()),
);

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
