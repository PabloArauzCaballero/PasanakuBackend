import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

void main() {
  test('el alta avanza y retrocede por los ocho pasos, en orden', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    final notifier = contenedor.read(altaProvider.notifier);

    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    for (var i = 0; i < PasoAlta.values.length - 1; i++) {
      notifier.siguiente();
    }
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);
    notifier.siguiente(); // no hay noveno paso: se queda
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);

    notifier.atras();
    expect(contenedor.read(altaProvider).paso, PasoAlta.perfilTransaccional);
  });

  test(
    'el alta manda todo junto a POST /usuarios, con los contratos aceptados',
    () async {
      final (:dio, :adaptador) = dioSimulado();
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
            ),
          );
      c.read(contratoProvider.notifier)
        ..alternarContrato(true)
        ..alternarTarifario(true)
        ..alternarTratamientoDatos(true);

      final billetera = await c.read(altaProvider.notifier).enviarAlServidor();

      expect(billetera, isNotNull, reason: 'el alta abre la billetera');
      // Los tres consentimientos viajan por separado: aceptar el contrato no es lo
      // mismo que aceptar el tarifario, y cuál se dio tiene que poder auditarse.
      expect(cuerpo, contains('ADHESION'));
      expect(cuerpo, contains('TARIFARIO'));
      expect(cuerpo, contains('TRATAMIENTO_DATOS'));
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

      expect(r, isNull);
      expect(c.read(altaProvider).error, 'Falta tu fecha de nacimiento.');
    },
  );
}
