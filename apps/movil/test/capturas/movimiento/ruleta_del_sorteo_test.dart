import 'dart:convert';
import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_sorteo.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../ayudas_de_captura.dart';
import '../../comun.dart';

/// Evidencia del sorteo con la ruleta: la ceremonia entera, cuadro por cuadro, a 2×.
/// Datos sintéticos; el orden lo «devuelve» el doble del servidor.
/// `flutter test test/capturas/movimiento/ruleta_del_sorteo_test.dart --update-goldens
///   --dart-define=CUADROS_RULETA=CARPETA` (carpeta absoluta); sin ella no corre.
const _salida = String.fromEnvironment('CUADROS_RULETA');
const _fps = 24;
const _grupo = 'c6900000-0000-4000-8000-0000000000f1';
const _nombres = [
  'Rosa Quispe',
  'Juan Mamani',
  'Ana Choque',
  'Luis Condori',
  'Carla Flores',
];
String _cupo(int i) => 'c6900000-0000-4000-8000-0000000000a${i + 1}';

Map<String, dynamic> _detalle() => {
  'grupoId': _grupo,
  'nombre': 'Pasanaku del barrio',
  'estado': 'CONFORMADO',
  'montoAporte': '500.00',
  'moneda': 'BOB',
  'periodicidad': 'MENSUAL',
  'cuposTotales': _nombres.length,
  'cuposOcupados': _nombres.length,
  'soyOrganizador': true,
  'debeFirmar': false,
  'puedeSortear': true,
  'reglamento': {'texto': 'Reglamento.', 'hash': 'c' * 64},
  'integrantes': [
    for (var i = 0; i < _nombres.length; i++)
      {
        'numero': i + 1,
        'cupoId': _cupo(i),
        'nombre': _nombres[i],
        'estado': 'FIRMADO',
        'esOrganizador': i == 1,
        'soyYo': i == 1,
        'turno': null,
      },
  ],
};

String _token() {
  String parte(Object v) =>
      base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({
    'sub': 'b8000000-0000-4000-8000-000000000088',
    'permisos': ['BILLETERA_VER'],
  })}.';
}

void main() {
  setUpAll(cargarFuentesReales);

  for (final (nombre, brillo) in [
    ('claro', Brightness.light),
    ('oscuro', Brightness.dark),
  ]) {
    testWidgets('ruleta del sorteo · $nombre', skip: _salida.isEmpty, (t) async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador
        ..onGet('/grupos/$_grupo/detalle', (s) => s.reply(200, _detalle()))
        ..onPost(
          '/grupos/$_grupo/sorteo',
          (s) => s.reply(201, {
            'sorteoId': 'c6900000-0000-4000-8000-0000000000c1',
            'hashSemilla': 'c' * 64,
            'algoritmo': 'FISHER_YATES_SHA256',
          }),
        )
        ..onPost(
          '/grupos/$_grupo/sorteo/revelacion',
          (s) => s.reply(200, {
            'sorteoId': 'c6900000-0000-4000-8000-0000000000c1',
            'verificado': true,
            'cuposEnOrden': [_cupo(2), _cupo(4), _cupo(1), _cupo(0), _cupo(3)],
          }),
          data: Matchers.any,
        );
      final sesion = Sesion(AlmacenEnMemoria());
      await sesion.abrir(acceso: _token());
      addTearDown(sesion.dispose);
      t.view.physicalSize = const Size(824, 1800);
      t.view.devicePixelRatio = 2;
      addTearDown(t.view.reset);
      await t.pumpWidget(
        ProviderScope(
          retry: (_, _) => null,
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(dio),
            sesionProvider.overrideWithValue(sesion),
          ],
          child: MaterialApp(
            debugShowCheckedModeBanner: false,
            theme: temaDesde(Tokens.claro, Brightness.light),
            darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
            themeMode: brillo == Brightness.dark
                ? ThemeMode.dark
                : ThemeMode.light,
            home: const PantallaSorteo(grupoId: _grupo),
          ),
        ),
      );
      await asentar(t);
      final dir = '$_salida/$nombre';
      Directory(dir).createSync(recursive: true);
      var cuadro = 0;
      Future<void> foto() => fotoSinExigir(
        t,
        '$dir/${(cuadro++).toString().padLeft(4, '0')}.png',
      );
      await foto();
      await t.tap(find.text('Girar la ruleta'));
      // Se filma hasta que la pantalla deja de pedir cuadros (fin de la ceremonia).
      while (t.binding.hasScheduledFrame || cuadro < 3) {
        await t.pump(const Duration(milliseconds: 1000 ~/ _fps));
        await foto();
        if (cuadro > _fps * 60) break;
      }
      for (var k = 0; k < _fps; k++) {
        await foto();
      }
      // Se vacía el reloj antes de desmontar, como en la prueba del sorteo.
      await t.pump(const Duration(seconds: 5));
    });
  }
}
