import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../pantallas/notificaciones/proveedor_bandeja.dart';

/// La tab bar de la maqueta (§2.2): Inicio · Grupos · Movimientos · Perfil, con la
/// campana de avisos aparte, en el `AppBar` de cada pantalla de nivel superior — no
/// es un quinto destino, es una acción que abre `/notificaciones/bandeja`.
///
/// **Supuesto declarado:** `Inicio` y `Movimientos` son las dos entradas de la
/// maqueta hacia el dominio `billetera` (`/billetera/inicio` y `/billetera/extracto`).
/// `StatefulShellRoute.indexedStack` da una pila propia por **rama**, y las dos
/// comparten rama porque son el mismo dominio (mismo `rutas.dart`, que este shell no
/// edita): tocar `Movimientos` navega dentro de la rama de `billetera` en vez de
/// cambiar de rama. `/billetera/extracto` la agrega el carril `M2`; hasta entonces
/// el `errorBuilder` de `crearEnrutador` muestra el estado «no disponible todavía»,
/// nunca una pantalla en blanco.
class ShellPrincipal extends ConsumerWidget {
  const ShellPrincipal({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  static const _destinos = [
    (
      texto: 'Inicio',
      icono: Icons.home_outlined,
      iconoActivo: Icons.home,
      rama: 0,
      ruta: '/billetera/inicio',
    ),
    (
      texto: 'Grupos',
      icono: Icons.groups_outlined,
      iconoActivo: Icons.groups,
      rama: 1,
      ruta: '/pasanaku',
    ),
    (
      texto: 'Movimientos',
      icono: Icons.swap_horiz_outlined,
      iconoActivo: Icons.swap_horiz,
      rama: 0,
      ruta: '/billetera/extracto',
    ),
    (
      texto: 'Perfil',
      icono: Icons.person_outline,
      iconoActivo: Icons.person,
      rama: 2,
      ruta: '/identidad',
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final sinLeer = ref.watch(noLeidasProvider);
    final ubicacion = GoRouterState.of(context).uri.toString();
    final actual = _indiceVisibleDesde(navigationShell.currentIndex, ubicacion);
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: const Text('AportaYa'),
        actions: [
          Badge(
            isLabelVisible: sinLeer > 0,
            label: Text('$sinLeer'),
            child: IconButton(
              icon: const Icon(Icons.notifications_outlined),
              tooltip: sinLeer > 0
                  ? 'Notificaciones, $sinLeer sin leer'
                  : 'Notificaciones',
              onPressed: () => context.push('/notificaciones/bandeja'),
            ),
          ),
        ],
      ),
      body: navigationShell,
      bottomNavigationBar: BarraPestanas(
        destinos: [
          for (final d in _destinos)
            (
              texto: d.texto,
              icono: d.icono,
              iconoActivo: d.iconoActivo,
              novedades: 0,
            ),
        ],
        actual: actual,
        onChanged: (i) {
          final d = _destinos[i];
          if (d.rama == navigationShell.currentIndex) {
            context.go(d.ruta);
          } else {
            navigationShell.goBranch(d.rama, initialLocation: true);
          }
        },
      ),
    );
  }

  /// Dos destinos visibles (`Inicio`, `Movimientos`) apuntan a la misma rama: se
  /// marca el que coincide con la ubicación actual, no siempre el primero.
  int _indiceVisibleDesde(int rama, String ubicacion) => switch (rama) {
    1 => 1,
    2 => 3,
    _ => ubicacion.startsWith('/billetera/extracto') ? 2 : 0,
  };
}
