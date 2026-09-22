import 'package:aportaya_movil/dominio/tutoriales/avance.dart';
import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/registro.dart';
import 'package:aportaya_movil/pantallas/soporte/vista_de_tutoriales.dart';
import 'package:flutter_test/flutter_test.dart';

import 'comun.dart';

void main() {
  group('RegistroDeTutoriales', () {
    final abierto = tutorialDePrueba('abierto', ruta: '/billetera/inicio');
    final conSesion = tutorialDePrueba(
      'con-sesion',
      ruta: '/billetera/extracto',
      requiere: {Capacidad.sesion},
    );
    final deOrganizador = tutorialDePrueba(
      'organizador',
      requiere: {Capacidad.organizador},
    );
    final catalogo = [abierto, conSesion, deOrganizador];

    test('sin capacidades solo se ve lo que no pide ninguna', () {
      expect(
        RegistroDeTutoriales(catalogo, const {}).disponibles.map((t) => t.id),
        ['abierto'],
      );
    });

    test('con sesión aparece lo suyo, y lo de organizador sigue oculto', () {
      final ids = RegistroDeTutoriales(catalogo, {
        Capacidad.sesion,
      }).disponibles.map((t) => t.id);
      expect(ids, contains('con-sesion'));
      expect(ids, isNot(contains('organizador')));
    });

    test('el tutorial de una pantalla es el de la ruta más específica', () {
      final registro = RegistroDeTutoriales(catalogo, {Capacidad.sesion});
      expect(registro.paraLaRuta('/billetera/extracto')?.id, 'con-sesion');
      expect(registro.paraLaRuta('/billetera/inicio')?.id, 'abierto');
    });

    test('una pantalla sin tutorial no inventa uno', () {
      expect(
        RegistroDeTutoriales(
          catalogo,
          const {},
        ).paraLaRuta('/identidad/perfil'),
        isNull,
      );
    });

    test('las categorías salen de lo disponible, no del catálogo entero', () {
      expect(RegistroDeTutoriales(catalogo, const {}).categorias, ['Pruebas']);
    });
  });

  group('componerLista', () {
    ProgresoDeTutorial hecho(String id) => cerrado(
      progresoInicial(tutorialDePrueba(id), ahoraDePrueba),
      EstadoDeProgreso.completado,
      ahoraDePrueba,
    );

    test(
      'ordena: importante, a medias, pendiente, omitido y al final lo hecho',
      () {
        final catalogo = [
          tutorialDePrueba('hecho'),
          tutorialDePrueba('pendiente'),
          tutorialDePrueba('medias'),
          tutorialDePrueba('importante', obligatorio: true),
        ];
        final avance = {
          'hecho': hecho('hecho'),
          'medias': enPaso(
            progresoInicial(tutorialDePrueba('medias'), ahoraDePrueba),
            1,
            'p2',
            ahoraDePrueba,
          ),
        };
        expect(componerLista(catalogo, avance).map((f) => f.tutorial.id), [
          'importante',
          'medias',
          'pendiente',
          'hecho',
        ]);
      },
    );

    test(
      'busca por título, descripción y categoría, sin importar mayúsculas',
      () {
        final catalogo = [
          tutorialDePrueba('saldo', categoria: 'Tu plata'),
          tutorialDePrueba('turno'),
        ];
        expect(
          componerLista(
            catalogo,
            const {},
            texto: 'SALDO',
          ).map((f) => f.tutorial.id),
          ['saldo'],
        );
        expect(
          componerLista(
            catalogo,
            const {},
            texto: 'tu plata',
          ).map((f) => f.tutorial.id),
          ['saldo'],
        );
      },
    );

    test(
      'el filtro de pendientes incluye los omitidos: dejarlo a medias no es hacerlo',
      () {
        final catalogo = [tutorialDePrueba('a'), tutorialDePrueba('b')];
        final avance = {
          'a': cerrado(
            progresoInicial(tutorialDePrueba('a'), ahoraDePrueba),
            EstadoDeProgreso.omitido,
            ahoraDePrueba,
          ),
        };
        final ids = componerLista(
          catalogo,
          avance,
          filtro: FiltroDeEstado.pendiente,
        ).map((f) => f.tutorial.id);
        expect(ids, containsAll(['a', 'b']));
      },
    );

    test('marca como continuable lo que quedó a mitad de camino', () {
      final avance = {
        'a': enPaso(
          progresoInicial(tutorialDePrueba('a'), ahoraDePrueba),
          1,
          'p2',
          ahoraDePrueba,
        ),
      };
      expect(
        componerLista([tutorialDePrueba('a')], avance).first.continuable,
        isTrue,
      );
    });

    test(
      'el recomendado es el primero sin requisitos pendientes; sin nada que hacer, no hay',
      () {
        final catalogo = [
          tutorialDePrueba('avanzado', requisitos: ['base']),
          tutorialDePrueba('base'),
        ];
        expect(
          recomendado(componerLista(catalogo, const {}))?.tutorial.id,
          'base',
        );
        expect(
          recomendado(
            componerLista([tutorialDePrueba('a')], {'a': hecho('a')}),
          ),
          isNull,
        );
      },
    );
  });
}
