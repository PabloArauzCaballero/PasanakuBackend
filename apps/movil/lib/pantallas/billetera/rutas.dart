import 'package:go_router/go_router.dart';

import 'pantalla_cuenta_no_disponible.dart';

/// El login no entrega la cuenta autenticada y no existe un endpoint para
/// resolverla. Un UUID de la URL solo prueba formato, no titularidad. Hasta que
/// exista ese contrato, ninguna ruta de billetera consulta ni mueve dinero.

/// Las rutas del dominio billetera. **Este archivo lo posee el carril M2**; el shell
/// solo lo importa. Agregar una pantalla es agregar un archivo acá al lado y una
/// entrada en esta lista: nada de `navegacion/` cambia.
final List<RouteBase> rutasBilletera = [
  GoRoute(
    path: '/billetera/inicio',
    name: 'billetera.inicio',
    builder: (context, state) => const PantallaCuentaNoDisponible(),
  ),
  GoRoute(
    path: '/billetera/recargar',
    name: 'billetera.recargar',
    builder: (context, state) => const PantallaCuentaNoDisponible(),
  ),
  GoRoute(
    path: '/billetera/retirar',
    name: 'billetera.retirar',
    builder: (context, state) => const PantallaCuentaNoDisponible(),
  ),
  GoRoute(
    path: '/billetera/transferir',
    name: 'billetera.transferir',
    builder: (context, state) => const PantallaCuentaNoDisponible(),
  ),
  GoRoute(
    path: '/billetera/extracto',
    name: 'billetera.extracto',
    builder: (context, state) => const PantallaCuentaNoDisponible(),
  ),
];
