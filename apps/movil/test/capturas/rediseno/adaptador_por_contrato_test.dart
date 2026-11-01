import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'adaptador_por_contrato.dart';

/// El doble de las capturas, en los tres niveles de su contrato (regla 65).
void main() {
  Dio dioCon(AdaptadorPorContrato a) =>
      Dio(BaseOptions(baseUrl: 'https://pruebas.aportaya.bo/api/v1'))
        ..httpClientAdapter = a;
  const saldo = '/billetera/11111111-1111-4111-8111-111111111111/saldo';

  test('correcto: datos devuelve el ejemplo ok del contrato', () async {
    final r = await dioCon(
      AdaptadorPorContrato(EstadoDeCaptura.datos),
    ).get(saldo);
    expect(r.statusCode, 200);
    expect((r.data as Map)['disponible'], isNotNull);
  });

  test(
    'límite: vacío y peor caso salen de sus escenarios, con su código',
    () async {
      for (final estado in [EstadoDeCaptura.vacio, EstadoDeCaptura.peorCaso]) {
        final r = await dioCon(AdaptadorPorContrato(estado)).get(saldo);
        expect(r.statusCode, 200, reason: estado.name);
      }
    },
  );

  test('límite: cargando nunca responde', () async {
    final pedido = dioCon(
      AdaptadorPorContrato(EstadoDeCaptura.cargando),
    ).get(saldo);
    await expectLater(
      pedido.timeout(const Duration(milliseconds: 200)),
      throwsA(isA<TimeoutException>()),
    );
  });

  test('inválido: error responde 503 a todo GET', () async {
    await expectLater(
      dioCon(AdaptadorPorContrato(EstadoDeCaptura.error)).get(saldo),
      throwsA(
        isA<DioException>().having(
          (e) => e.response?.statusCode,
          'código',
          503,
        ),
      ),
    );
  });

  test('inválido: una ruta sin contrato da 404 y queda anotada', () async {
    final a = AdaptadorPorContrato(EstadoDeCaptura.datos);
    await expectLater(
      dioCon(a).get('/no-existe/en-ningun-contrato'),
      throwsA(
        isA<DioException>().having(
          (e) => e.response?.statusCode,
          'código',
          404,
        ),
      ),
    );
    expect(a.sinContrato, contains('GET /api/v1/no-existe/en-ningun-contrato'));
  });
}
