/// Traduce un *deep link* de esquema propio (`aportaya://unirse/ABC123`) a una ruta
/// interna de `go_router` (`/pasanaku/unirse/ABC123`). Vive en `navegacion/` porque
/// es el shell quien decide cómo entra un enlace externo — **no** una tabla que cada
/// dominio edita: agregar un enlace nuevo es una entrada acá, y la pantalla de
/// destino la sigue poseyendo su propio `rutas.dart`.
///
/// `AndroidManifest.xml` declara el intent-filter del esquema `aportaya`; Flutter
/// reenvía la URI recibida como ruta inicial (o como navegación en caliente si la
/// app ya estaba abierta) por el canal de sistema `flutter/navigation`, sin
/// necesitar un paquete de pub adicional.
const Map<String, String> _prefijosPorHost = {
  'unirse': '/pasanaku/unirse',
  'billetera': '/billetera/inicio',
  'aporte': '/billetera/aportes',
  'notificaciones': '/notificaciones/bandeja',
};

/// `null` si `uri` no es del esquema propio o no hay traducción conocida — en ese
/// caso `go_router` la trata como una ruta interna normal (o cae en el `errorBuilder`,
/// nunca en pantalla en blanco).
String? rutaInternaDesde(Uri uri) {
  if (uri.scheme != 'aportaya') return null;
  final prefijo = _prefijosPorHost[uri.host];
  if (prefijo == null) return null;
  final resto = uri.pathSegments;
  if (resto.isEmpty) return prefijo;
  return '$prefijo/${resto.join('/')}';
}
