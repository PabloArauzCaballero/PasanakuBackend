import 'dart:convert';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/pasanaku/pantalla_mis_grupos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// 2026-10-19: con la identidad en revisión, la pestaña Grupos dice qué se puede
/// hacer y deja «Consultar estado» ahí mismo, en vez de mandar a una pantalla de
/// espera. Los permisos salen del token (sintético) y el servidor es un doble.
String _token({required bool habilitada}) {
  String parte(Object valor) =>
      base64Url.encode(utf8.encode(jsonEncode(valor))).replaceAll('=', '');
  return '${parte({'alg': 'none'})}.${parte({
    'sub': 'b8000000-0000-4000-8000-000000000088',
    'permisos': habilitada ? ['BILLETERA_VER'] : <String>[],
  })}.';
}

void main() {
  Future<void> montar(
    WidgetTester tester, {
    required bool habilitada,
    required String estadoDeCuenta,
  }) async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador
      ..onGet('/grupos/participaciones', (s) => s.reply(200, <Object>[]))
      ..onGet(
        '/usuarios/yo/perfil',
        (s) => s.reply(200, {
          'usuarioId': 'b8000000-0000-4000-8000-000000000088',
          'correo': 'rosa@example.com',
          'correoVerificado': true,
          'estadoDeCuenta': estadoDeCuenta,
        }),
      );
    final sesion = Sesion(AlmacenEnMemoria());
    await sesion.abrir(acceso: _token(habilitada: habilitada));
    addTearDown(sesion.dispose);
    await tester.pumpWidget(
      ProviderScope(
        retry: (_, _) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
          sesionProvider.overrideWithValue(sesion),
        ],
        child: MaterialApp(
          theme: temaDesde(Tokens.claro, Brightness.light),
          home: const PantallaMisGrupos(),
        ),
      ),
    );
    await asentar(tester);
  }

  testWidgets('correcto: en revisión, la pestaña lo dice y deja consultar', (
    tester,
  ) async {
    await montar(
      tester,
      habilitada: false,
      estadoDeCuenta: 'PENDIENTE_VERIFICACION',
    );
    expect(find.text('Estamos verificando tu identidad'), findsOneWidget);
    expect(find.text('Consultar estado'), findsOneWidget);
    // Los grupos siguen a la vista.
    expect(find.text('Organizar un grupo nuevo'), findsOneWidget);
  });

  testWidgets('límite: con la cuenta habilitada no hay aviso', (tester) async {
    await montar(tester, habilitada: true, estadoDeCuenta: 'ACTIVO');
    expect(find.text('Consultar estado'), findsNothing);
  });

  testWidgets('inválido: sin carnet enviado no promete una revisión', (
    tester,
  ) async {
    await montar(tester, habilitada: false, estadoDeCuenta: 'INCOMPLETO_KYC');
    expect(find.text('Estamos verificando tu identidad'), findsNothing);
    expect(find.text('Consultar estado'), findsNothing);
  });
}
