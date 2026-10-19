/// El enlace de invitación que se comparte por WhatsApp o SMS (2026-10-18).
///
/// El servidor entrega `aportaya://unirse/<tokenId>.<token>`: un esquema propio que la
/// app entiende, pero que WhatsApp y los SMS no muestran como enlace tocable. Lo que se
/// comparte es la dirección https de la app web, que abre en cualquier teléfono y
/// lleva a la misma pantalla de unirse (`#/pasanaku/unirse/<código>`: la app web usa
/// rutas con `#`).
///
/// La dirección web sale de `--dart-define=APP_WEB=...`; sin el define se deriva de la
/// API (`https://api.<dominio>/api/v1` → `https://app.<dominio>`), que es como están
/// publicadas las dos en TEST. Si no se puede armar, se devuelve el enlace original.
library;

const String _appWebCruda = String.fromEnvironment('APP_WEB');

/// La dirección de la app web, o `null` si no se puede saber.
String? baseDeLaAppWeb({String appWeb = _appWebCruda, String? api}) {
  final explicita = Uri.tryParse(appWeb);
  if (appWeb.isNotEmpty && explicita != null && explicita.scheme == 'https') {
    return appWeb.replaceAll(RegExp(r'/+$'), '');
  }
  final deLaApi = api == null ? null : Uri.tryParse(api);
  if (deLaApi == null || deLaApi.scheme != 'https') return null;
  final host = deLaApi.host;
  if (!host.startsWith('api.')) return null;
  return 'https://app.${host.substring(4)}';
}

/// El código de la invitación (`<tokenId>.<token>`) dentro del enlace del servidor.
String? codigoDeInvitacion(String enlace) {
  final uri = Uri.tryParse(enlace.trim());
  if (uri == null) return null;
  // La app web lleva la ruta después del `#`: también se mira ahí.
  final fragmento = uri.fragment.isEmpty
      ? const <String>[]
      : Uri.tryParse(uri.fragment)?.pathSegments ?? const <String>[];
  final partes = [
    if (uri.host.isNotEmpty) uri.host,
    ...uri.pathSegments,
    ...fragmento,
  ];
  final i = partes.indexOf('unirse');
  if (i < 0 || i + 1 >= partes.length) return null;
  final codigo = partes[i + 1];
  return RegExp(r'^[A-Za-z0-9-]+\.[A-Za-z0-9]+$').hasMatch(codigo)
      ? codigo
      : null;
}

/// El enlace https para compartir, o el original si no se puede armar.
String enlaceParaCompartir(String enlaceDelServidor, {String? baseWeb}) {
  final codigo = codigoDeInvitacion(enlaceDelServidor);
  if (baseWeb == null || codigo == null) return enlaceDelServidor;
  return '$baseWeb/#/pasanaku/unirse/$codigo';
}

/// El mensaje que se manda con el enlace.
String mensajeDeInvitacion(String enlace) =>
    'Te invito a mi pasanaku en AportaYa. Entrá con este enlace para unirte: $enlace';
