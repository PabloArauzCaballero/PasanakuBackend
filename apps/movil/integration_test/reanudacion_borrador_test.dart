import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/infraestructura/android/almacen_seguro_android.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:patrol/patrol.dart';

/// Aísla los datos sintéticos del E2E sin tocar un borrador de una persona real.
class _AlmacenDeEnsayo implements AlmacenSeguro {
  _AlmacenDeEnsayo(this._real);

  final AlmacenSeguro _real;
  static const _prefijo = 'e2e.reanudacion.';

  @override
  Future<String?> leer(String clave) => _real.leer('$_prefijo$clave');

  @override
  Future<void> guardar(String clave, String valor) =>
      _real.guardar('$_prefijo$clave', valor);

  @override
  Future<void> borrar(String clave) => _real.borrar('$_prefijo$clave');
}

void main() {
  patrolTest('el borrador sobrevive a recrear la app con Keystore Android', (
    $,
  ) async {
    final almacen = _AlmacenDeEnsayo(AlmacenSeguroAndroid());
    const claveBorrador = 'alta.borrador.v1';
    await almacen.borrar(claveBorrador);

    Future<void> abrirRegistro() async {
      await $.pumpWidgetAndSettle(
        ProviderScope(
          overrides: [almacenSeguroProvider.overrideWithValue(almacen)],
          child: AppAportaYa(),
        ),
      );
      await $(TextosIdentidad.portadaCrearCuenta).tap();
      await $(TextosIdentidad.portadaSaltar).tap();
      await $(TextosIdentidad.registroTitulo).waitUntilVisible();
    }

    try {
      await abrirRegistro();
      await $(TextField).first.enterText('Ensayo');
      await $.tester.pump(const Duration(milliseconds: 450));
      // La escritura al Keystore es asíncrona después del debounce de 400 ms.
      await Future<void>.delayed(const Duration(milliseconds: 300));
      expect(await almacen.leer(claveBorrador), contains('Ensayo'));

      await $.pumpWidgetAndSettle(const SizedBox.shrink());
      await abrirRegistro();

      await $(TextosIdentidad.registroReanudado).waitUntilVisible();
      expect(
        $.tester
            .widget<TextField>(find.byType(TextField).first)
            .controller
            ?.text,
        'Ensayo',
      );
    } finally {
      await almacen.borrar(claveBorrador);
    }
  });
}
