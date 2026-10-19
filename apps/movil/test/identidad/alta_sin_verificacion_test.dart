import 'dart:convert';

import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/verificacion_diferida.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

/// Verificación de identidad opcional al crear la cuenta (2026-10-17): «Saltar por
/// ahora» en el paso del carnet, y la verificación después, desde adentro de la app.
void main() {
  const usuario = 'b8000000-0000-4000-8000-000000000077';

  ProviderContainer contenedor(Dio dio) {
    final c = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [
        almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(c.dispose);
    return c;
  }

  group('el orden de los pasos', () {
    test('correcto: saltar el carnet lleva a los datos y de ahí al contrato, '
        'sin la actividad', () async {
      final c = ProviderContainer();
      addTearDown(c.dispose);
      final alta = c.read(altaProvider.notifier)
        ..siguiente()
        ..siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.documento);

      await alta.saltarVerificacion();
      expect(c.read(altaProvider).paso, PasoAlta.datos);
      expect(c.read(altaProvider).sinVerificacion, isTrue);
      expect(c.read(altaProvider).totalDePasos, 4);

      alta.siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.contrato);
    });

    test('límite: volver desde los datos reencuentra el carnet y el registro '
        'vuelve a incluir la verificación', () async {
      final c = ProviderContainer();
      addTearDown(c.dispose);
      final alta = c.read(altaProvider.notifier)
        ..siguiente()
        ..siguiente();
      await alta.saltarVerificacion();

      alta.atras();
      expect(c.read(altaProvider).paso, PasoAlta.documento);
      expect(c.read(altaProvider).modo, ModoDelAlta.completo);
      alta.siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.datos);
      alta.siguiente();
      expect(
        c.read(altaProvider).paso,
        PasoAlta.actividad,
        reason: 'sin saltar, la actividad sigue en el registro',
      );
    });

    test('la verificación desde la app recorre carnet, actividad y envío', () {
      final c = ProviderContainer();
      addTearDown(c.dispose);
      final alta = c.read(altaProvider.notifier)..empezarSoloVerificacion();
      expect(c.read(altaProvider).paso, PasoAlta.documento);
      alta.siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.actividad);
      alta.siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.contrato);
      alta.siguiente();
      expect(c.read(altaProvider).paso, PasoAlta.contrato);
    });
  });

  test('correcto: quien saltó el carnet crea la cuenta sin documento y el '
      'estado INCOMPLETO_KYC se entiende', () async {
    final (:dio, :adaptador) = dioSimulado();
    late Map<String, dynamic> cuerpo;
    adaptador
      ..onPost(
        '/usuarios',
        (s) => s.reply(202, {
          'usuarioId': usuario,
          'estado': 'INCOMPLETO_KYC',
          'tokenDeAlta': 'token-sintetico',
        }),
        data: Matchers.any,
      )
      ..onPost(
        '/usuarios/verificaciones/correo',
        (s) => s.reply(202, {
          'verificacionId': 'dddddddd-0000-4000-8000-000000000003',
          'destinoEnmascarado': 'ro***@example.com',
          'expiraEn': '2026-10-07T20:10:00Z',
        }),
        data: Matchers.any,
      )
      ..onPost(
        '/usuarios/verificaciones/correo/dddddddd-0000-4000-8000-000000000003/confirmacion',
        (s) => s.reply(200, {'verificada': true}),
        data: Matchers.any,
      );
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          if (o.path.endsWith('/usuarios')) {
            cuerpo = jsonDecode('${o.data}') as Map<String, dynamic>;
          }
          h.next(o);
        },
      ),
    );
    final c = contenedor(dio);
    final alta = c.read(altaProvider.notifier)
      ..actualizarDatos(
        DatosPersonales(
          nombres: 'Rosa',
          apellidos: 'Quispe',
          fechaNacimiento: DateTime.utc(1990, 5, 20),
          telefono: '+59178123456',
          correo: 'rosa@example.com',
        ),
      )
      ..elegirContrasena('clave-de-prueba-2026');
    expect(await alta.solicitarCodigoCorreo(), isTrue);
    await alta.confirmarCodigoCorreo('482019');
    final vigentes = contratosVigentesDePrueba();
    c.read(contratoProvider.notifier)
      ..fijarVigentes([for (final v in vigentes) ContratoVigente.fromJson(v)])
      ..alternarContrato(true)
      ..alternarTarifario(true)
      ..alternarTratamientoDatos(true);
    alta
      ..siguiente()
      ..siguiente();
    await alta.saltarVerificacion();
    alta.siguiente();

    final resultado = await alta.enviarAlServidor();

    expect(resultado.error, isNull);
    expect(resultado.usuarioId, usuario);
    expect(cuerpo.containsKey('documento'), isFalse);
    expect(cuerpo['nombres'], 'Rosa');
    expect(cuerpo['fechaNacimiento'], '1990-05-20');
  });

  group('enviar el carnet desde la app', () {
    const ruta = '/usuarios/$usuario/verificacion';

    Future<void> enviar(Dio dio) => contenedor(dio)
        .read(iniciarVerificacionProvider)
        .ejecutar(
          usuarioId: usuario,
          tipo: 'CI',
          numero: '1234567',
          lugarExpedicion: 'SC',
          fechaExpiracion: DateTime.utc(2031, 1, 15),
        );

    test(
      'correcto: manda el documento del contrato a su verificación',
      () async {
        final (:dio, :adaptador) = dioSimulado();
        late Map<String, dynamic> cuerpo;
        adaptador.onPost(
          ruta,
          (s) => s.reply(202, {
            'verificacionId': 'abababab-0000-4000-8000-000000000001',
            'estado': 'EN_REVISION',
            'fotos': <Object>[],
          }),
          data: Matchers.any,
        );
        dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (o, h) {
              if (o.path.endsWith('/verificacion')) {
                cuerpo = Map<String, dynamic>.from(o.data as Map);
              }
              h.next(o);
            },
          ),
        );

        await enviar(dio);

        expect(cuerpo, {
          'tipo': 'CI',
          'numero': '1234567',
          'lugarExpedicion': 'SC',
          'fechaExpiracion': '2031-01-15',
        });
      },
    );

    test('límite: si la verificación ya estaba abierta (reintento), sigue '
        'sin error para subir las fotos', () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU01-12',
          'mensaje': 'Tu verificacion ya esta en curso.',
          'trazaId': 't-1',
        }),
        data: Matchers.any,
      );

      await expectLater(enviar(dio), completes);
    });

    test('inválido: un carnet de otra cuenta se informa como error', () async {
      final (:dio, :adaptador) = dioSimulado();
      adaptador.onPost(
        ruta,
        (s) => s.reply(422, {
          'codigo': 'AP-CU01-03',
          'mensaje': 'Ya hay una cuenta con ese documento.',
          'trazaId': 't-2',
          'detalle': {'campo': 'documento'},
        }),
        data: Matchers.any,
      );

      await expectLater(
        enviar(dio),
        throwsA(
          isA<ErrorDeApi>().having((e) => e.codigo, 'codigo', 'AP-CU01-03'),
        ),
      );
    });
  });

  test('el estado de la cuenta sale del perfil', () {
    expect(EstadoDeCuenta.desde('INCOMPLETO_KYC'), EstadoDeCuenta.sinVerificar);
    expect(
      EstadoDeCuenta.desde('PENDIENTE_VERIFICACION'),
      EstadoDeCuenta.enRevision,
    );
    expect(EstadoDeCuenta.desde('ACTIVO'), EstadoDeCuenta.verificada);
    expect(EstadoDeCuenta.desde('SUSPENDIDO'), EstadoDeCuenta.otro);
    expect(EstadoDeCuenta.desde(null), EstadoDeCuenta.otro);
  });
}
