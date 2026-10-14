import 'dart:io';

import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/dominio/puertos/camara_en_vivo.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_subir_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/subida_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_camara.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_registro.dart';
import 'package:aportaya_movil/pantallas/identidad/pantalla_sesion.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

class _AlmacenEnMemoria implements AlmacenSeguro {
  final _datos = <String, String>{};

  @override
  Future<String?> leer(String clave) async => _datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async {
    _datos[clave] = valor;
  }

  @override
  Future<void> borrar(String clave) async {
    _datos.remove(clave);
  }
}

class _CamaraSintetica implements CamaraEnVivo {
  @override
  Future<EstadoDePermiso> pedirPermiso() async => EstadoDePermiso.concedido;

  @override
  Future<void> iniciar({
    required bool frontal,
    bool conDeteccion = false,
  }) async {}

  @override
  Widget vista() => const ColoredBox(
    color: Colors.black,
    child: Center(child: Text('VISTA SINTÉTICA')),
  );

  @override
  Future<String> tomarFoto() => throw StateError('La prueba no toma fotos.');

  @override
  Future<bool> escucharRostros(void Function(LecturaDeRostro?) alLeer) async =>
      false;

  @override
  Future<void> dejarDeEscuchar() async {}

  @override
  Future<void> liberar() async {}
}

class _SubidaSintetica extends SubidaDeFotos {
  _SubidaSintetica(super.ref);

  var llamadas = 0;

  @override
  Future<FotoDelExpediente> subir({
    required String usuarioId,
    required CaraDelCarril cara,
    required String rutaLocal,
    required String formularioId,
    CancelToken? cancelToken,
    String? tokenDeAlta,
  }) async {
    if (!await File(rutaLocal).exists()) {
      throw StateError('El archivo sintético no existía antes de subir.');
    }
    llamadas++;
    return const FotoDelExpediente(
      claveObjeto: 'objeto-sintetico',
      hashArchivo:
          '0000000000000000000000000000000000000000000000000000000000000000',
    );
  }
}

Widget _app(ProviderContainer container, Widget home) =>
    UncontrolledProviderScope(
      container: container,
      child: MaterialApp(
        theme: temaDesde(Tokens.claro, Brightness.light),
        home: home,
      ),
    );

Future<void> _esperarParaCaptura(WidgetTester tester) async {
  await tester.runAsync(
    () => Future<void>.delayed(const Duration(seconds: 12)),
  );
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('H10.S4.M3 protege ingreso, cuenta e identidad en Android', (
    tester,
  ) async {
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'));
    final container = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(() {
      container.dispose();
      dio.close(force: true);
    });

    await tester.pumpWidget(_app(container, const PantallaDeSesion()));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 300));
    debugPrint('H10_S4_M3_CAPTURE=LOGIN');
    await _esperarParaCaptura(tester);

    await tester.pumpWidget(_app(container, const PantallaDeRegistro()));
    await tester.pump();
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 400)),
    );
    debugPrint('H10_S4_M3_CAPTURE=ALTA_CUENTA');
    await _esperarParaCaptura(tester);

    final alta = container.read(altaProvider.notifier);
    alta
      ..siguiente()
      ..siguiente();
    await tester.pump();
    await tester.runAsync(
      () => Future<void>.delayed(const Duration(milliseconds: 400)),
    );
    expect(container.read(altaProvider).paso.name, 'documento');
    debugPrint('H10_S4_M3_CAPTURE=ALTA_IDENTIDAD');
    await _esperarParaCaptura(tester);

    Navigator.of(tester.element(find.byType(PantallaDeRegistro))).push<void>(
      MaterialPageRoute<void>(
        builder: (_) => PantallaDeCamara(
          cara: CaraDelCarril.anverso,
          camara: _CamaraSintetica(),
        ),
      ),
    );
    var camaraLista = false;
    for (var frame = 0; frame < 80; frame++) {
      await tester.pump(const Duration(milliseconds: 250));
      if (find.text('VISTA SINTÉTICA').evaluate().isNotEmpty) {
        camaraLista = true;
        break;
      }
    }
    expect(camaraLista, isTrue, reason: 'La pantalla de cámara no se mostró.');
    debugPrint('H10_S4_M3_CAPTURE=CAMARA');
    await _esperarParaCaptura(tester);
    await tester.tap(find.text('Cancelar'));
    await tester.pump(const Duration(seconds: 1));

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
  });

  test('H10.S4.M4 elimina las cinco fotos tras completar la subida', () async {
    final directorio = await Directory.systemTemp.createTemp('alta-sintetica-');
    late _SubidaSintetica falsa;
    final dio = Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'));
    final container = ProviderContainer(
      overrides: [
        almacenSeguroProvider.overrideWithValue(_AlmacenEnMemoria()),
        dioProvider.overrideWithValue(dio),
        subidaDeFotosProvider.overrideWith(
          (ref) => falsa = _SubidaSintetica(ref),
        ),
      ],
    );
    addTearDown(() async {
      container.dispose();
      dio.close(force: true);
      if (await directorio.exists()) await directorio.delete(recursive: true);
    });

    var capturas = const CapturasDelExpediente();
    for (final cara in CapturasDelExpediente.orden) {
      final archivo = File('${directorio.path}/${cara.name}.jpg');
      await archivo.writeAsBytes(const [83, 89, 78, 84, 72, 69, 84, 73, 67]);
      capturas = capturas.conCaptura(
        cara,
        Captura(
          ruta: archivo.path,
          bytes: 9,
          ancho: 12,
          alto: 8,
          sha256Corto: 'sintetico',
        ),
      );
    }
    final antes = await directorio.list().length;
    expect(antes, 5);

    await container
        .read(subidaProvider.notifier)
        .subirPendientes(usuarioId: 'cuenta-sintetica', capturas: capturas);

    final despues = await directorio.list().length;
    expect(falsa.llamadas, 5);
    expect(container.read(subidaProvider).completa, isTrue);
    expect(despues, 0);
    debugPrint('H10_S4_M4_FILES_BEFORE=$antes AFTER=$despues');
  });
}
