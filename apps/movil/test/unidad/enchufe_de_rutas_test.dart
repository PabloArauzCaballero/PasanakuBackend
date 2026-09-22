import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

import '../comun.dart';

/// **La prueba del gate de F0**: agregar una pantalla no toca nada fuera del
/// directorio de su dominio. El shell importa el `rutas.dart` de cada dominio una vez
/// y se congela; el carril llena el suyo.
void main() {
  final lib = Directory('${raizDelRepositorio()}/apps/movil/lib');
  final dominios = [
    'identidad',
    'billetera',
    'alianzas',
    'pasanaku',
    'soporte',
    'notificaciones',
  ];

  test(
    'el enchufe del shell importa las rutas de cada dominio, y nada más las conoce',
    () {
      final enchufe = File(
        '${lib.path}/navegacion/rutas.dart',
      ).readAsStringSync();
      for (final d in dominios) {
        expect(enchufe, contains("import '../pantallas/$d/rutas.dart';"));
        expect(
          File('${lib.path}/pantallas/$d/rutas.dart').existsSync(),
          isTrue,
          reason: '$d sin rutas.dart',
        );
      }
    },
  );

  test(
    'agregar una pantalla vacía en un dominio no cambia ningún archivo fuera de él',
    () {
      Map<String, String> huella() => {
        for (final f in lib.listSync(recursive: true).whereType<File>())
          if (!f.path.contains('/pantallas/identidad/'))
            f.path: f.readAsStringSync(),
      };
      final antes = huella();
      final nueva = File(
        '${lib.path}/pantallas/identidad/pantalla_de_andamiaje.dart',
      );
      final rutas = File('${lib.path}/pantallas/identidad/rutas.dart');
      final rutasAntes = rutas.readAsStringSync();
      nueva.writeAsStringSync(
        "import 'package:flutter/material.dart';\nclass PantallaDeAndamiaje extends StatelessWidget { const PantallaDeAndamiaje({super.key}); @override Widget build(BuildContext c) => const SizedBox(); }\n",
      );
      rutas.writeAsStringSync(
        rutasAntes.replaceFirst(
          '= [];',
          "= [GoRoute(path: '/identidad/andamiaje', builder: (c, s) => const SizedBox())];",
        ),
      );
      try {
        final despues = huella();
        expect(despues, equals(antes));
      } finally {
        nueva.deleteSync();
        rutas.writeAsStringSync(rutasAntes);
      }
    },
  );
}
