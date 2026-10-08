import 'dart:async';
import 'dart:convert';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/cu10_recargar_saldo.dart';
import 'package:aportaya_movil/dominio/cu11_retirar_saldo.dart';
import 'package:aportaya_movil/dominio/cu12_transferir_saldo.dart';
import 'package:aportaya_movil/dominio/error_de_operacion_de_dinero.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/pantallas/pasanaku/dominio/cu21_cobrar_aporte.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const _cuenta = '11111111-1111-4111-8111-111111111111';
const _obligacion = '22222222-2222-4222-8222-222222222222';

String _registro(String operacion) => switch (operacion) {
  'recarga' => 'idempotencia.cu10-recarga-$_cuenta',
  'retiro' => 'idempotencia.cu11-retiro-$_cuenta',
  'transferencia' => 'idempotencia.cu12-transferencia-$_cuenta',
  _ => 'idempotencia.cu21.$_obligacion',
};

class _AlmacenConPausa implements AlmacenSeguro {
  final base = AlmacenEnMemoria();
  final guardado = Completer<void>();
  final continuar = Completer<void>();

  @override
  Future<String?> leer(String clave) => base.leer(clave);

  @override
  Future<void> guardar(String clave, String valor) async {
    await base.guardar(clave, valor);
    guardado.complete();
    await continuar.future;
  }

  @override
  Future<void> borrar(String clave) => base.borrar(clave);
}

Object? _error(ProviderContainer contenedor, String operacion) =>
    switch (operacion) {
      'recarga' => contenedor.read(recargarSaldoProvider).error,
      'retiro' => contenedor.read(retirarSaldoProvider).error,
      'transferencia' => contenedor.read(transferirSaldoProvider).error,
      _ => contenedor.read(cobrarAporteProvider(_obligacion)).error,
    };

Future<void> _enviar(
  ProviderContainer contenedor,
  String operacion, {
  bool inicializar = true,
}) async {
  switch (operacion) {
    case 'recarga':
      if (inicializar) await contenedor.read(recargarSaldoProvider.future);
      await contenedor
          .read(recargarSaldoProvider.notifier)
          .enviar(cuentaId: _cuenta, monto: '10.00', medio: 'QR');
    case 'retiro':
      if (inicializar) await contenedor.read(retirarSaldoProvider.future);
      await contenedor
          .read(retirarSaldoProvider.notifier)
          .enviar(
            cuentaId: _cuenta,
            monto: '10.00',
            instrumentoDestinoId: _obligacion,
            factorMfa: '123456',
          );
    case 'transferencia':
      if (inicializar) await contenedor.read(transferirSaldoProvider.future);
      await contenedor
          .read(transferirSaldoProvider.notifier)
          .enviar(
            cuentaOrigenId: _cuenta,
            monto: '10.00',
            tipoDestino: EntradaTransferenciaDestinoTipoEnum.ALIAS,
            valorDestino: 'destinatario',
            concepto: 'aporte',
          );
    case 'aporte':
      if (inicializar) {
        await contenedor.read(cobrarAporteProvider(_obligacion).future);
      }
      await contenedor
          .read(cobrarAporteProvider(_obligacion).notifier)
          .enviar(
            monto: '10.00',
            canal: 'BILLETERA_MOVIL',
            referenciaProveedor: 'ref-sintetica',
          );
  }
}

void main() {
  test('la sonda de prueba entrega red confirmada', () async {
    final contenedor = ProviderContainer(
      overrides: [
        conectividadProvider.overrideWithValue(const ConexionDePruebaEnLinea()),
      ],
    );
    addTearDown(contenedor.dispose);
    contenedor.listen(hayConexionProvider, (_, _) {});
    expect(
      await contenedor
          .read(hayConexionProvider.future)
          .timeout(const Duration(seconds: 3)),
      isTrue,
    );
  });

  for (final operacion in ['recarga', 'retiro', 'transferencia', 'aporte']) {
    test('$operacion no hace POST si la sonda terminó con error', () async {
      final simulado = dioSimulado();
      var posts = 0;
      simulado.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST') posts++;
            handler.next(options);
          },
        ),
      );
      final almacen = AlmacenEnMemoria();
      final contenedor = ProviderContainer(
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          dioProvider.overrideWithValue(simulado.dio),
          hayConexionProvider.overrideWith((ref) async* {
            yield* Stream<bool>.error(StateError('sonda de prueba'));
          }),
        ],
      );
      addTearDown(contenedor.dispose);
      contenedor.listen(hayConexionProvider, (_, _) {});
      await expectLater(
        contenedor.read(hayConexionProvider.future),
        throwsA(isA<StateError>()),
      );

      await _enviar(contenedor, operacion);

      expect(posts, 0);
      expect(_error(contenedor, operacion), isA<ErrorDeConexionNoConfirmada>());
      expect(await almacen.leer(_registro(operacion)), isNull);
    });

    test(
      '$operacion no hace POST si la primera sonda sigue pendiente',
      () async {
        final sonda = Completer<bool>();
        final simulado = dioSimulado();
        var posts = 0;
        simulado.dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) {
              if (options.method == 'POST') posts++;
              handler.next(options);
            },
          ),
        );
        final contenedor = ProviderContainer(
          overrides: [
            almacenSeguroProvider.overrideWithValue(AlmacenEnMemoria()),
            dioProvider.overrideWithValue(simulado.dio),
            hayConexionProvider.overrideWith((ref) async* {
              yield await sonda.future;
            }),
          ],
        );
        addTearDown(contenedor.dispose);
        addTearDown(() {
          if (!sonda.isCompleted) sonda.complete(false);
        });

        await _enviar(contenedor, operacion);

        expect(posts, 0);
        expect(
          _error(contenedor, operacion),
          isA<ErrorDeConexionNoConfirmada>(),
        );
      },
    );

    test('$operacion no hace POST si cae la red al guardar la clave', () async {
      final almacen = _AlmacenConPausa();
      final cambios = StreamController<bool>.broadcast();
      final simulado = dioSimulado();
      final clavesEnviadas = <String?>[];
      simulado.dio.interceptors.add(
        InterceptorsWrapper(
          onRequest: (options, handler) {
            if (options.method == 'POST') {
              clavesEnviadas.add(options.headers['Idempotency-Key'] as String?);
            }
            handler.next(options);
          },
        ),
      );
      final contenedor = ProviderContainer(
        overrides: [
          almacenSeguroProvider.overrideWithValue(almacen),
          dioProvider.overrideWithValue(simulado.dio),
          hayConexionProvider.overrideWith((ref) async* {
            yield true;
            yield* cambios.stream;
          }),
        ],
      );
      addTearDown(contenedor.dispose);
      addTearDown(cambios.close);
      contenedor.listen(hayConexionProvider, (_, _) {});
      expect(await contenedor.read(hayConexionProvider.future), isTrue);

      final envio = _enviar(contenedor, operacion);
      await almacen.guardado.future.timeout(const Duration(seconds: 3));
      final desconectado = Completer<void>();
      contenedor.listen(hayConexionProvider, (_, siguiente) {
        if (siguiente.value == false && !desconectado.isCompleted) {
          desconectado.complete();
        }
      });
      cambios.add(false);
      await desconectado.future.timeout(const Duration(seconds: 3));
      almacen.continuar.complete();
      await envio;

      expect(clavesEnviadas, isEmpty);
      final error = _error(contenedor, operacion);
      expect(error, isA<ErrorDeConexionNoConfirmada>());
      expect((error as ErrorDeConexionNoConfirmada).sinConexion, isTrue);
      expect(error.mensaje, contains('No enviamos'));

      final registro =
          jsonDecode((await almacen.leer(_registro(operacion)))!)
              as Map<String, dynamic>;
      final clavePersistida = registro['clave'] as String;
      final recuperado = Completer<void>();
      contenedor.listen(hayConexionProvider, (_, siguiente) {
        if (siguiente.permiteOperar && !recuperado.isCompleted) {
          recuperado.complete();
        }
      });
      cambios.add(true);
      await recuperado.future.timeout(const Duration(seconds: 3));
      await _enviar(contenedor, operacion, inicializar: false);
      expect(clavesEnviadas, [clavePersistida]);
    });
  }
}
