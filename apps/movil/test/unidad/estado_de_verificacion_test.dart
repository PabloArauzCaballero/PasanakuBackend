import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/cu02_consultar_verificacion.dart';
import 'package:aportaya_movil/pantallas/identidad/dominio/estado_de_verificacion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Doble de `ConsultarVerificacion`: devuelve los estados de la lista en orden y,
/// cuando se acaba, repite el último. Cuenta cuántas veces la consultaron.
class _ConsultaFalsa extends ConsultarVerificacion {
  _ConsultaFalsa(super.ref, this._estados, {this.falla = false});
  final List<EstadoDeVerificacionEstadoEnum> _estados;
  final bool falla;
  int consultas = 0;

  @override
  Future<EstadoDeVerificacion> ejecutar(String usuarioId) async {
    consultas++;
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
