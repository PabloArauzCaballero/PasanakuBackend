/// `validarGateway` — el equivalente Dart de `validarConfiguracion`
/// (`packages/dominio-cliente/src/configuracion.ts`, D-A6). Puro, sin Flutter y sin
/// `dart:io`: no depende de la plataforma, así se prueba igual en `flutter test` que
/// dentro de la app.
///
/// Historia: `dominio/cliente.dart` tenía un valor por omisión
/// (`http://localhost/api/v1`) para `baseDelGateway`. En un build de release sin
/// `--dart-define=API=...`, ese valor por omisión es **el propio teléfono de quien
/// instaló la app** — el mismo hallazgo que los `gateway.ts` de backoffice y web.
library;

/// El resultado: o la URL usable, o el motivo del rechazo — nunca las dos cosas, y
/// nunca una excepción (la función no lanza).
class ResultadoValidacionGateway {
  const ResultadoValidacionGateway.valida(this.url)
    : valida = true,
      motivo = null;

  const ResultadoValidacionGateway.invalida(this.motivo)
    : valida = false,
      url = null;

  final bool valida;
  final String? url;
  final String? motivo;

  @override
  String toString() => valida
      ? 'ResultadoValidacionGateway.valida($url)'
      : 'ResultadoValidacionGateway.invalida($motivo)';
}

final RegExp _rutaVersionada = RegExp(r'/v\d+/?$');
const Set<String> _loopback = {'localhost', '127.0.0.1', '::1', '0.0.0.0'};

/// Valida la URL del gateway para el build de la app.
///
/// - `release`: el equivalente de "producción" — en `debug`/`profile` se admite el
///   simulado (Prism) en `localhost`/`10.0.2.2` (el alias del emulador de Android a la
///   máquina anfitriona); en `release`, nunca.
/// - `hostsPermitidos`: la lista compilada de hosts propios (D-A6) — infra la carga en
///   el despliegue (Q-J4). En `release`, sin ningún host en la lista, **todo** host
///   externo queda rechazado — no hay "origen propio" implícito como en un navegador.
ResultadoValidacionGateway validarGateway(
  String? url, {
  required bool release,
  List<String> hostsPermitidos = const [],
}) {
  final cruda = (url ?? '').trim();
  if (cruda.isEmpty) {
    return const ResultadoValidacionGateway.invalida('vacia');
  }
  if (cruda.startsWith('//')) {
    return const ResultadoValidacionGateway.invalida(
      'esquema-relativo-a-protocolo',
    );
  }

  Uri analizada;
  try {
    analizada = Uri.parse(cruda);
  } on FormatException {
    return const ResultadoValidacionGateway.invalida('formato-invalido');
  }
  if (analizada.scheme != 'http' && analizada.scheme != 'https') {
    return const ResultadoValidacionGateway.invalida('esquema-no-soportado');
  }
  if (analizada.host.isEmpty) {
    return const ResultadoValidacionGateway.invalida('formato-invalido');
  }

  final host = analizada.host.toLowerCase();
  final esLoopback = _loopback.contains(host) || host.startsWith('127.');
  if (esLoopback && release) {
    return const ResultadoValidacionGateway.invalida('localhost-no-permitido');
  }

  final permitidos = hostsPermitidos
      .map((h) => h.trim().toLowerCase())
      .where((h) => h.isNotEmpty)
      .toSet();
  final hostPermitido = permitidos.contains(host);

  if (!hostPermitido && !esLoopback) {
    return const ResultadoValidacionGateway.invalida('host-ajeno');
  }
  if (release && analizada.scheme != 'https' && !esLoopback) {
    return const ResultadoValidacionGateway.invalida('sin-tls-en-produccion');
  }

  var ruta = analizada.path;
  if (ruta.length > 1 && ruta.endsWith('/')) {
    ruta = ruta.substring(0, ruta.length - 1);
  }
  if (!_rutaVersionada.hasMatch(ruta)) {
    return const ResultadoValidacionGateway.invalida('sin-ruta-versionada');
  }

  var normalizada = cruda;
  if (normalizada.length > 1 && normalizada.endsWith('/')) {
    normalizada = normalizada.substring(0, normalizada.length - 1);
  }
  return ResultadoValidacionGateway.valida(normalizada);
}
