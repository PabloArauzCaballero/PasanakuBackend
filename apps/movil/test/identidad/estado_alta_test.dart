import 'package:aportaya_movil/pantallas/identidad/dominio/estado_alta.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('el alta avanza y retrocede por los ocho pasos, en orden', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    final notifier = contenedor.read(altaProvider.notifier);

    expect(contenedor.read(altaProvider).paso, PasoAlta.datos);
    for (var i = 0; i < PasoAlta.values.length - 1; i++) {
      notifier.siguiente();
    }
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);
    notifier.siguiente(); // no hay noveno paso: se queda
    expect(contenedor.read(altaProvider).paso, PasoAlta.contrato);

    notifier.atras();
    expect(contenedor.read(altaProvider).paso, PasoAlta.perfilTransaccional);
  });

  test('enviarAlServidor no simula una llamada: declara el hueco', () {
    final contenedor = ProviderContainer();
    addTearDown(contenedor.dispose);
    expect(
      () => contenedor.read(altaProvider.notifier).enviarAlServidor(),
      throwsUnimplementedError,
    );
  });
}
