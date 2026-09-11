import 'package:go_router/go_router.dart';

import '../../pantallas/billetera/pantalla_de_saldo.dart';
import '../../pantallas/billetera/pantalla_extracto.dart';
import '../../pantallas/billetera/pantalla_recargar.dart';
import '../../pantallas/billetera/pantalla_retirar.dart';
import '../../pantallas/billetera/pantalla_transferir.dart';

const _cuentaPorOmision = '11111111-1111-4111-8111-111111111111';

/// Las rutas del dominio billetera. **Este archivo lo posee el carril M2**; el shell
/// solo lo importa. Agregar una pantalla es agregar un archivo acá al lado y una
/// entrada en esta lista: nada de `navegacion/` cambia.
final List<RouteBase> rutasBilletera = [
  GoRoute(
    path: '/billetera/inicio',
    name: 'billetera.inicio',
    builder: (context, state) => PantallaDeSaldo(
      cuentaId: state.uri.queryParameters['cuenta'] ?? _cuentaPorOmision,
    ),
  ),
  GoRoute(
    path: '/billetera/recargar',
    name: 'billetera.recargar',
    builder: (context, state) => PantallaRecargar(
      cuentaId: state.uri.queryParameters['cuenta'] ?? _cuentaPorOmision,
    ),
  ),
  GoRoute(
    path: '/billetera/retirar',
    name: 'billetera.retirar',
    builder: (context, state) => PantallaRetirar(
      cuentaId: state.uri.queryParameters['cuenta'] ?? _cuentaPorOmision,
    ),
  ),
  GoRoute(
    path: '/billetera/transferir',
    name: 'billetera.transferir',
    builder: (context, state) => PantallaTransferir(
      cuentaOrigenId: state.uri.queryParameters['cuenta'] ?? _cuentaPorOmision,
    ),
  ),
  GoRoute(
    path: '/billetera/extracto',
    name: 'billetera.extracto',
    builder: (context, state) => PantallaExtracto(
      cuentaId: state.uri.queryParameters['cuenta'] ?? _cuentaPorOmision,
    ),
  ),
];
