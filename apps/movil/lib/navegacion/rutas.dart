import 'package:aportaya_diseno/organismos/estado_error.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../pantallas/alianzas/rutas.dart';
import '../pantallas/billetera/rutas.dart';
import '../pantallas/identidad/rutas.dart';
import '../pantallas/notificaciones/rutas.dart';
import '../pantallas/pasanaku/rutas.dart';
import '../pantallas/soporte/rutas.dart';
import 'enlaces_profundos.dart';
import 'shell.dart';

/// **El enchufe por dominio.** Lo escribe el shell (F2) UNA vez y se congela: cada
/// carril de pantallas llena el `rutas.dart` de su directorio y este archivo no
/// cambia. La prueba `enchufe_de_rutas_test.dart` lo verifica agregando una pantalla.
///
/// La tab bar (`ShellPrincipal`) envuelve solo las tres ramas con pila propia
/// (billetera, pasanaku, identidad — la maqueta §2.2). `alianzas`, `soporte` y
/// `notificaciones` se navegan por encima, con `push`, como el resto de la app: no
/// son destinos de la barra.
/// [inicial] por defecto es la **portada**: quien abre la app por primera vez tiene
/// que encontrarse con qué es AportaYa y cómo entrar, no con el tablero de una cuenta
/// que todavía no es suya. Las pruebas que van directo a una pantalla de adentro pasan
/// su ruta explícitamente.
GoRouter crearEnrutador({String inicial = '/portada'}) => GoRouter(
  initialLocation: inicial,
  redirect: (context, state) {
    final interna = rutaInternaDesde(state.uri);
    return interna;
  },
  routes: [
    StatefulShellRoute.indexedStack(
      builder: (context, state, navigationShell) =>
          ShellPrincipal(navigationShell: navigationShell),
      branches: [
        StatefulShellBranch(routes: rutasBilletera),
        StatefulShellBranch(
          // Sin esto, la rama arranca en su primera ruta declarada. Para `identidad`
          // esa primera ruta es `/identidad`, que es **iniciar sesión**: tocar
          // «Perfil» dejaba a alguien ya identificado frente a un formulario de login.
          // Cada rama dice explícitamente cuál es su casa.
          initialLocation: '/pasanaku/mi-estado',
          routes: [
            ...rutasPasanaku,
            // Marcador del shell hasta que el carril M3 llene `pasanaku/rutas.dart`
            // (ficha `F5`): la ruta es `/pasanaku`, distinta de cualquier subruta
            // que M3 vaya a declarar (`/pasanaku/...`), así que no hay colisión el
            // día que ese archivo deje de estar vacío.
            GoRoute(
              path: '/pasanaku',
              builder: (context, state) =>
                  const _AunNoDisponible(dominio: 'Grupos'),
            ),
          ],
        ),
        StatefulShellBranch(
          initialLocation: '/identidad/perfil',
          routes: [
            ...rutasIdentidad,
            // idem, para `identidad/rutas.dart` (carril M1, ficha `F3`).
            GoRoute(
              path: '/identidad',
              builder: (context, state) =>
                  const _AunNoDisponible(dominio: 'Perfil'),
            ),
          ],
        ),
        // La quinta rama: el centro de ayuda. Tiene pila propia a propósito — se entra
        // a buscar cómo se hace algo y se vuelve a lo que uno estaba haciendo, sin
        // perder el lugar.
        StatefulShellBranch(
          initialLocation: '/soporte/ayuda',
          routes: rutasSoporte,
        ),
      ],
    ),
    // Portada e ingreso van acá arriba, fuera del shell: son lo que se ve **antes**
    // de tener sesión, y una barra de pestañas debajo de un formulario de acceso
    // ofrece destinos a los que todavía no se puede ir.
    ...rutasDeEntrada,
    ...rutasAlianzas,
    ...rutasNotificaciones,
  ],
  errorBuilder: (context, state) => Scaffold(
    body: SafeArea(
      child: EstadoError(
        error: StateError('Ruta no encontrada: ${state.uri}'),
        reintentar: () => context.go(inicial),
      ),
    ),
  ),
);

/// Lo que ve una persona si toca un destino cuyo dominio todavía no tiene
/// pantallas — nunca una pantalla en blanco (gate del shell). Desaparece solo: en
/// cuanto el carril dueño agrega su primera ruta real bajo ese prefijo, `go_router`
/// la resuelve primero por ser más específica.
class _AunNoDisponible extends StatelessWidget {
  const _AunNoDisponible({required this.dominio});
  final String dominio;

  @override
  Widget build(BuildContext context) => Scaffold(
    body: SafeArea(
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(Espacio.s5),
          child: Text(
            '$dominio todavía no está disponible en esta versión.',
            textAlign: TextAlign.center,
          ),
        ),
      ),
    ),
  );
}
