import 'package:flutter/foundation.dart' show debugPrint, kDebugMode;

/// Registra solo datos seguros para correlacionar rechazos API en builds debug.
void registrarRechazoApi({
  required String metodo,
  required String ruta,
  required int estado,
  required String codigo,
  required String traza,
}) {
  if (!kDebugMode) return;
  // debugPrint llega a flutter attach/logcat; developer.log queda solo en DevTools.
  debugPrint(
    'API rechazo $metodo ${_rutaSegura(ruta)} HTTP $estado codigo=$codigo '
    'traza=${traza.isEmpty ? "sin-traza" : traza}',
  );
}

/// El aviso no contiene credenciales ni identificadores y solo sale en debug.
void registrarAvisoRefrescoAusente() {
  if (!kDebugMode) return;
  debugPrint('El servidor no entregó tokenRefresco para la sesión móvil.');
}

/// Nunca registra query params ni IDs dinámicos de cuentas o grupos.
String _rutaSegura(String path) {
  if (path == '/billetera/cuentas/propia' || path == '/sesiones') return path;
  for (final recurso in ['saldo', 'movimientos', 'extracto']) {
    if (RegExp('^/billetera/[^/]+/$recurso\$').hasMatch(path)) {
      return '/billetera/{cuentaId}/$recurso';
    }
  }
  if (path == '/grupos' || path.startsWith('/grupos/')) return '/grupos/{ruta}';
  return '<ruta-omitida>';
}
