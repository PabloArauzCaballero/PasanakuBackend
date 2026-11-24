import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:aportaya_movil/proveedores/sesion.dart';
import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// DOC-001: el comentario de [AlmacenDeProgresoSeguro] prometía que el avance de los
/// tutoriales se va con la sesión, pero al cerrar solo se borraban los tokens. En un
/// teléfono compartido, quien entraba después heredaba el avance de la persona anterior.
void main() {
  test(
    'correcto: al cerrar sesión se borra el progreso de tutoriales',
    () async {
      final almacen = AlmacenEnMemoria();
      final sesion = Sesion(almacen);
      await sesion.abrir(acceso: 'a', refresco: 'r');
      await almacen.guardar(claveDeProgreso, '[{"tutorial":"x"}]');

      await sesion.cerrar();

      expect(await almacen.leer(claveDeProgreso), isNull);
    },
  );

  test('límite: la huella de la instalación sobrevive al cierre', () async {
    final almacen = AlmacenEnMemoria();
    final sesion = Sesion(almacen);
    await sesion.abrir(acceso: 'a', refresco: 'r');
    await sesion.guardarHuella('h-1');
    await almacen.guardar(claveDeProgreso, '[]');

    await sesion.cerrar();

    expect(await sesion.leerHuella(), 'h-1');
    expect(await almacen.leer(claveDeProgreso), isNull);
  });

  test('inválido: cerrar una sesión que ya estaba cerrada no falla', () async {
    final sesion = Sesion(AlmacenEnMemoria());
    await expectLater(sesion.cerrar(), completes);
  });
}
