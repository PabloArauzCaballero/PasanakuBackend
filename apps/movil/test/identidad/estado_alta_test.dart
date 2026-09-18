import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
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
  test('el alta avanza y retrocede por sus pasos, en orden', () {
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
      c.read(altaProvider.notifier).elegirContrasena('una-clave-larga-2026');
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
      // Los tres consentimientos viajan por separado: aceptar el contrato no es lo
      // mismo que aceptar el tarifario, y cuál se dio tiene que poder auditarse.
      for (final v in vigentes) {
        expect(cuerpo, contains(v['id']));
      }
      // Y la contraseña va en la misma petición: sin ella la cuenta nace sin
      // credencial y no se puede entrar.
      expect(cuerpo, contains('una-clave-larga-2026'));
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
