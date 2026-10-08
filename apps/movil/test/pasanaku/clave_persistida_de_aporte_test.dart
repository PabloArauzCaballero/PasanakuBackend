import 'dart:convert';

import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/clave_persistida_de_aporte.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/cu21_cobrar_aporte.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _obligacion = '68a98585-2854-4b71-80fd-5ef490fe60f6';
const _ruta = '/aportes/obligaciones/$_obligacion/pagos';
const _claveLocal = 'idempotencia.cu21.$_obligacion';

Future<ProviderContainer> _contenedor(Dio dio, AlmacenSeguro almacen) async {
  final contenedor = ProviderContainer(
    retry: (retryCount, error) => null,
    overrides: [
      almacenSeguroProvider.overrideWithValue(almacen),
      dioProvider.overrideWithValue(dio),
      conectividadProvider.overrideWithValue(const ConexionDePruebaEnLinea()),
    ],
  );
  contenedor.listen(hayConexionProvider, (_, _) {});
  await contenedor.read(cobrarAporteProvider(_obligacion).future);
  await contenedor.read(hayConexionProvider.future);
  return contenedor;
}

Future<void> _enviar(ProviderContainer contenedor) => contenedor
    .read(cobrarAporteProvider(_obligacion).notifier)
    .enviar(
      monto: '125.00',
      canal: 'BILLETERA_MOVIL',
      referenciaProveedor: 'REF-SINTETICA',
    );

void main() {
  test(
    'un cierre de proceso conserva la clave para reintentar el aporte',
    () async {
      final almacen = AlmacenEnMemoria();
      final (:dio, :adaptador) = dioSimulado();
      final clavesEnviadas = <String?>[];
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == _ruta) {
              clavesEnviadas.add(options.headers['Idempotency-Key'] as String?);
            }
            handler.next(options);
          },
        ),
      );
      adaptador.onPost(
        _ruta,
        (s) => s.reply(
          503,
          ejemplo('aportes', 'cobrarAporte', 'intermitente')['cuerpo'],
        ),
      );
      final primerProceso = await _contenedor(dio, almacen);
      await _enviar(primerProceso);
      expect(
        primerProceso.read(cobrarAporteProvider(_obligacion)).hasError,
        true,
      );
      final registroGuardado =
          jsonDecode((await almacen.leer(_claveLocal))!)
              as Map<String, dynamic>;
      final claveGuardada = registroGuardado['clave'] as String;
      expect(claveGuardada, isNotEmpty);
      expect(registroGuardado['huella'], isNotEmpty);
      expect(registroGuardado.toString(), isNot(contains('REF-SINTETICA')));
      primerProceso.dispose();
      adaptador.onPost(
        _ruta,
        (s) => s.reply(200, ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo']),
      );
      final segundoProceso = await _contenedor(dio, almacen);
      addTearDown(segundoProceso.dispose);
      await _enviar(segundoProceso);
      expect(clavesEnviadas, [claveGuardada, claveGuardada]);
      expect(
        segundoProceso.read(cobrarAporteProvider(_obligacion)).value,
        isNotNull,
      );
      expect(await almacen.leer(_claveLocal), isNull);
    },
  );

  test(
    'si falla el guardado seguro no sale ninguna petición de cobro',
    () async {
      final (:dio, :adaptador) = dioSimulado();
      var llamadas = 0;
      dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST' && options.path == _ruta) llamadas++;
            handler.next(options);
          },
        ),
      );
      adaptador.onPost(
        _ruta,
        (s) => s.reply(200, ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo']),
      );
      final almacen = _AlmacenQueNoGuarda();
      final contenedor = await _contenedor(dio, almacen);
      addTearDown(contenedor.dispose);
      await _enviar(contenedor);
      expect(almacen.intentos, 1);
      expect(llamadas, 0);
      expect(contenedor.read(cobrarAporteProvider(_obligacion)).hasError, true);
    },
  );

  test('si falla la limpieza local no se oculta el cobro confirmado', () async {
    final almacen = _AlmacenQueNoBorra();
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      _ruta,
      (s) => s.reply(200, ejemplo('aportes', 'cobrarAporte', 'ok')['cuerpo']),
    );
    final contenedor = await _contenedor(dio, almacen);
    addTearDown(contenedor.dispose);
    await _enviar(contenedor);
    final estado = contenedor.read(cobrarAporteProvider(_obligacion));
    expect(estado.hasError, false);
    expect(estado.value, isNotNull);
    expect(await almacen.leer(_claveLocal), isNotEmpty);
  });

  test('tras reiniciar, cambiar datos no envía otro aporte', () async {
    final almacen = AlmacenEnMemoria();
    final (:dio, :adaptador) = dioSimulado();
    var llamadas = 0;
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          if (options.method == 'POST' && options.path == _ruta) llamadas++;
          handler.next(options);
        },
      ),
    );
    adaptador.onPost(
      _ruta,
      (s) => s.reply(
        503,
        ejemplo('aportes', 'cobrarAporte', 'intermitente')['cuerpo'],
      ),
    );
    final primero = await _contenedor(dio, almacen);
    await _enviar(primero);
    primero.dispose();

    final segundo = await _contenedor(dio, almacen);
    addTearDown(segundo.dispose);
    await segundo
        .read(cobrarAporteProvider(_obligacion).notifier)
        .enviar(
          monto: '125.00',
          canal: 'TARJETA',
          referenciaProveedor: 'REF-DISTINTA',
        );

    expect(llamadas, 1);
    expect(
      segundo.read(cobrarAporteProvider(_obligacion)).error,
      isA<ErrorDeAportePendiente>(),
    );
    expect(await almacen.leer(_claveLocal), isNotNull);
  });
}

class _AlmacenQueNoGuarda implements AlmacenSeguro {
  int intentos = 0;

  @override
  Future<String?> leer(String clave) async => null;

  @override
  Future<void> guardar(String clave, String valor) async {
    intentos++;
    throw StateError('Almacén seguro no disponible en esta prueba');
  }

  @override
  Future<void> borrar(String clave) async {}
}

class _AlmacenQueNoBorra extends AlmacenEnMemoria {
  @override
  Future<void> borrar(String clave) async {
    throw StateError('No se pudo limpiar el almacén de esta prueba');
  }
}
