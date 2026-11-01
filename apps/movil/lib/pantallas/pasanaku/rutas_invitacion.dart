import 'package:flutter/widgets.dart';
import 'package:go_router/go_router.dart';

import 'pantalla_escanear.dart';
import 'pantalla_unirse.dart';

/// El enlace llega también antes de iniciar sesión, por eso vive fuera del shell.
final List<RouteBase> rutasDeInvitacion = [
  GoRoute(
    path: '/pasanaku/escanear',
    name: 'pasanaku.escanear',
    builder: (context, state) => const PantallaEscanear(),
  ),
  GoRoute(
    path: '/pasanaku/unirse/:codigo',
    name: 'pasanaku.unirse',
    // La clave por código: con la app ya abierta en una invitación, un segundo enlace llega a
    // la MISMA ruta con otro parámetro y, sin clave, Flutter reutilizaba el estado y seguía
    // mostrando la invitación anterior (hallazgo del carril H en emulador).
    builder: (context, state) {
      final codigo = state.pathParameters['codigo']!;
      return PantallaUnirse(key: ValueKey(codigo), codigo: codigo);
    },
  ),
];
