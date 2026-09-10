import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Genera la clave al abrir un formulario y la **reutiliza** en el reintento. El
/// usuario en mala señal toca dos veces; eso no puede duplicar un aporte.
class Idempotencia extends Notifier<Map<String, String>> {
  @override
  Map<String, String> build() => {};

  String claveDe(String formularioId) {
    final existente = state[formularioId];
    if (existente != null) return existente;
    final nueva = _uuidV4();
    state = {...state, formularioId: nueva};
    return nueva;
  }

  /// Solo cuando el efecto se confirmó: la próxima vez es otra operación.
  void cerrar(String formularioId) {
    state = {...state}..remove(formularioId);
  }
}

final idempotenciaProvider =
    NotifierProvider<Idempotencia, Map<String, String>>(Idempotencia.new);

String _uuidV4() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
