import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../proveedores/sesion.dart';

/// Guardias que cada `rutas.dart` de dominio **compone** en su propia `GoRoute`
/// (`redirect: (c, s) => exigirSesion(ref, s)`). Este archivo no se edita para
/// agregar una pantalla — eso es exactamente lo que rompe el enchufe por dominio.
class Guardias {
  Guardias(this._ref);
  final Ref _ref;

  /// `null` deja pasar; una cadena no vacía es la ruta a la que se redirige.
  Future<String?> exigirSesion(GoRouterState estado) async {
    final sesion = _ref.read(sesionProvider);
    final token = await sesion.tokenDeAcceso();
    if (token != null) return null;
    final vuelta = Uri.encodeComponent(estado.uri.toString());
    return '/identidad/ingreso?volver=$vuelta';
  }

  /// Nivel de verificación insuficiente (CU-02, CU-40): a la pantalla que explica
  /// qué desbloquea el nivel siguiente, sin perder a dónde iba.
  String? exigirNivel(
    GoRouterState estado, {
    required int minimo,
    required int actual,
  }) {
    if (actual >= minimo) return null;
    return '/identidad/aumentar-limite?requerido=$minimo';
  }

  /// CU-46 — servicio no habilitado para la cuenta: se explica sin jerga, no se
  /// muestra la pantalla como si el problema fuera de red.
  String? exigirHabilitado(GoRouterState estado, {required bool habilitado}) {
    if (habilitado) return null;
    return '/soporte/servicio-no-habilitado';
  }
}

final guardiasProvider = Provider<Guardias>((ref) => Guardias(ref));
