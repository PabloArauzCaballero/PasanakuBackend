import 'package:aportaya_diseno/moviles/barra_pestanas.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

/// La tab bar de la app: Inicio · Grupos · Movimientos · Perfil.
///
/// **El shell no dibuja cabecera.** Antes ponía una barra con «AportaYa» y la campana,
/// y encima cada pantalla ponía su propio título: dos cabeceras apiladas para una sola
/// pantalla, con el nombre de la app ocupando el renglón más valioso cada vez. Ahora
/// cada pantalla trae la suya —saludo y título en las de nivel superior, volver y
/// título en las interiores— y la campana viaja con ella ([AccionDeAvisos]).
///
/// **Supuesto declarado:** `Inicio` y `Movimientos` son las dos entradas de la maqueta
/// hacia el dominio `billetera` (`/billetera/inicio` y `/billetera/extracto`).
/// `StatefulShellRoute.indexedStack` da una pila propia por **rama**, y las dos
/// comparten rama porque son el mismo dominio (mismo `rutas.dart`, que este shell no
/// edita): tocar `Movimientos` navega dentro de la rama de `billetera` en vez de
/// cambiar de rama.
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
      icono: Icons.donut_large_outlined,
      iconoActivo: Icons.donut_large,
      rama: 1,
      ruta: '/pasanaku/mi-estado',
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
      ruta: '/identidad/perfil',
    ),
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ubicacion = GoRouterState.of(context).uri.toString();
    final actual = _indiceVisibleDesde(navigationShell.currentIndex, ubicacion);
    return Scaffold(
      // El `indexedStack` cambia de rama sin transición: la pantalla nueva aparece de
      // golpe y no queda claro que uno se movió. Un fundido con un empujón corto hacia
      // arriba lo cuenta sin hacer esperar —180 ms, menos que el tiempo de sacar el
      // dedo—. La clave es el índice de rama: sin ella, Flutter reusa el mismo
      // elemento y no hay nada que fundir.
      body: _RamaConTransicion(
        indice: navigationShell.currentIndex,
        child: navigationShell,
      ),
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
          // El mismo golpecito que da el sistema al cambiar de pestaña: confirma el
          // toque sin esperar a que la pantalla nueva termine de dibujarse.
          HapticFeedback.selectionClick();
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

/// Funde y empuja apenas la rama que entra.
///
/// **Una sola copia del shell, siempre.** Un `AnimatedSwitcher` mantiene vivo al hijo
/// que sale mientras entra el nuevo, y el shell de `go_router` lleva un `GlobalKey`:
/// dos copias a la vez son la misma clave dos veces, y Flutter responde truncando
/// parte del árbol —una pantalla en blanco al cambiar de pestaña—. Acá el widget es
/// uno solo; lo que se anima es cómo aparece, no cuántos hay.
class _RamaConTransicion extends StatefulWidget {
  const _RamaConTransicion({required this.indice, required this.child});

  final int indice;
  final Widget child;

  @override
  State<_RamaConTransicion> createState() => _RamaConTransicionState();
}

class _RamaConTransicionState extends State<_RamaConTransicion>
    with SingleTickerProviderStateMixin {
  late final AnimationController _control = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 180),
    value: 1,
  );

  @override
  void didUpdateWidget(_RamaConTransicion anterior) {
    super.didUpdateWidget(anterior);
    if (anterior.indice == widget.indice) return;
    if (MediaQuery.disableAnimationsOf(context)) {
      _control.value = 1;
      return;
    }
    _control.forward(from: 0);
  }

  @override
  void dispose() {
    _control.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final curva = CurvedAnimation(parent: _control, curve: Curves.easeOut);
    return FadeTransition(
      opacity: curva,
      child: SlideTransition(
        position: Tween(
          begin: const Offset(0, 0.012),
          end: Offset.zero,
        ).animate(curva),
        child: widget.child,
      ),
    );
  }
}
