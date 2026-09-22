import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'sesion.dart';

const _claveDispositivo = 'dispositivo.id';

/// El identificador de dispositivo de confianza que pide CU-04 al ingresar desde un
/// aparato nuevo. **Generado y guardado en el almacén seguro — nunca el IMEI ni el
/// `androidId`** (`planes/12`): esos identifican a la persona más allá de esta app,
/// y no son revocables si el dispositivo se pierde.
final dispositivoIdProvider = FutureProvider<String>((ref) async {
  final almacen = ref.watch(almacenSeguroProvider);
  final existente = await almacen.leer(_claveDispositivo);
  if (existente != null) return existente;
  final nuevo = _uuidV4();
  await almacen.guardar(_claveDispositivo, nuevo);
  return nuevo;
});

String _uuidV4() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
