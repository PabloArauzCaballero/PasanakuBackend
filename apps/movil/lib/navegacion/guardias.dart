import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

/// Guards de capacidades específicas del dominio. La autenticación de rutas es global
/// en `navegacion/rutas.dart` para evitar políticas divergentes por pantalla.
class Guardias {
  Guardias();

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

final guardiasProvider = Provider<Guardias>((ref) => Guardias());
