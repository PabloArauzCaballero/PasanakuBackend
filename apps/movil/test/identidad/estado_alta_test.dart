import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

class _AlmacenAltaEnPrueba implements AlmacenSeguro {
  final datos = <String, String>{};

  @override
  Future<String?> leer(String clave) async => datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      datos[clave] = valor;

  @override
  Future<void> borrar(String clave) async => datos.remove(clave);
}

void main() {
  testWidgets('persiste el avance al ir a segundo plano sin esperar debounce', (
    tester,
  ) async {
    final almacen = _AlmacenAltaEnPrueba();
    final contenedor = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
    );
    addTearDown(contenedor.dispose);

    contenedor
        .read(altaProvider.notifier)
        .actualizarDatos(
          const DatosPersonales(nombres: 'Rosa', apellidos: 'Quispe'),
        );
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    await tester.pump(const Duration(milliseconds: 20));

    expect(await almacen.leer('alta.borrador.v1'), contains('Rosa'));
  });

  test('el alta se detiene antes de avanzar sin verificar el contacto', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    final notifier = contenedor.read(altaProvider.notifier);

    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    notifier.siguiente();
    notifier.siguiente();
    expect(contenedor.read(altaProvider).paso, PasoAlta.celular);
    notifier.siguiente(); // no hay verificación OTP contractual
    expect(contenedor.read(altaProvider).paso, PasoAlta.celular);

    notifier.atras();
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrasena);
  });

  test('tras verificación simulada, serializa POST /usuarios con contratos', () async {
    final (:dio, :adaptador) = dioSimulado();
    final almacen = _AlmacenAltaEnPrueba();
    await almacen.guardar('alta.borrador.v1', 'borrador-previo');
    late String cuerpo;
    adaptador.onPost(
      '/usuarios',
      (s) => s.reply(
        202,
        ejemplo('identidad', 'registrarUsuario', 'ok')['cuerpo'],
      ),
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
        almacenSeguroProvider.overrideWithValue(almacen),
        dioProvider.overrideWithValue(dio),
        altaProvider.overrideWith(AltaVerificadaSoloEnPrueba.new),
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
          ),
        );
    c.read(altaProvider.notifier).elegirContrasena('una-clave-larga-2026');
    (c.read(altaProvider.notifier) as AltaVerificadaSoloEnPrueba)
        .simularVerificacionDelServidor();
    // Los ids salen de `GET /cumplimiento/contratos/vigentes`: son UUID, uno por
    // entorno. Antes esto mandaba los códigos 'ADHESION' y 'TARIFARIO', inventados
    // en la app, y el servidor los rechazaba al deserializar.
    final vigentes = contratosVigentesDePrueba();
    c.read(contratoProvider.notifier)
      ..fijarVigentes([for (final v in vigentes) ContratoVigente.fromJson(v)])
      ..alternarContrato(true)
      ..alternarTarifario(true)
      ..alternarTratamientoDatos(true);

    final billetera = await c.read(altaProvider.notifier).enviarAlServidor();

    expect(billetera, isNotNull, reason: 'el alta abre la billetera');
    expect(await almacen.leer('alta.borrador.v1'), isNull);
    // Los tres consentimientos viajan por separado: aceptar el contrato no es lo
    // mismo que aceptar el tarifario, y cuál se dio tiene que poder auditarse.
    for (final v in vigentes) {
      expect(cuerpo, contains(v['id']));
    }
    // Y la contraseña va en la misma petición: sin ella la cuenta nace sin
    // credencial y no se puede entrar.
    expect(cuerpo, contains('una-clave-larga-2026'));
  });

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
          altaProvider.overrideWith(AltaVerificadaSoloEnPrueba.new),
        ],
      );
      addTearDown(c.dispose);

      (c.read(altaProvider.notifier) as AltaVerificadaSoloEnPrueba)
          .simularVerificacionDelServidor();
      final r = await c.read(altaProvider.notifier).enviarAlServidor();

      expect(r, isNull);
      expect(c.read(altaProvider).error, 'Falta tu fecha de nacimiento.');
    },
  );
}
