import 'package:aportaya_movil/dominio/tutoriales/avance.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:flutter_test/flutter_test.dart';

import 'comun.dart';

void main() {
  final t = tutorialDePrueba('a', pasos: 4);

  test('arranca en el primer paso, en progreso y sin terminar', () {
    final p = progresoInicial(t, ahoraDePrueba);
    expect(p.estado, EstadoDeProgreso.enProgreso);
    expect(p.indice, 0);
    expect(p.pasoId, 'p1');
    expect(p.terminadoEn, isNull);
  });

  test('completar suma una repetición; omitir no', () {
    final inicial = progresoInicial(t, ahoraDePrueba);
    expect(
      cerrado(inicial, EstadoDeProgreso.completado, ahoraDePrueba).repeticiones,
      1,
    );
    expect(
      cerrado(inicial, EstadoDeProgreso.omitido, ahoraDePrueba).repeticiones,
      0,
    );
  });

  test('la fracción sale del paso, y completado es siempre 1', () {
    final enDos = enPaso(
      progresoInicial(t, ahoraDePrueba),
      2,
      'p3',
      ahoraDePrueba,
    );
    expect(fraccionDe(enDos, t), 0.5);
    expect(
      fraccionDe(cerrado(enDos, EstadoDeProgreso.completado, ahoraDePrueba), t),
      1,
    );
  });

  test('sin progreso, el tutorial está pendiente y en cero', () {
    expect(estadoDe(null, t), EstadoDeProgreso.pendiente);
    expect(fraccionDe(null, t), 0);
  });

  test('cambiar la versión invalida lo hecho: vuelve a ofrecerse', () {
    final hecho = cerrado(
      progresoInicial(t, ahoraDePrueba),
      EstadoDeProgreso.completado,
      ahoraDePrueba,
    );
    expect(estadoDe(hecho, t), EstadoDeProgreso.completado);
    final nueva = tutorialDePrueba('a', version: '2.0.0', pasos: 4);
    expect(vigente(hecho, nueva), isFalse);
    expect(estadoDe(hecho, nueva), EstadoDeProgreso.pendiente);
  });

  test('lo abandonado a mitad de camino también se puede retomar', () {
    final aMedias = enPaso(
      progresoInicial(t, ahoraDePrueba),
      2,
      'p3',
      ahoraDePrueba,
    );
    final abandonado = cerrado(
      aMedias,
      EstadoDeProgreso.omitido,
      ahoraDePrueba,
    );
    expect(sePuedeContinuar(abandonado, t), isTrue);
    final desdeElPrincipio = cerrado(
      progresoInicial(t, ahoraDePrueba),
      EstadoDeProgreso.omitido,
      ahoraDePrueba,
    );
    expect(sePuedeContinuar(desdeElPrincipio, t), isFalse);
  });

  test('se continúa solo lo que quedó a medias en la versión vigente', () {
    final enDos = enPaso(
      progresoInicial(t, ahoraDePrueba),
      2,
      'p3',
      ahoraDePrueba,
    );
    expect(sePuedeContinuar(enDos, t), isTrue);
    expect(
      sePuedeContinuar(
        enDos,
        tutorialDePrueba('a', version: '2.0.0', pasos: 4),
      ),
      isFalse,
    );
    expect(sePuedeContinuar(progresoInicial(t, ahoraDePrueba), t), isFalse);
  });

  test('el avance general no cuenta lo omitido como hecho', () {
    final otro = tutorialDePrueba('b', pasos: 4);
    final mapa = {
      'a': cerrado(
        progresoInicial(t, ahoraDePrueba),
        EstadoDeProgreso.completado,
        ahoraDePrueba,
      ),
      'b': cerrado(
        progresoInicial(otro, ahoraDePrueba),
        EstadoDeProgreso.omitido,
        ahoraDePrueba,
      ),
    };
    expect(avanceGeneral([t, otro], mapa), 0.5);
  });

  test('los requisitos pendientes son los que todavía no se completaron', () {
    final base = tutorialDePrueba('base');
    final con = tutorialDePrueba('con', requisitos: ['base']);
    expect(requisitosPendientes(con, {'base': base}, {}).map((r) => r.id), [
      'base',
    ]);
    final hechos = {
      'base': cerrado(
        progresoInicial(base, ahoraDePrueba),
        EstadoDeProgreso.completado,
        ahoraDePrueba,
      ),
    };
    expect(requisitosPendientes(con, {'base': base}, hechos), isEmpty);
  });
}
