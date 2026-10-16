import 'package:go_router/go_router.dart';

import '../../pantallas/billetera/pantalla_de_saldo.dart';
import '../../pantallas/billetera/pantalla_extracto.dart';
import '../../pantallas/billetera/pantalla_recargar.dart';
import '../../pantallas/billetera/pantalla_retirar.dart';
import '../../pantallas/billetera/pantalla_transferir.dart';
import '../../pantallas/billetera/resolver_cuenta.dart';
import '../../pantallas/billetera/textos.dart';

/// Las rutas del dominio billetera. **Este archivo lo posee el carril M2**; el shell
/// solo lo importa. Agregar una pantalla es agregar un archivo acá al lado y una
/// entrada en esta lista: nada de `navegacion/` cambia.
///
/// La cuenta viene en `?cuenta=` cuando una pantalla la conoce; si no, se resuelve contra el
/// servidor (`GET /billetera/cuentas/propia`) en [PantallaConCuenta]. Ya no hay un id de cuenta
/// por omisión en el código (defecto D4 del carril H3).
final List<RouteBase> rutasBilletera = [
  GoRoute(
    path: '/billetera/inicio',
    name: 'billetera.inicio',
    builder: (context, state) =>
        PantallaDeSaldo(cuentaId: state.uri.queryParameters['cuenta']),
  ),
  GoRoute(
    path: '/billetera/recargar',
    name: 'billetera.recargar',
    builder: (context, state) => PantallaConCuenta(
      titulo: TextosBilletera.recargar,
      cuentaId: state.uri.queryParameters['cuenta'],
      construir: (id) => PantallaRecargar(cuentaId: id),
    ),
  ),
  GoRoute(
    path: '/billetera/retirar',
    name: 'billetera.retirar',
    builder: (context, state) => PantallaConCuenta(
      titulo: TextosBilletera.retirar,
      cuentaId: state.uri.queryParameters['cuenta'],
      construir: (id) => PantallaRetirar(cuentaId: id),
    ),
  ),
  GoRoute(
    path: '/billetera/transferir',
    name: 'billetera.transferir',
    builder: (context, state) => PantallaConCuenta(
      titulo: TextosBilletera.transferir,
      cuentaId: state.uri.queryParameters['cuenta'],
      construir: (id) => PantallaTransferir(cuentaOrigenId: id),
    ),
  ),
  GoRoute(
    path: '/billetera/extracto',
    name: 'billetera.extracto',
    builder: (context, state) => PantallaConCuenta(
      titulo: TextosBilletera.extracto,
      cuentaId: state.uri.queryParameters['cuenta'],
      construir: (id) => PantallaExtracto(cuentaId: id),
    ),
  ),
];
