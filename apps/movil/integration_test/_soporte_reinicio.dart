import 'package:aportaya_movil/app.dart';
import 'package:aportaya_movil/dominio/puertos/almacen_seguro.dart';
import 'package:aportaya_movil/infraestructura/android/almacen_seguro_android.dart';
import 'package:aportaya_movil/pantallas/identidad/textos.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:patrol/patrol.dart';

const claveBorradorDeEnsayo = 'alta.borrador.v1';
const nombreDeEnsayo = 'Ensayo de reinicio';

/// El prefijo deja intacto el borrador normal de quien use este dispositivo.
class AlmacenDeEnsayo implements AlmacenSeguro {
  AlmacenDeEnsayo() : _real = AlmacenSeguroAndroid();

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

Future<void> abrirRegistroEnEnsayo(
  PatrolIntegrationTester $,
  AlmacenDeEnsayo almacen,
) async {
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
