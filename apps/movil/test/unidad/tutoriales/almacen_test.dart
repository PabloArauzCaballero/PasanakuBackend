import 'package:aportaya_movil/dominio/tutoriales/avance.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/infraestructura/almacen_de_progreso_seguro.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../comun.dart';
import 'comun.dart';

void main() {
  test('guarda y devuelve lo guardado', () async {
    final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
    final p = progresoInicial(tutorialDePrueba('a'), ahoraDePrueba);
    await almacen.guardar(p);
    final filas = await almacen.leer();
    expect(filas.single.tutorialId, 'a');
    expect(filas.single.estado, EstadoDeProgreso.enProgreso);
  });

  test(
    'guardar dos veces el mismo tutorial deja una sola fila, la última',
    () async {
      final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
      final p = progresoInicial(tutorialDePrueba('a'), ahoraDePrueba);
      await almacen.guardar(p);
      await almacen.guardar(enPaso(p, 3, 'p4', ahoraDePrueba));
      final filas = await almacen.leer();
      expect(filas, hasLength(1));
      expect(filas.single.indice, 3);
    },
  );

  test('reiniciar borra la fila de ese tutorial y deja el resto', () async {
    final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
    await almacen.guardar(
      progresoInicial(tutorialDePrueba('a'), ahoraDePrueba),
    );
    await almacen.guardar(
      progresoInicial(tutorialDePrueba('b'), ahoraDePrueba),
    );
    await almacen.reiniciar('a');
    expect((await almacen.leer()).map((p) => p.tutorialId), ['b']);
  });

  test('las fechas y las repeticiones sobreviven la ida y vuelta', () async {
    final almacen = AlmacenDeProgresoSeguro(AlmacenEnMemoria());
    final hecho = cerrado(
      progresoInicial(tutorialDePrueba('a'), ahoraDePrueba),
      EstadoDeProgreso.completado,
      ahoraDePrueba,
    );
    await almacen.guardar(hecho);
    final leido = (await almacen.leer()).single;
    expect(leido.repeticiones, 1);
    expect(leido.terminadoEn, ahoraDePrueba);
    expect(leido.iniciadoEn, ahoraDePrueba);
  });

  test('un contenido ilegible no rompe la app: se empieza de cero', () async {
    final crudo = AlmacenEnMemoria();
    await crudo.guardar(claveDeProgreso, '{no es json');
    expect(await AlmacenDeProgresoSeguro(crudo).leer(), isEmpty);
  });

  test(
    'descarta filas que no tienen forma de progreso y conserva las buenas',
    () async {
      final crudo = AlmacenEnMemoria();
      await crudo.guardar(
        claveDeProgreso,
        '[{"basura":true},{"tutorialId":"a","version":"1","estado":"enProgreso","indice":1,'
        '"iniciadoEn":"2026-09-17T12:00:00.000Z","ultimaInteraccion":"2026-09-17T12:00:00.000Z"}]',
      );
      final filas = await AlmacenDeProgresoSeguro(crudo).leer();
      expect(filas.map((p) => p.tutorialId), ['a']);
    },
  );
}
