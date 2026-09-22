import 'package:go_router/go_router.dart';

import 'pantalla_de_bandeja.dart';

/// Las rutas del dominio notificaciones. Las hace F2 (ficha `F2`): la bandeja es
/// del shell, no de un carril de pantallas.
final List<RouteBase> rutasNotificaciones = [
  GoRoute(
    path: '/notificaciones/bandeja',
    name: 'notificaciones.bandeja',
    builder: (context, state) => const PantallaDeBandeja(),
  ),
];
