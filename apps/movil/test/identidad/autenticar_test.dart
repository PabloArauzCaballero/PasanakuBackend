import 'dart:convert';

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
      late String cuerpo;
      adaptador.onPost(
        ruta,
        (s) => s.reply(200, ejemplo('identidad', 'autenticar', 'ok')['cuerpo']),
        data: Matchers.any,
      );
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (o, h) {
            if (o.path == ruta) cuerpo = '${o.data}';
            h.next(o);
          },
        ),
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final r = await c
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: '+59178123456',
            credencial: '12345678',
          );

      expect(r, ResultadoDeAutenticacion.faltaSegundoFactor);
      expect(
        await c.read(sesionProvider).tokenDeAcceso(),
        isNull,
        reason: 'un segundo factor pendiente no es una sesión abierta',
      );
      final body = jsonDecode(cuerpo) as Map<String, dynamic>;
      expect(body.keys, containsAll({'telefonoE164', 'huellaDispositivo'}));
      expect(
        body.keys.toSet().difference({
          'telefonoE164',
          'credencial',
          'huellaDispositivo',
          'plataforma',
          'factor',
        }),
        isEmpty,
      );
      expect(body['telefonoE164'], matches(RegExp(r'^\+591\d{8}$')));
      expect((body['credencial'] as String).length, 8);
      expect(
        (body['huellaDispositivo'] as String).length,
        lessThanOrEqualTo(128),
      );
      expect(body['plataforma'], anyOf('ANDROID', 'IOS'));
      await c
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: '+59178123456',
            credencial: List.filled(128, 'x').join(),
          );
      final bodyMaximo = jsonDecode(cuerpo) as Map<String, dynamic>;
      expect((bodyMaximo['credencial'] as String).length, 128);
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
            identificador: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(r, ResultadoDeAutenticacion.entro);
      expect(await c.read(sesionProvider).tokenDeAcceso(), 'tokenAcceso');
    },
  );

  test(
    'el servicio real pide el segundo factor con 422 AP-CU04-03: la app pasa al paso del código',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU04-03',
          'mensaje':
              'Confirma el codigo que te enviamos para terminar de entrar.',
          'detalle': <String, dynamic>{},
          'trazaId': 'traza-1',
        }),
        data: Matchers.any,
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final r = await c
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(r, ResultadoDeAutenticacion.faltaSegundoFactor);
      expect(await c.read(sesionProvider).tokenDeAcceso(), isNull);
    },
  );

  test(
    'confirma por separado cuando el correo de segundo factor fue aceptado',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU04-10',
          'mensaje': 'Enviamos el codigo al correo confirmado de tu cuenta.',
          'detalle': <String, dynamic>{},
          'trazaId': 'traza-correo-mfa',
        }),
        data: Matchers.any,
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      final r = await c
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(r, ResultadoDeAutenticacion.factorEnviadoPorCorreo);
      expect(await c.read(sesionProvider).tokenDeAcceso(), isNull);
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
          identificador: '+59178123456',
          credencial: 'Secreta123',
          codigo: '000000',
        );

    expect(r, ResultadoDeAutenticacion.entro);
    expect(await c.read(sesionProvider).tokenDeAcceso(), 'tokenAcceso');
  });

  test(
    'con correo, el cuerpo lleva `correo` y no `telefonoE164`: el contrato pide uno solo',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      late String cuerpo;
      adaptador.onPost(
        ruta,
        (s) =>
            s.reply(200, ejemplo('identidad', 'autenticar', 'vacio')['cuerpo']),
        data: Matchers.any,
      );
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (o, h) {
            if (o.path == ruta) cuerpo = '${o.data}';
            h.next(o);
          },
        ),
      );
      final c = contenedorCon(dio);
      addTearDown(c.dispose);

      await c
          .read(autenticacionProvider)
          .conCredenciales(
            identificador: 'ana@ejemplo.invalid',
            credencial: 'Secreta123',
          );

      final body = jsonDecode(cuerpo) as Map<String, dynamic>;
      expect(body['correo'], 'ana@ejemplo.invalid');
      expect(body.containsKey('telefonoE164'), isFalse);
    },
  );

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
        identificador: '+59178123456',
        contrasena: 'Secreta123',
      );
      final avanzo = await n.enviarCredenciales();

      expect(avanzo, isFalse);
      expect(await c.read(sesionProvider).tokenDeAcceso(), isNull);
      expect(
        c.read(sesionIdentidadProvider).error,
        // AP-CU04-01 es CREDENCIAL_INVALIDA (contrato): hablar de «código» con una
        // contraseña mal escrita mandaba a la persona a buscar un SMS que no existe.
        'El celular o la contraseña no coinciden.',
        reason: 'la app no muestra el mensaje crudo del backend',
      );
      expect(c.read(sesionIdentidadProvider).enviando, isFalse);
      expect(c.read(sesionIdentidadProvider).trazaId, 't');
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
      identificador: '+59178123456',
      credencial: 'Secreta123',
    );
    await api.conCredenciales(
      identificador: '+59178123456',
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
