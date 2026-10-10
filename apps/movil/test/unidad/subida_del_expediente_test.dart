import 'dart:async';

import 'package:aportaya_movil/pantallas/identidad/dominio/capturas_del_expediente.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_subir_foto.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/subida_del_expediente.dart';
import 'package:aportaya_movil/proveedores/idempotencia.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Doble del puerto `SubidaDeFotos`: no toca la red, pero lee la clave de
/// idempotencia igual que el real (`idempotenciaProvider.claveDe(formularioId)`),
/// así el test ve qué clave viajaría en cada intento. Cada llamada queda pendiente
/// hasta que el test la resuelve, o se corta si cancelan su `CancelToken`.
class _SubidaFalsa extends SubidaDeFotos {
  _SubidaFalsa(this._ref) : super(_ref);
  final Ref _ref;

  final llamadas =
      <({CaraDelCarril cara, String clave, Completer<void> fin})>[];

  @override
  Future<FotoDelExpediente> subir({
    required String usuarioId,
    required CaraDelCarril cara,
    required String rutaLocal,
    required String formularioId,
    CancelToken? cancelToken,
  }) async {
    final fin = Completer<void>();
    llamadas.add((
      cara: cara,
      clave: _ref.read(idempotenciaProvider.notifier).claveDe(formularioId),
      fin: fin,
    ));
    final cortes = <Future<void>>[fin.future];
    if (cancelToken != null) {
      cortes.add(cancelToken.whenCancel.then((e) => throw e));
    }
    await Future.any(cortes);
    return const FotoDelExpediente(claveObjeto: 'k', hashArchivo: 'h');
  }
}

DioException _fallo503() {
  final pedido = RequestOptions(path: '/usuarios/u-1/documentos');
  return DioException(
    requestOptions: pedido,
    response: Response(requestOptions: pedido, statusCode: 503),
    type: DioExceptionType.badResponse,
  );
}

Captura _captura(CaraDelCarril cara) => Captura(
  ruta: '/tmp/${cara.name}.jpg',
  bytes: 102400,
  ancho: 1600,
  alto: 1009,
  sha256Corto: '9f2c1e4a3b7d',
);

CapturasDelExpediente _cinco() {
  var capturas = const CapturasDelExpediente();
  for (final cara in CapturasDelExpediente.orden) {
    capturas = capturas.conCaptura(cara, _captura(cara));
  }
  return capturas;
}

({ProviderContainer contenedor, _SubidaFalsa falsa}) _armar() {
  late _SubidaFalsa falsa;
  final contenedor = ProviderContainer(
    overrides: [
      subidaDeFotosProvider.overrideWith((ref) => falsa = _SubidaFalsa(ref)),
    ],
  );
  addTearDown(contenedor.dispose);
  contenedor.read(subidaDeFotosProvider);
  return (contenedor: contenedor, falsa: falsa);
}

EstadoDeUnaSubida? _estado(ProviderContainer c, CaraDelCarril cara) =>
    c.read(subidaProvider).porCara[cara]?.estado;

/// Deja correr las microtareas pendientes (los `await` del notifier).
Future<void> _vaciar() => Future<void>.delayed(Duration.zero);

void main() {
  test('sube las cinco en orden y termina con todas «subida»', () async {
    final (:contenedor, :falsa) = _armar();
    final subida = contenedor
        .read(subidaProvider.notifier)
        .subirPendientes(usuarioId: 'u-1', capturas: _cinco());

    for (var i = 0; i < CapturasDelExpediente.orden.length; i++) {
      await _vaciar();
      final cara = CapturasDelExpediente.orden[i];
      expect(falsa.llamadas[i].cara, cara);
      expect(_estado(contenedor, cara), EstadoDeUnaSubida.subiendo);
      expect(contenedor.read(subidaProvider).subiendoAlguna, isTrue);
      falsa.llamadas[i].fin.complete();
    }
    await subida;

    expect(contenedor.read(subidaProvider).completa, isTrue);
    expect(contenedor.read(subidaProvider).subiendoAlguna, isFalse);
  });

  test('sin capturas no sube nada (el bug de H5.S1.M1 se veía así)', () async {
    final (:contenedor, :falsa) = _armar();
    await contenedor
        .read(subidaProvider.notifier)
        .subirPendientes(
          usuarioId: 'u-1',
          capturas: const CapturasDelExpediente(),
        );
    expect(falsa.llamadas, isEmpty);
    expect(contenedor.read(subidaProvider).completa, isFalse);
  });

  test('una cara que falla queda «fallida» y no frena a las demás', () async {
    final (:contenedor, :falsa) = _armar();
    final subida = contenedor
        .read(subidaProvider.notifier)
        .subirPendientes(usuarioId: 'u-1', capturas: _cinco());

    await _vaciar();
    falsa.llamadas[0].fin.completeError(_fallo503());
    for (var i = 1; i < CapturasDelExpediente.orden.length; i++) {
      await _vaciar();
      falsa.llamadas[i].fin.complete();
    }
    await subida;

    final anverso = contenedor
        .read(subidaProvider)
        .porCara[CaraDelCarril.anverso]!;
    expect(anverso.estado, EstadoDeUnaSubida.fallida);
    expect(anverso.error, isNotEmpty);
    expect(
      _estado(contenedor, CaraDelCarril.perfilDerecho),
      EstadoDeUnaSubida.subida,
    );
    expect(contenedor.read(subidaProvider).completa, isFalse);
  });

  test(
    '«Reintentar» reusa la clave de idempotencia del intento fallido',
    () async {
      final (:contenedor, :falsa) = _armar();
      final notifier = contenedor.read(subidaProvider.notifier);

      final primera = notifier.reintentar(
        usuarioId: 'u-1',
        cara: CaraDelCarril.selfie,
        ruta: '/tmp/selfie.jpg',
      );
      await _vaciar();
      falsa.llamadas[0].fin.completeError(_fallo503());
      await primera;
      expect(
        _estado(contenedor, CaraDelCarril.selfie),
        EstadoDeUnaSubida.fallida,
      );

      final segunda = notifier.reintentar(
        usuarioId: 'u-1',
        cara: CaraDelCarril.selfie,
        ruta: '/tmp/selfie.jpg',
      );
      await _vaciar();
      falsa.llamadas[1].fin.complete();
      await segunda;

      expect(falsa.llamadas[1].clave, falsa.llamadas[0].clave);
      expect(
        _estado(contenedor, CaraDelCarril.selfie),
        EstadoDeUnaSubida.subida,
      );
    },
  );

  test('«Repetir la foto» rota la clave: es otra operación', () async {
    final (:contenedor, :falsa) = _armar();
    final notifier = contenedor.read(subidaProvider.notifier);

    final primera = notifier.reintentar(
      usuarioId: 'u-1',
      cara: CaraDelCarril.reverso,
      ruta: '/tmp/reverso.jpg',
    );
    await _vaciar();
    falsa.llamadas[0].fin.completeError(_fallo503());
    await primera;

    notifier.rotarClaveParaRepetir(CaraDelCarril.reverso);
    final segunda = notifier.reintentar(
      usuarioId: 'u-1',
      cara: CaraDelCarril.reverso,
      ruta: '/tmp/reverso-nueva.jpg',
    );
    await _vaciar();
    falsa.llamadas[1].fin.complete();
    await segunda;

    expect(falsa.llamadas[1].clave, isNot(falsa.llamadas[0].clave));
  });

  test(
    '«Cancelar la subida» corta la petición y la cara vuelve a pendiente',
    () async {
      final (:contenedor, :falsa) = _armar();
      final notifier = contenedor.read(subidaProvider.notifier);

      final subida = notifier.reintentar(
        usuarioId: 'u-1',
        cara: CaraDelCarril.anverso,
        ruta: '/tmp/anverso.jpg',
      );
      await _vaciar();
      expect(
        _estado(contenedor, CaraDelCarril.anverso),
        EstadoDeUnaSubida.subiendo,
      );

      notifier.cancelar(CaraDelCarril.anverso);
      await subida;

      expect(
        _estado(contenedor, CaraDelCarril.anverso),
        EstadoDeUnaSubida.pendiente,
      );
      expect(contenedor.read(subidaProvider).subiendoAlguna, isFalse);
    },
  );

  testWidgets('a los 10 s sin respuesta pasa a «lenta», y no antes', (
    tester,
  ) async {
    final (:contenedor, :falsa) = _armar();
    final subida = contenedor
        .read(subidaProvider.notifier)
        .reintentar(
          usuarioId: 'u-1',
          cara: CaraDelCarril.selfie,
          ruta: '/tmp/selfie.jpg',
        );
    await tester.pump();

    await tester.pump(const Duration(seconds: 9));
    expect(
      _estado(contenedor, CaraDelCarril.selfie),
      EstadoDeUnaSubida.subiendo,
    );

    await tester.pump(const Duration(seconds: 2));
    expect(_estado(contenedor, CaraDelCarril.selfie), EstadoDeUnaSubida.lenta);
    // «Lenta» sigue contando como en curso: «Continuar» no se habilita todavía.
    expect(contenedor.read(subidaProvider).subiendoAlguna, isTrue);

    falsa.llamadas.single.fin.complete();
    await tester.pump();
    await subida;
    expect(_estado(contenedor, CaraDelCarril.selfie), EstadoDeUnaSubida.subida);
  });
}
