import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

/// Simula el almacén seguro con un mapa en memoria **fuera** del `ProviderScope`:
/// así se comprueba que la sesión sobrevive a "cerrar y reabrir" (un `ProviderScope`
/// nuevo, como pasa cuando el proceso de la app vuelve a arrancar), porque lo que
/// persiste de verdad es el almacén, no el estado de Riverpod.
class _AlmacenEnMemoria implements AlmacenSeguro {
  final Map<String, String> _datos = {};

  @override
  Future<String?> leer(String clave) async => _datos[clave];

  @override
  Future<void> guardar(String clave, String valor) async =>
      _datos[clave] = valor;

  @override
  Future<void> borrar(String clave) async => _datos.remove(clave);
}

void main() {
  test('la sesión sobrevive a cerrar y reabrir la app', () async {
    final almacenCompartido = _AlmacenEnMemoria();

    // "Abrir la app": un ProviderScope, se guarda la sesión.
    final scope1 = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacenCompartido)],
    );
    await scope1
        .read(sesionProvider)
        .guardar(acceso: 'token-acceso', refresco: 'token-refresco');
    scope1.dispose();

    // "Cerrar y reabrir": un ProviderScope NUEVO (el estado de memoria de Riverpod
    // se perdió), pero el mismo almacén persistente del dispositivo.
    final scope2 = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacenCompartido)],
    );
    final sesionReabierta = scope2.read(sesionProvider);

    expect(await sesionReabierta.tokenDeAcceso(), 'token-acceso');
    expect(await sesionReabierta.tokenDeRefresco(), 'token-refresco');

    scope2.dispose();
  });

  test('cerrar sesión borra los dos tokens del almacén', () async {
    final almacen = _AlmacenEnMemoria();
    final scope = ProviderContainer(
      overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
    );
    final sesion = scope.read(sesionProvider);
    await sesion.guardar(acceso: 'a', refresco: 'r');
    await sesion.cerrar();

    expect(await sesion.tokenDeAcceso(), isNull);
    expect(await sesion.tokenDeRefresco(), isNull);
    scope.dispose();
  });
}
