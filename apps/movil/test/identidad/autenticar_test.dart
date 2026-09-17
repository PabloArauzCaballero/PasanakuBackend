import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu04_autenticar.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_sesion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// **CU-04 contra el contrato de verdad.**
///
/// La pantalla de ingreso avanzaba sin hablar con nadie: guardaba el teléfono en
/// memoria y navegaba a MFA, y el MFA aceptaba cualquier código. Se podía «entrar» a
/// AportaYa sin que existiera la cuenta. Estas pruebas fijan que ahora la sesión la
/// abre el servidor, y que sin token no hay sesión.
void main() {
  const ruta = '/sesiones';

  ProviderContainer contenedorCon(dynamic dio) => ProviderContainer(
    retry: (retryCount, error) => null,
    overrides: [
      almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
      dioProvider.overrideWithValue(dio),
    ],
  );

  test(
    'cuando el servidor pide segundo factor, no hay sesión todavía: el token no se '
    'guarda hasta que el factor pasa',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(200, ejemplo('identidad', 'autenticar', 'ok')['cuerpo']),
        data: Matchers.any,
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final r = await c
          .read(autenticacionProvider)
          .conCredenciales(
            telefonoE164: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(r, ResultadoDeAutenticacion.faltaSegundoFactor);
      expect(
        await c.read(sesionProvider).tokenDeAcceso(),
        isNull,
        reason: 'un segundo factor pendiente no es una sesión abierta',
      );
    },
  );

  test(
    'cuando no hace falta segundo factor, el token queda guardado',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) =>
            s.reply(200, ejemplo('identidad', 'autenticar', 'vacio')['cuerpo']),
        data: Matchers.any,
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final r = await c
          .read(autenticacionProvider)
          .conCredenciales(
            telefonoE164: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(r, ResultadoDeAutenticacion.entro);
      expect(await c.read(sesionProvider).tokenDeAcceso(), 'tokenAcceso');
    },
  );

  test('el segundo factor abre la sesión', () async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      ruta,
      (s) =>
          s.reply(200, ejemplo('identidad', 'autenticar', 'vacio')['cuerpo']),
      data: Matchers.any,
    );
    final c = contenedorCon(dio);
    addTearDown(c.dispose);

    final r = await c
        .read(autenticacionProvider)
        .conFactor(
          telefonoE164: '+59178123456',
          credencial: 'Secreta123',
          codigo: '000000',
        );

    expect(r, ResultadoDeAutenticacion.entro);
    expect(await c.read(sesionProvider).tokenDeAcceso(), 'tokenAcceso');
  });

  test(
    'un rechazo del servidor no abre sesión y se cuenta en voz de marca',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU04-01',
          'mensaje': 'mensaje',
          'trazaId': 't',
        }),
        data: Matchers.any,
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final n = c.read(sesionIdentidadProvider.notifier);
      n.actualizarCredenciales(
        telefono: '+59178123456',
        contrasena: 'Secreta123',
      );
      final avanzo = await n.enviarCredenciales();

      expect(avanzo, isFalse);
      expect(await c.read(sesionProvider).tokenDeAcceso(), isNull);
      expect(
        c.read(sesionIdentidadProvider).error,
        'El código no coincide. Revisalo e intentá de nuevo.',
        reason: 'la app no muestra el mensaje crudo del backend',
      );
      expect(c.read(sesionIdentidadProvider).enviando, isFalse);
    },
  );

  test('la huella de dispositivo es estable entre intentos', () async {
    final (:dio, :adaptador) = dioSimulado();
    final enviadas = <String>[];
    adaptador.onPost(
      ruta,
      (s) =>
          s.reply(200, ejemplo('identidad', 'autenticar', 'vacio')['cuerpo']),
      data: Matchers.any,
    );
    final c = contenedorCon(dio);
    addTearDown(c.dispose);
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          final cuerpo = o.data;
          if (cuerpo is String && cuerpo.contains('huellaDispositivo')) {
            enviadas.add(cuerpo);
          }
          h.next(o);
        },
      ),
    );

    final api = c.read(autenticacionProvider);
    await api.conCredenciales(
      telefonoE164: '+59178123456',
      credencial: 'Secreta123',
    );
    await api.conCredenciales(
      telefonoE164: '+59178123456',
      credencial: 'Secreta123',
    );

    expect(enviadas, hasLength(2));
    final huella = RegExp(r'"huellaDispositivo":"([^"]+)"');
    expect(
      huella.firstMatch(enviadas[0])!.group(1),
      huella.firstMatch(enviadas[1])!.group(1),
      reason:
          'una huella distinta por intento haría que el teléfono nunca sea de confianza',
    );
  });
}
