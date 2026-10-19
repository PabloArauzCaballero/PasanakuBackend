import 'dart:convert';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/navegacion/vigilante_de_aprobacion.dart';
import 'package:aportaya_movil/proveedores/renovar_acceso.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// La app se entera sola de que aprobaron la identidad (2026-10-18), sin salir y
/// volver a entrar.
void main() {
  const perfil = '/usuarios/yo/perfil';
  const usuario = 'b8000000-0000-4000-8000-000000000077';
  const intervalo = Duration(seconds: 30);

  String token(List<String> permisos) {
    String parte(Object v) =>
        base64Url.encode(utf8.encode(jsonEncode(v))).replaceAll('=', '');
    return '${parte({'alg': 'none'})}.${parte({'sub': usuario, 'permisos': permisos})}.';
  }

  Map<String, Object?> cuenta(String estado) => {
    'usuarioId': usuario,
    'correoVerificado': true,
    'estadoDeCuenta': estado,
  };

  Future<({int Function() renovaciones, int Function() consultas})> montar(
    WidgetTester tester, {
    required List<String> permisos,
    required void Function(dynamic adaptador) responder,
  }) async {
    final (:dio, :adaptador) = dioSimulado();
    responder(adaptador);
    var consultas = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          if (o.path.endsWith(perfil)) consultas++;
          h.next(o);
        },
      ),
    );
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.abrir(acceso: token(permisos));
    var renovaciones = 0;
    await tester.pumpWidget(
      ProviderScope(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
          sesionProvider.overrideWithValue(sesion),
          renovarAccesoProvider.overrideWithValue(() async {
            renovaciones++;
            return true;
          }),
        ],
        child: const MaterialApp(
          home: Scaffold(
            body: VigilanteDeAprobacion(
              intervalo: intervalo,
              hijo: Text('Inicio'),
            ),
          ),
        ),
      ),
    );
    return (renovaciones: () => renovaciones, consultas: () => consultas);
  }

  testWidgets('correcto: cuando aprueban la identidad, activa la cuenta sola y '
      'lo avisa', (tester) async {
    final r = await montar(
      tester,
      permisos: const [],
      responder: (a) => a.onGet(perfil, (s) => s.reply(200, cuenta('ACTIVO'))),
    );

    await tester.pump(intervalo);
    await asentar(tester);

    expect(r.consultas(), 1);
    expect(r.renovaciones(), 1);
    expect(
      find.text(
        'Tu identidad fue verificada. Ya podés usar la billetera y los grupos.',
      ),
      findsOneWidget,
    );
  });

  testWidgets(
    'límite: mientras sigue en revisión, consulta en cada vuelta y no '
    'renueva',
    (tester) async {
      final r = await montar(
        tester,
        permisos: const [],
        responder: (a) => a.onGet(
          perfil,
          (s) => s.reply(200, cuenta('PENDIENTE_VERIFICACION')),
        ),
      );

      await tester.pump(intervalo);
      await asentar(tester);
      await tester.pump(intervalo);
      await asentar(tester);

      expect(r.consultas(), 2);
      expect(r.renovaciones(), 0);
    },
  );

  testWidgets('inválido: sin red no rompe nada y no renueva', (tester) async {
    final r = await montar(
      tester,
      permisos: const [],
      responder: (a) => a.onGet(
        perfil,
        (s) =>
            s.reply(500, {'codigo': 'AP-X', 'mensaje': 'no', 'trazaId': 't'}),
      ),
    );

    await tester.pump(intervalo);
    await asentar(tester);

    expect(r.consultas(), 1);
    expect(r.renovaciones(), 0);
    expect(find.text('Inicio'), findsOneWidget);
  });

  testWidgets('con la cuenta ya habilitada no consulta nada', (tester) async {
    final r = await montar(
      tester,
      permisos: const ['BILLETERA_VER'],
      responder: (a) => a.onGet(perfil, (s) => s.reply(200, cuenta('ACTIVO'))),
    );

    await tester.pump(intervalo * 3);
    await asentar(tester);

    expect(r.consultas(), 0);
    expect(r.renovaciones(), 0);
  });
}
