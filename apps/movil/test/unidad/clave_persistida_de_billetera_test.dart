import 'dart:convert';

import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:aportaya_movil/dominio/clave_persistida_de_billetera.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/dominio/cu10_recargar_saldo.dart';
import 'package:aportaya_movil/dominio/cu11_retirar_saldo.dart';
import 'package:aportaya_movil/dominio/cu12_transferir_saldo.dart';
import 'package:aportaya_movil/dominio/error_de_operacion_de_dinero.dart';
import 'package:aportaya_movil/dominio/errores.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/proveedores/conexion.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

const cuenta = '11111111-1111-4111-8111-111111111111';

ProviderContainer contenedor(AlmacenSeguro almacen, Dio dio) {
  final scope = ProviderContainer(
    overrides: [
      almacenSeguroProvider.overrideWithValue(almacen),
      dioProvider.overrideWithValue(dio),
      conectividadProvider.overrideWithValue(const ConexionDePruebaEnLinea()),
    ],
  );
  scope.listen(hayConexionProvider, (_, _) {});
  return scope;
}

Future<void> enviar(
  ProviderContainer scope,
  String operacion, {
  String monto = '10.00',
  String otp = '123456',
}) async {
  await scope.read(hayConexionProvider.future);
  switch (operacion) {
    case 'recarga':
      await scope.read(recargarSaldoProvider.future);
      await scope
          .read(recargarSaldoProvider.notifier)
          .enviar(cuentaId: cuenta, monto: monto, medio: 'QR');
    case 'retiro':
      await scope.read(retirarSaldoProvider.future);
      await scope
          .read(retirarSaldoProvider.notifier)
          .enviar(
            cuentaId: cuenta,
            monto: monto,
            instrumentoDestinoId: '22222222-2222-4222-8222-222222222222',
            factorMfa: otp,
          );
    case 'transferencia':
      await scope.read(transferirSaldoProvider.future);
      await scope
          .read(transferirSaldoProvider.notifier)
          .enviar(
            cuentaOrigenId: cuenta,
            monto: monto,
            tipoDestino: EntradaTransferenciaDestinoTipoEnum.ALIAS,
            valorDestino: 'destinatario',
            concepto: 'aporte',
          );
  }
}

bool tieneError(ProviderContainer scope, String operacion) =>
    switch (operacion) {
      'recarga' => scope.read(recargarSaldoProvider).hasError,
      'retiro' => scope.read(retirarSaldoProvider).hasError,
      _ => scope.read(transferirSaldoProvider).hasError,
    };

Object? errorDe(ProviderContainer scope, String operacion) =>
    switch (operacion) {
      'recarga' => scope.read(recargarSaldoProvider).error,
      'retiro' => scope.read(retirarSaldoProvider).error,
      _ => scope.read(transferirSaldoProvider).error,
    };

void main() {
  for (final (operacion, ruta, nombreEjemplo) in [
    ('recarga', '/billetera/recargas', 'solicitarRecarga'),
    ('retiro', '/billetera/retiros', 'solicitarRetiro'),
    ('transferencia', '/billetera/transferencias', 'transferirSaldo'),
  ]) {
    test('$operacion: 503 y reinicio reutilizan la misma clave', () async {
      final almacen = AlmacenEnMemoria();
      final claves = <String?>[];
      Dio dioPara(String escenario) {
        final (:dio, :adaptador) = dioSimulado();
        dio.interceptors.add(
          InterceptorsWrapper(
            onRequest: (options, handler) {
              if (options.method == 'POST' && options.path == ruta) {
                claves.add(options.headers['Idempotency-Key'] as String?);
              }
              handler.next(options);
            },
          ),
        );
        adaptador.onPost(
          ruta,
          (s) => s.reply(
            escenario == 'ok' ? 201 : 503,
            ejemplo('nucleo-financiero', nombreEjemplo, escenario)['cuerpo'],
          ),
        );
        return dio;
      }

      final primero = contenedor(almacen, dioPara('intermitente'));
      await enviar(primero, operacion);
      expect(tieneError(primero, operacion), isTrue);
      final error = errorDe(primero, operacion);
      expect(error, isA<ErrorDeResultadoIncierto>());
      expect((error as ErrorDeResultadoIncierto).trazaId, 'traza-intermitente');
      expect(error.sinConexion, isFalse);
      expect(claves, hasLength(1));
      expect(claves.single, isNotNull);
      primero.dispose();

      final segundo = contenedor(almacen, dioPara('ok'));
      await enviar(segundo, operacion, otp: '654321');
      expect(
        tieneError(segundo, operacion),
        isFalse,
        reason:
            'recarga=${segundo.read(recargarSaldoProvider).error}, '
            'retiro=${segundo.read(retirarSaldoProvider).error}, '
            'transferencia=${segundo.read(transferirSaldoProvider).error}',
      );
      expect(claves, [claves.first, claves.first]);
      segundo.dispose();
    });
  }

  test(
    'timeout se presenta como resultado incierto, no como fallo confirmado',
    () {
      final request = RequestOptions(path: '/billetera/recargas');
      final error = errorDeOperacionDeDinero(
        DioException(
          requestOptions: request,
          type: DioExceptionType.connectionTimeout,
          error: ErrorDeRed(),
        ),
      );
      expect(error, isA<ErrorDeResultadoIncierto>());
      expect((error as ErrorDeResultadoIncierto).sinConexion, isFalse);
      expect(error.mensaje, contains('No pudimos confirmar el resultado'));
    },
  );

  test(
    'fallo de conexión sí usa la variante offline de resultado incierto',
    () {
      final error = errorDeOperacionDeDinero(
        DioException(
          requestOptions: RequestOptions(path: '/billetera/recargas'),
          type: DioExceptionType.connectionError,
          error: ErrorDeRed(),
        ),
      );
      expect(error, isA<ErrorDeResultadoIncierto>());
      expect((error as ErrorDeResultadoIncierto).sinConexion, isTrue);
    },
  );

  test('409 no ofrece un reintento que podría repetir dinero', () {
    final error = errorDeOperacionDeDinero(
      ErrorDeApi(codigo: 'AP-CU10-09', trazaId: 'traza-409', estado: 409),
    );
    expect(error, isA<ErrorDeOperacionPendiente>());
  });

  test('500 conserva la traza y no afirma que la operación falló', () {
    final error = errorDeOperacionDeDinero(
      ErrorDeApi(codigo: 'AP-DESCONOCIDO', trazaId: 'traza-500', estado: 500),
    );
    expect(error, isA<ErrorDeResultadoIncierto>());
    expect((error as ErrorDeResultadoIncierto).trazaId, 'traza-500');
    expect(error.mensaje, contains('No pudimos confirmar el resultado'));
  });

  test(
    'un intento con datos distintos no sobrescribe la clave pendiente',
    () async {
      final almacen = AlmacenEnMemoria();
      final dio = dioSimulado().dio;
      final primero = contenedor(almacen, dio);
      final clave = await primero
          .read(clavePersistidaDeBilleteraProvider)
          .abrir(formularioId: 'cu10-recarga-$cuenta', datos: ['10.00', 'QR']);
      primero.dispose();

      final segundo = contenedor(almacen, dio);
      final claves = segundo.read(clavePersistidaDeBilleteraProvider);
      await expectLater(
        claves.abrir(
          formularioId: 'cu10-recarga-$cuenta',
          datos: ['11.00', 'QR'],
        ),
        throwsA(isA<ErrorDeOperacionPendiente>()),
      );
      expect(
        await claves.abrir(
          formularioId: 'cu10-recarga-$cuenta',
          datos: ['10.00', 'QR'],
        ),
        clave,
      );
      segundo.dispose();
    },
  );

  test('registro corrupto falla cerrado y no genera clave nueva', () async {
    final almacen = AlmacenEnMemoria();
    await almacen.guardar('idempotencia.cu11-retiro-$cuenta', '{invalido');
    final scope = contenedor(almacen, dioSimulado().dio);
    await expectLater(
      scope
          .read(clavePersistidaDeBilleteraProvider)
          .abrir(
            formularioId: 'cu11-retiro-$cuenta',
            datos: ['10.00', 'destino'],
          ),
      throwsA(isA<ErrorDeOperacionPendiente>()),
    );
    expect(await almacen.leer('idempotencia.cu11-retiro-$cuenta'), '{invalido');
    scope.dispose();
  });

  test('el factor MFA no aparece en el registro persistido', () async {
    final almacen = AlmacenEnMemoria();
    final (:dio, :adaptador) = dioSimulado();
    adaptador.onPost(
      '/billetera/retiros',
      (s) => s.reply(
        503,
        ejemplo(
          'nucleo-financiero',
          'solicitarRetiro',
          'intermitente',
        )['cuerpo'],
      ),
    );
    final scope = contenedor(almacen, dio);
    await enviar(scope, 'retiro', otp: '987654');
    final registro = await almacen.leer('idempotencia.cu11-retiro-$cuenta');
    expect(registro, isNotNull);
    expect(registro, isNot(contains('987654')));
    expect(jsonDecode(registro!)['clave'], isNotEmpty);
    scope.dispose();
  });
}
