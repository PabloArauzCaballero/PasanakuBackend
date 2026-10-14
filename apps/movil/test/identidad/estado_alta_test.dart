import 'dart:convert';

import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  test('el alta avanza y retrocede por sus pasos, en orden', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    final notifier = contenedor.read(altaProvider.notifier);

    expect(contenedor.read(altaProvider).paso, PasoAlta.cuenta);
    for (var i = 0; i < PasoAlta.values.length - 1; i++) {
      notifier.siguiente();
    }
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);
    notifier.siguiente(); // no hay noveno paso: se queda
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);

    notifier.atras();
    expect(contenedor.read(altaProvider).paso, PasoAlta.actividad);
  });

  test(
    'el alta manda todo junto a POST /usuarios, con los contratos aceptados',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      late String cuerpo;
      final claveMaxima = List.filled(128, 'a').join();
      final apellidoMaximo = 'Quispe Mamani${List.filled(47, 'Á').join()}';
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(
          202,
          ejemplo('identidad', 'registrarUsuario', 'ok')['cuerpo'],
        ),
        data: Matchers.any,
      );
      adaptador.onPost(
        '/usuarios/verificaciones/correo',
        (s) => s.reply(202, {
          'verificacionId': 'dddddddd-0000-4000-8000-000000000003',
          'destinoEnmascarado': 'ro***@example.com',
          'expiraEn': '2026-10-07T20:10:00Z',
        }),
        data: Matchers.any,
      );
      adaptador.onPost(
        '/usuarios/verificaciones/correo/dddddddd-0000-4000-8000-000000000003/confirmacion',
        (s) => s.reply(200, {'verificada': true}),
        data: Matchers.any,
      );
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (o, h) {
            if (o.path.contains('usuarios')) cuerpo = '${o.data}';
            h.next(o);
          },
        ),
      );
      final c = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(c.dispose);

      c
          .read(altaProvider.notifier)
          .actualizarDatos(
            DatosPersonales(
              nombres: 'Él',
              apellidos: apellidoMaximo,
              fechaNacimiento: DateTime.utc(1990, 5, 20),
              telefono: '+59178123456',
              numeroDocumento: '1234567',
              correo: 'rosa@example.com',
            ),
          );
      c.read(altaProvider.notifier).elegirContrasena(claveMaxima);
      expect(
        await c.read(altaProvider.notifier).solicitarCodigoCorreo(),
        isTrue,
      );
      await c.read(altaProvider.notifier).confirmarCodigoCorreo('482019');
      expect(c.read(altaProvider).codigoConfirmado, isTrue);
      // Los ids salen de `GET /cumplimiento/contratos/vigentes`: son UUID, uno por
      // entorno. Antes esto mandaba los códigos 'ADHESION' y 'TARIFARIO', inventados
      // en la app, y el servidor los rechazaba al deserializar.
      final vigentes = contratosVigentesDePrueba();
      c.read(contratoProvider.notifier)
        ..fijarVigentes([for (final v in vigentes) ContratoVigente.fromJson(v)])
        ..alternarContrato(true)
        ..alternarTarifario(true)
        ..alternarTratamientoDatos(true);
      for (var i = 0; i < PasoAlta.values.length - 1; i++) {
        c.read(altaProvider.notifier).siguiente();
      }

      final resultado = await c.read(altaProvider.notifier).enviarAlServidor();

      expect(resultado.error, isNull);
      expect(
        resultado.usuarioId,
        isNotNull,
        reason: 'se puede subir el expediente',
      );
      expect(
        resultado.cuentaBilleteraId,
        isNotNull,
        reason: 'el alta abre la billetera',
      );
      // Los tres consentimientos viajan por separado: aceptar el contrato no es lo
      // mismo que aceptar el tarifario, y cuál se dio tiene que poder auditarse.
      for (final v in vigentes) {
        expect(cuerpo, contains(v['id']));
      }
      // Y la contraseña va en la misma petición: sin ella la cuenta nace sin
      // credencial y no se puede entrar.
      expect(cuerpo, contains('dddddddd-0000-4000-8000-000000000003'));
      expect(cuerpo, contains(claveMaxima));
      final body = jsonDecode(cuerpo) as Map<String, dynamic>;
      const permitidas = {
        'telefonoE164',
        'nombres',
        'apellidos',
        'fechaNacimiento',
        'contrasena',
        'correo',
        'verificacionCorreoId',
        'canalVerificacion',
        'documento',
        'aceptaContratos',
      };
      expect(
        body.keys,
        containsAll({
          'telefonoE164',
          'nombres',
          'apellidos',
          'fechaNacimiento',
          'contrasena',
          'correo',
          'verificacionCorreoId',
          'documento',
          'aceptaContratos',
        }),
      );
      expect(body.keys.toSet().difference(permitidas), isEmpty);
      expect(body['telefonoE164'], matches(RegExp(r'^\+591\d{8}$')));
      expect((body['nombres'] as String).length, 2);
      expect(body['nombres'], 'Él');
      expect((body['apellidos'] as String).length, 60);
      expect((body['apellidos'] as String), startsWith('Quispe Mamani'));
      expect((body['contrasena'] as String).length, 128);
      final documento = body['documento'] as Map<String, dynamic>;
      expect(
        documento.keys.toSet().difference({
          'tipo',
          'numero',
          'lugarExpedicion',
          'fechaExpiracion',
        }),
        isEmpty,
      );
      expect(documento['tipo'], anyOf('CI', 'CEX', 'PASAPORTE'));
      expect((documento['numero'] as String).length, inInclusiveRange(5, 20));
    },
  );

  test(
    'sin fecha de nacimiento no se manda nada y se dice qué falta',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(202, {}),
        data: Matchers.any,
      );
      final c = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(c.dispose);

      final r = await c.read(altaProvider.notifier).enviarAlServidor();

      expect(r.error, 'Falta tu fecha de nacimiento.');
      expect(r.usuarioId, isNull);
      expect(c.read(altaProvider).error, 'Falta tu fecha de nacimiento.');
    },
  );

  test(
    'el rechazo de contraseña devuelve el alta al campo corregible',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(422, {
          'codigo': 'AP-CU01-06',
          'mensaje': 'no usar texto del servidor',
          'trazaId': 'traza-prueba',
        }),
        data: Matchers.any,
      );
      adaptador.onPost(
        '/usuarios/verificaciones/correo',
        (s) => s.reply(202, {
          'verificacionId': 'dddddddd-0000-4000-8000-000000000003',
          'destinoEnmascarado': 'ro***@example.com',
          'expiraEn': '2026-10-07T20:10:00Z',
        }),
        data: Matchers.any,
      );
      final c = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(c.dispose);
      c
          .read(altaProvider.notifier)
          .actualizarDatos(
            DatosPersonales(
              nombres: 'Rosa',
              apellidos: 'Quispe',
              fechaNacimiento: DateTime.utc(1990, 5, 20),
              telefono: '+59178123456',
              numeroDocumento: '1234567',
              correo: 'rosa@example.com',
            ),
          );
      c.read(altaProvider.notifier).elegirContrasena('Clave123');
      await c.read(altaProvider.notifier).solicitarCodigoCorreo();
      c.read(altaProvider.notifier).confirmarCelular();
      final vigentes = contratosVigentesDePrueba();
      c.read(contratoProvider.notifier)
        ..fijarVigentes([for (final v in vigentes) ContratoVigente.fromJson(v)])
        ..alternarContrato(true)
        ..alternarTarifario(true)
        ..alternarTratamientoDatos(true);
      for (var i = 0; i < PasoAlta.values.length - 1; i++) {
        c.read(altaProvider.notifier).siguiente();
      }

      final resultado = await c.read(altaProvider.notifier).enviarAlServidor();

      expect(
        resultado.error,
        isNotNull,
        reason: 'el mock debe devolver un rechazo',
      );
      expect(
        resultado.campoError,
        CampoDeErrorApi.contrasena,
        reason: 'error recibido: ${resultado.error}',
      );
      expect(c.read(altaProvider).paso, PasoAlta.cuenta);
      expect(c.read(altaProvider).error, contains('Esa contraseña'));
      expect(c.read(altaProvider).trazaId, 'traza-prueba');
      expect(c.read(altaProvider).error, isNot(contains('no usar texto')));
    },
  );

  test('el rechazo de vencimiento devuelve el alta a documento', () async {
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/usuarios',
      (s) => s.reply(422, {
        'codigo': 'AP-CU01-07',
        'mensaje': 'documento vencido',
        'trazaId': 'traza-vencimiento',
      }),
      data: Matchers.any,
    );
    adaptador.onPost(
      '/usuarios/verificaciones/correo',
      (s) => s.reply(202, {
        'verificacionId': 'dddddddd-0000-4000-8000-000000000003',
        'destinoEnmascarado': 'ro***@example.com',
        'expiraEn': '2026-10-07T20:10:00Z',
      }),
      data: Matchers.any,
    );
    final c = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(c.dispose);
    c
        .read(altaProvider.notifier)
        .actualizarDatos(
          DatosPersonales(
            nombres: 'Rosa',
            apellidos: 'Quispe',
            fechaNacimiento: DateTime.utc(1990, 5, 20),
            telefono: '+59178123456',
            numeroDocumento: '1234567',
            lugarExpedicion: 'LP',
            fechaExpiracion: DateTime.utc(2030, 5, 20),
            correo: 'rosa@example.com',
          ),
        );
    c.read(altaProvider.notifier).elegirContrasena('Clave123');
    await c.read(altaProvider.notifier).solicitarCodigoCorreo();
    c.read(altaProvider.notifier).confirmarCelular();
    final vigentes = contratosVigentesDePrueba();
    c.read(contratoProvider.notifier)
      ..fijarVigentes([for (final v in vigentes) ContratoVigente.fromJson(v)])
      ..alternarContrato(true)
      ..alternarTarifario(true)
      ..alternarTratamientoDatos(true);
    for (var i = 0; i < PasoAlta.values.length - 1; i++) {
      c.read(altaProvider.notifier).siguiente();
    }

    final resultado = await c.read(altaProvider.notifier).enviarAlServidor();

    expect(resultado.campoError, CampoDeErrorApi.fechaExpiracion);
    expect(c.read(altaProvider).paso, PasoAlta.documento);
    expect(c.read(altaProvider).error, contains('vencido'));
    expect(c.read(altaProvider).trazaId, 'traza-vencimiento');
  });
}
