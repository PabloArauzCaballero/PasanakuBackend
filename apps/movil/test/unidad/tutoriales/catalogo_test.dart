import 'dart:io';

import 'package:aportaya_movil/dominio/tutoriales/modelo.dart';
import 'package:aportaya_movil/dominio/tutoriales/validacion.dart';
import 'package:aportaya_movil/pantallas/soporte/catalogo/catalogo.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../comun.dart';

/// **La prueba que cuida el catálogo real.** Un tutorial que apunta a una pantalla que
/// ya no existe, o a un ancla que nadie marcó, se descubre acá y no cuando alguien lo
/// abre.
void main() {
  test('el catálogo de la app no tiene ningún problema de configuración', () {
    expect(
      validarCatalogo(catalogoDeTutoriales, rutas: rutasConocidas),
      isEmpty,
    );
  });

  test(
    'las rutas declaradas como conocidas son las que la app monta de verdad',
    () {
      final fuentes = _fuentesDe(
        '${raizDelRepositorio()}/apps/movil/lib/pantallas',
      );
      for (final ruta in rutasConocidas) {
        expect(
          fuentes,
          contains("path: '$ruta'"),
          reason: '$ruta no está declarada en ningún rutas.dart',
        );
      }
    },
  );

  test(
    'cada ancla de cada paso está marcada con MarcaDeTutorial en alguna pantalla',
    () {
      final fuentes = _fuentesDe('${raizDelRepositorio()}/apps/movil/lib');
      final anclas = {
        for (final t in catalogoDeTutoriales)
          for (final p in t.pasos)
            if (p.ancla != null) p.ancla!,
      };
      final faltantes = anclas
          .where((a) => !fuentes.contains("id: '$a'"))
          .toList();
      expect(faltantes, isEmpty, reason: 'anclas sin widget que las lleve');
    },
  );

  test('hay al menos un tutorial importante y todos declaran cuánto duran', () {
    expect(catalogoDeTutoriales.any((t) => t.obligatorio), isTrue);
    expect(catalogoDeTutoriales.every((t) => (t.minutos ?? 0) > 0), isTrue);
  });

  test('ningún paso pide una acción que escriba en el servidor', () {
    const seguras = {
      TipoDeAccion.ninguna,
      TipoDeAccion.toque,
      TipoDeAccion.navegar,
      TipoDeAccion.aparezca,
    };
    for (final t in catalogoDeTutoriales) {
      for (final p in t.pasos) {
        expect(seguras, contains(p.accion.tipo));
      }
    }
  });
}

String _fuentesDe(String raiz) => Directory(raiz)
    .listSync(recursive: true)
    .whereType<File>()
    .where((f) => f.path.endsWith('.dart'))
    .map((f) => f.readAsStringSync())
    .join('\n');
