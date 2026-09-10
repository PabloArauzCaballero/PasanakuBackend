import 'package:go_router/go_router.dart';

import '../../pantallas/billetera/pantalla_de_saldo.dart';

/// Las rutas del dominio billetera. **Este archivo lo posee el carril M2**; el shell
/// solo lo importa. Agregar una pantalla es agregar un archivo acá al lado y una
/// entrada en esta lista: nada de `navegacion/` cambia.
final List<RouteBase> rutasBilletera = [
  GoRoute(
    path: '/billetera/inicio',
    name: 'billetera.inicio',
    builder: (context, state) => PantallaDeSaldo(
      cuentaId:
          state.uri.queryParameters['cuenta'] ??
          '11111111-1111-4111-8111-111111111111',
    ),
  ),
];
