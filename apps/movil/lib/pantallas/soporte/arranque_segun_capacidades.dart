import 'package:flutter/foundation.dart' show kReleaseMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../proveedores/capacidades.dart';
import 'pantalla_bloqueo_plataforma.dart';

/// El gate de arranque de H2.S2.M3 (madre H6.S2.M3): en un RELEASE donde el almacén
/// seguro o la protección de pantalla no están soportados, se muestra
/// [PantallaBloqueoPlataforma] en vez de construir el resto de la app -- y
/// [builder] (que es quien termina leyendo `verificacionContratoProvider` y por lo
/// tanto creando el cliente HTTP, `dominio/cliente.dart`) **nunca se llama**.
///
/// `esRelease` tiene a `kReleaseMode` como valor por omisión -- que es una constante
/// de COMPILACIÓN, no se puede simular en una prueba -- así que las pruebas
/// (`test/widget/bloqueo_plataforma_test.dart`) lo fuerzan a `true` explícitamente
/// en vez de intentar compilar la suite en modo release.
class ArranqueSegunCapacidades extends ConsumerWidget {
  const ArranqueSegunCapacidades({
    super.key,
    required this.builder,
    this.esRelease = kReleaseMode,
  });

  final WidgetBuilder builder;
  final bool esRelease;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final capacidades = ref.watch(capacidadesProvider);
    if (esRelease && !capacidades.seguridadCriticaSoportada) {
      return const MaterialApp(
        title: 'AportaYa',
        debugShowCheckedModeBanner: false,
        home: PantallaBloqueoPlataforma(),
      );
    }
    return builder(context);
  }
}
