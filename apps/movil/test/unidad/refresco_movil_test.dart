import 'package:aportaya_movil/pantallas/identidad/dominio/cu04_autenticar.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

void main() {
  test(
    'el ingreso guarda tokenRefresco y nunca usa sesionId como reemplazo',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      final salida = ejemplo('identidad', 'autenticar', 'vacio')['cuerpo'];
      adaptador.onPost(
        '/sesiones',
        (s) => s.reply(200, salida),
        data: Matchers.any,
      );
      final almacen = AlmacenEnMemoria();
      final contenedor = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(contenedor.dispose);

      await contenedor
          .read(autenticacionProvider)
          .conCredenciales(
            telefonoE164: '+59178123456',
            credencial: 'Secreta123',
          );

      final refresco = await contenedor.read(sesionProvider).tokenDeRefresco();
      expect(refresco, salida['tokenRefresco']);
      expect(refresco, isNot(salida['sesionId']));
    },
  );

  test(
    'si falta tokenRefresco no inventa uno y conserva solo la sesión de acceso',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      final salida = Map<String, dynamic>.from(
        ejemplo('identidad', 'autenticar', 'vacio')['cuerpo'] as Map,
      )..remove('tokenRefresco');
      adaptador.onPost(
        '/sesiones',
        (s) => s.reply(200, salida),
        data: Matchers.any,
      );
      final almacen = AlmacenEnMemoria();
      final contenedor = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          dioProvider.overrideWithValue(dio),
        ],
      );
      addTearDown(contenedor.dispose);

      await contenedor
          .read(autenticacionProvider)
          .conCredenciales(
            telefonoE164: '+59178123456',
            credencial: 'Secreta123',
          );

      expect(await contenedor.read(sesionProvider).tokenDeRefresco(), isNull);
      expect(
        await contenedor.read(sesionProvider).tokenDeAcceso(),
        salida['tokenAcceso'],
      );
      expect(contenedor.read(sesionProvider).estado, EstadoDeSesion.activa);
    },
  );
}
