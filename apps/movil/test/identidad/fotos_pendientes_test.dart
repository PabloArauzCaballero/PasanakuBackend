import 'dart:io';

import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_contrato.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/notifier_alta.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http_mock_adapter/http_mock_adapter.dart';

import '../comun.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'un 500 al subir la foto deja la cuenta creada y avisa el pendiente',
    () async {
      final carpeta = await Directory.systemTemp.createTemp(
        'alta-foto-prueba-',
      );
      addTearDown(() => carpeta.delete(recursive: true));
      final foto = File('${carpeta.path}/anverso.jpg');
      await foto.writeAsBytes(const [1, 2, 3]);

      final (:dio, :adaptador) = dioSimulado();
      final respuesta =
          ejemplo('identidad', 'registrarUsuario', 'ok')['cuerpo']
              as Map<String, dynamic>;
      adaptador.onPost(
        '/usuarios',
        (s) => s.reply(202, respuesta),
        data: Matchers.any,
      );
      adaptador.onPost(
        '/usuarios/${respuesta['usuarioId']}/documentos',
        (s) => s.reply(500, {'codigo': 'AP-CU02-99'}),
        data: Matchers.any,
      );
      final contenedor = ProviderContainer(
        retry: (retryCount, error) => null,
        overrides: [
          almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
          dioProvider.overrideWithValue(dio),
          altaProvider.overrideWith(AltaVerificadaSoloEnPrueba.new),
        ],
      );
      addTearDown(contenedor.dispose);

      final notifier = contenedor.read(altaProvider.notifier);
      notifier.actualizarDatos(
        DatosPersonales(
          nombres: 'Rosa',
          apellidos: 'Quispe',
          fechaNacimiento: DateTime.utc(1990, 5, 20),
          telefono: '+59178123456',
          numeroDocumento: '1234567',
        ),
      );
      notifier.capturarAnverso(foto.path);
      (notifier as AltaVerificadaSoloEnPrueba).simularVerificacionDelServidor();
      contenedor.read(contratoProvider.notifier)
        ..fijarVigentes([
          for (final v in contratosVigentesDePrueba())
            ContratoVigente.fromJson(v),
        ])
        ..alternarContrato(true)
        ..alternarTarifario(true)
        ..alternarTratamientoDatos(true);

      final billetera = await notifier.enviarAlServidor();

      expect(billetera, isNotNull);
      expect(contenedor.read(altaProvider).error, isNull);
      expect(contenedor.read(altaProvider).fotosPendientes, isTrue);
      expect(await foto.exists(), isFalse, reason: 'se borra la copia local');
    },
  );
}
