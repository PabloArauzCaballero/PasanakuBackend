import 'package:go_router/go_router.dart';

import 'pantalla_centro_de_ayuda.dart';

/// Las rutas del dominio soporte: el centro de tutoriales, que es la quinta pestaña.
///
/// El hueco que este directorio declaraba —«queda vacío hasta que aparezca una pantalla
/// de soporte que no sea un caso de uso de `pasanaku` (p. ej. centro de ayuda)»— es
/// exactamente esto.
final List<RouteBase> rutasSoporte = [
  GoRoute(
    path: '/soporte/ayuda',
    name: 'soporte.ayuda',
    builder: (context, state) => const PantallaCentroDeAyuda(),
  ),
];
