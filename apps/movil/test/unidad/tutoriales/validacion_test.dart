import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/validacion.dart';
import 'package:flutter_test/flutter_test.dart';

import 'comun.dart';

List<CodigoDeProblema> codigos(List<ProblemaDeCatalogo> p) =>
    p.map((x) => x.codigo).toList();

void main() {
  test('un catálogo sano no tiene nada que decir', () {
    expect(
      validarCatalogo([tutorialDePrueba('a'), tutorialDePrueba('b')]),
      isEmpty,
    );
  });

  test('detecta ids duplicados: uno pisa al otro', () {
    final problemas = validarCatalogo([
      tutorialDePrueba('a'),
      tutorialDePrueba('a'),
    ]);
    expect(codigos(problemas), contains(CodigoDeProblema.idDuplicado));
  });

  test('detecta un tutorial sin pasos', () {
    expect(
      codigos(validarCatalogo([tutorialDePrueba('a', pasos: 0)])),
      contains(CodigoDeProblema.tutorialVacio),
    );
  });

  test('detecta un paso que no resalta nada ni navega', () {
    const t = TutorialDefinicion(
      id: 'a',
      version: '1',
      titulo: 'A',
      descripcion: 'd',
      categoria: 'c',
      dificultad: Dificultad.inicial,
      pasos: [PasoDeTutorial(id: 'p1', titulo: 't', descripcion: 'd')],
    );
    expect(
      codigos(validarCatalogo([t])),
      contains(CodigoDeProblema.pasoSinAncla),
    );
  });

  test(
    'un paso sin ancla pero que pide navegar es válido: el objetivo es la ruta',
    () {
      const t = TutorialDefinicion(
        id: 'a',
        version: '1',
        titulo: 'A',
        descripcion: 'd',
        categoria: 'c',
        dificultad: Dificultad.inicial,
        pasos: [
          PasoDeTutorial(
            id: 'p1',
            titulo: 't',
            descripcion: 'd',
            accion: AccionEsperada.navegar('/billetera/inicio'),
          ),
        ],
      );
      expect(validarCatalogo([t], rutas: const ['/billetera/inicio']), isEmpty);
    },
  );

  test('detecta una ruta que la app no sabe abrir, y acepta una subruta', () {
    expect(
      codigos(
        validarCatalogo(
          [tutorialDePrueba('a', ruta: '/inventada')],
          rutas: const ['/billetera/inicio'],
        ),
      ),
      contains(CodigoDeProblema.rutaInexistente),
    );
    expect(
      validarCatalogo(
        [tutorialDePrueba('a', ruta: '/pasanaku/mi-estado')],
        rutas: const ['/pasanaku'],
      ),
      isEmpty,
    );
  });

  test('detecta requisitos y siguientes que no existen', () {
    final problemas = codigos(
      validarCatalogo([
        tutorialDePrueba('a', requisitos: ['fantasma'], siguiente: 'otro'),
      ]),
    );
    expect(problemas, contains(CodigoDeProblema.requisitoInexistente));
    expect(problemas, contains(CodigoDeProblema.siguienteInexistente));
  });

  test('detecta una dependencia circular entre requisitos', () {
    final catalogo = [
      tutorialDePrueba('a', requisitos: ['b']),
      tutorialDePrueba('b', requisitos: ['a']),
    ];
    expect(
      codigos(validarCatalogo(catalogo)),
      contains(CodigoDeProblema.ciclo),
    );
  });

  test('detecta pasos duplicados dentro de un tutorial', () {
    const t = TutorialDefinicion(
      id: 'a',
      version: '1',
      titulo: 'A',
      descripcion: 'd',
      categoria: 'c',
      dificultad: Dificultad.inicial,
      pasos: [
        PasoDeTutorial(id: 'p1', titulo: 't', descripcion: 'd', ancla: 'x'),
        PasoDeTutorial(id: 'p1', titulo: 't', descripcion: 'd', ancla: 'y'),
      ],
    );
    expect(
      codigos(validarCatalogo([t])),
      contains(CodigoDeProblema.pasoDuplicado),
    );
  });

  test('devuelve TODOS los problemas, no el primero', () {
    final catalogo = [
      tutorialDePrueba('a', pasos: 0),
      tutorialDePrueba('a', ruta: '/inventada'),
    ];
    expect(
      validarCatalogo(catalogo, rutas: const ['/billetera/inicio']).length,
      greaterThan(1),
    );
  });
}
