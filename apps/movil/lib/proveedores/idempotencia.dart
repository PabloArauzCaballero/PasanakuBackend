import 'dart:math';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Una clave de idempotencia por **intento de negocio** (F1.L6.M12).
///
/// - El intento es el formulario más su **contenido normalizado** (importe, destino…):
///   reenviar lo mismo (doble toque, mala señal, timeout) reusa la clave y el
///   servidor no duplica; corregir el importe es otro intento y lleva otra clave.
/// - Ante un **rechazo definitivo** (4xx) la clave se libera: el servidor no hizo
///   nada y la persona tiene que poder corregir y reenviar.
/// - Ante la **duda** (timeout, sin respuesta, 5xx) la clave se conserva: si el
///   servidor alcanzó a procesar, el reintento con la misma clave no duplica.
/// - Ante el éxito se cierra: la próxima vez es otra operación.
class Idempotencia
    extends Notifier<Map<String, ({String huella, String clave})>> {
  @override
  Map<String, ({String huella, String clave})> build() => {};

  String claveDe(String formularioId, {String contenido = ''}) {
    final existente = state[formularioId];
    if (existente != null && existente.huella == contenido) {
      return existente.clave;
    }
    final nueva = _uuidV4();
    state = {...state, formularioId: (huella: contenido, clave: nueva)};
    return nueva;
  }

  /// Vuelve a usar una clave que sobrevivió a un cierre de la app: el servidor
  /// reconoce la misma operación y no la repite.
  void restaurar(String formularioId, String clave, {String contenido = ''}) {
    state = {...state, formularioId: (huella: contenido, clave: clave)};
  }

  /// Solo cuando el efecto se confirmó, o cuando el servidor lo rechazó sin efecto.
  void cerrar(String formularioId) {
    state = {...state}..remove(formularioId);
  }

  /// Después de un fallo: libera la clave si el rechazo es definitivo, la conserva si
  /// el resultado es desconocido.
  void trasElFallo(String formularioId, Object error) {
    if (esRechazoDefinitivo(error)) cerrar(formularioId);
  }
}

/// `true` solo si el servidor respondió y dijo que no (4xx), salvo los 4xx que
/// significan «probá de nuevo» (408, 425, 429): ahí el resultado sigue en duda.
bool esRechazoDefinitivo(Object error) {
  if (error is! DioException) return false;
  final estado = error.response?.statusCode;
  if (estado == null) return false;
  if (estado == 408 || estado == 425 || estado == 429) return false;
  return estado >= 400 && estado < 500;
}

final idempotenciaProvider =
    NotifierProvider<
      Idempotencia,
      Map<String, ({String huella, String clave})>
    >(Idempotencia.new);

String _uuidV4() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
