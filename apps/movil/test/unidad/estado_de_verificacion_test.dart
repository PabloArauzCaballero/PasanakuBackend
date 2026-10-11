import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_movil/dominio/cliente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_de_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/seguimiento_del_alta.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// Doble de `ConsultarVerificacion`: devuelve los estados de la lista en orden y,
/// cuando se acaba, repite el último. Cuenta cuántas veces la consultaron.
class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref, this._estados, {this.falla = false});
  final List<EstadoDeVerificacionEstadoEnum> _estados;
  final bool falla;
  int consultas = 0;

  /// El `Token-De-Alta` con el que llegó cada consulta, en orden.
  final tokens = <String?>[];

  @override
  Future<EstadoDeVerificacion> ejecutar(
    String usuarioId, {
    String? tokenDeAlta,
  }) async {
    consultas++;
    tokens.add(tokenDeAlta);
    if (falla) throw StateError('sin red');
    final i = consultas - 1 < _estados.length
        ? consultas - 1
        : _estados.length - 1;
    return EstadoDeVerificacion(
      verificacionId: 'v-1',
      estado: _estados[i],
      fotos: const [],
    );
  }
}

({ProviderContainer contenedor, _ConsultaFalsa falsa}) _armar(
  List<EstadoDeVerificacionEstadoEnum> estados, {
  bool falla = false,
}) {
  late _ConsultaFalsa falsa;
  final contenedor = ProviderContainer(
    overrides: [
      consultarVerificacionProvider.overrideWith(
        (ref) => falsa = _ConsultaFalsa(ref, estados, falla: falla),
      ),
    ],
  );
  addTearDown(contenedor.dispose);
  contenedor.read(consultarVerificacionProvider);
  // Mantener el provider vivo mientras dura el test, como lo hace la pantalla.
  contenedor.listen(verificacionProvider, (_, _) {});
  return (contenedor: contenedor, falsa: falsa);
}

const _enRevision = EstadoDeVerificacionEstadoEnum.EN_REVISION;
const _aprobada = EstadoDeVerificacionEstadoEnum.APROBADA;
const _rechazada = EstadoDeVerificacionEstadoEnum.RECHAZADA;

void main() {
  test('solo APROBADA y RECHAZADA son finales', () {
    expect(esEstadoFinal(_aprobada), isTrue);
    expect(esEstadoFinal(_rechazada), isTrue);
    expect(esEstadoFinal(_enRevision), isFalse);
    expect(esEstadoFinal(EstadoDeVerificacionEstadoEnum.PENDIENTE), isFalse);
  });

  testWidgets('sondea cada 2 s y se detiene apenas llega APROBADA', (
    tester,
  ) async {
    final (:contenedor, :falsa) = _armar([_enRevision, _enRevision, _aprobada]);
    contenedor.read(verificacionProvider.notifier).iniciar('u-1');
    await tester.pump();
    expect(falsa.consultas, 1);
    expect(contenedor.read(verificacionProvider).value?.estado, _enRevision);

    await tester.pump(const Duration(milliseconds: 1900));
    expect(falsa.consultas, 1, reason: 'no consulta antes de los 2 s');

    await tester.pump(const Duration(milliseconds: 200));
    expect(falsa.consultas, 2);
    await tester.pump(const Duration(seconds: 2));
    expect(falsa.consultas, 3);
    expect(contenedor.read(verificacionProvider).value?.estado, _aprobada);

    await tester.pump(const Duration(seconds: 10));
    expect(falsa.consultas, 3, reason: 'un estado final corta el sondeo');
  });

  testWidgets('RECHAZADA también corta el sondeo', (tester) async {
    final (:contenedor, :falsa) = _armar([_rechazada]);
    contenedor.read(verificacionProvider.notifier).iniciar('u-1');
    await tester.pump();
    await tester.pump(const Duration(seconds: 10));
    expect(falsa.consultas, 1);
  });

  testWidgets('no sondea para siempre: tope de 20 consultas', (tester) async {
    final (:contenedor, :falsa) = _armar([_enRevision]);
    contenedor.read(verificacionProvider.notifier).iniciar('u-1');
    await tester.pump();
    for (var i = 0; i < 30; i++) {
      await tester.pump(const Duration(seconds: 2));
    }
    expect(falsa.consultas, 20);
  });

  testWidgets('«Actualizar estado» a mano consulta de nuevo', (tester) async {
    final (:contenedor, :falsa) = _armar([_aprobada]);
    final notifier = contenedor.read(verificacionProvider.notifier)
      ..iniciar('u-1');
    await tester.pump();
    expect(falsa.consultas, 1);

    await notifier.actualizar();
    expect(falsa.consultas, 2);
  });

  testWidgets(
    'cada consulta lleva el Token-De-Alta de ESE alta; sin seguimiento no lleva ninguno',
    (tester) async {
      final (:contenedor, :falsa) = _armar([_enRevision, _aprobada]);
      contenedor
          .read(seguimientoDelAltaProvider.notifier)
          .fijar('u-1', const CapturasDelExpediente(), tokenDeAlta: 'tok-u-1');
      contenedor.read(verificacionProvider.notifier).iniciar('u-1');
      await tester.pump();
      await tester.pump(const Duration(seconds: 2));
      expect(falsa.tokens, ['tok-u-1', 'tok-u-1']);

      // Otro usuario en pantalla: el token de u-1 NO viaja a la consulta de u-2.
      contenedor.read(verificacionProvider.notifier).iniciar('u-2');
      await tester.pump();
      expect(falsa.tokens.last, isNull);
    },
  );

  test('contra el cliente real: el Token-De-Alta viaja como cabecera', () async {
    final (:dio, :adaptador) = dioSimulado();
    const usuario = 'dddddddd-0000-4000-8000-000000000001';
    final cabeceras = <Map<String, dynamic>>[];
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          if (o.path.endsWith('/verificacion')) cabeceras.add(o.headers);
          h.next(o);
        },
      ),
    );
    adaptador.onGet(
      '/usuarios/$usuario/verificacion',
      (s) => s.reply(
        200,
        ejemplo('identidad', 'consultarEstadoDeVerificacion', 'ok')['cuerpo'],
      ),
      headers: {'Token-De-Alta': 'tok-real'},
    );
    final c = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [dioProvider.overrideWithValue(dio)],
    );
    addTearDown(c.dispose);

    final r = await c
        .read(consultarVerificacionProvider)
        .ejecutar(usuario, tokenDeAlta: 'tok-real');

    expect(r.estado, EstadoDeVerificacionEstadoEnum.PENDIENTE);
    expect(cabeceras.single['Token-De-Alta'], 'tok-real');
  });

  test('contra el cliente real: sin token no hay cabecera y el 403 llega como error', () async {
    final (:dio, :adaptador) = dioSimulado();
    const usuario = 'dddddddd-0000-4000-8000-000000000001';
    final cabeceras = <Map<String, dynamic>>[];
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (o, h) {
          cabeceras.add(o.headers);
          h.next(o);
        },
      ),
    );
    adaptador.onGet(
      '/usuarios/$usuario/verificacion',
      (s) => s.reply(403, {
        'codigo': 'AP-SEG-01',
        'mensaje': 'No tenés permiso para esta acción',
        'trazaId': 't-1',
      }),
    );
    final c = ProviderContainer(
      retry: (retryCount, error) => null,
      overrides: [dioProvider.overrideWithValue(dio)],
    );
    addTearDown(c.dispose);

    await expectLater(
      c.read(consultarVerificacionProvider).ejecutar(usuario),
      throwsA(isA<DioException>()),
    );
    expect(cabeceras.single.containsKey('Token-De-Alta'), isFalse);
  });

  testWidgets('un fallo de red queda como error y no reprograma', (
    tester,
  ) async {
    final (:contenedor, :falsa) = _armar([_enRevision], falla: true);
    contenedor.read(verificacionProvider.notifier).iniciar('u-1');
    await tester.pump();
    expect(contenedor.read(verificacionProvider).hasError, isTrue);

    await tester.pump(const Duration(seconds: 10));
    expect(falsa.consultas, 1);
  });
}
