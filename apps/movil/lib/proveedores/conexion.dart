import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/error_de_operacion_de_dinero.dart';
import '../dominio/puertos/conectividad.dart';
import '../infraestructura/plataforma.dart';

/// El adaptador real (con la sonda al gateway, no solo el reporte del SO).
final conectividadProvider = Provider<Conectividad>(
  (_) => conectividadDeLaPlataforma(),
);

/// Estado de red vivo, para bloquear operaciones de dinero con motivo visible
/// (regla 1 del shell) y para que `EstadoDePantalla`/`EstadoError.sinConexion`
/// tengan de dónde leer en vez de que cada pantalla arme su propio `StreamBuilder`.
final hayConexionProvider = StreamProvider<bool>((ref) async* {
  final conectividad = ref.watch(conectividadProvider);
  try {
    yield await conectividad.hayConexion();
  } on Object catch (error, traza) {
    // Emitir el error sin cerrar el stream: la sonda periódica todavía puede
    // recuperar el acceso sin exigir que la persona reinicie la pantalla.
    yield* Stream<bool>.error(error, traza);
  }
  yield* conectividad.cambios;
});

/// Una operación monetaria requiere una sonda terminada y positiva. Ni el
/// estado inicial ni un error del proveedor autorizan el envío.
extension ConexionConfirmada on AsyncValue<bool> {
  bool get permiteOperar => value == true && !isLoading && !hasError;
}

/// Guardia en el límite del caso de uso. La UI puede quedar obsoleta entre el
/// toque y el POST; por eso cada comando vuelve a leer el estado vivo.
void exigirConexionConfirmada(Ref ref) {
  final estado = ref.read(hayConexionProvider);
  if (estado.permiteOperar) return;
  if (estado.isLoading) {
    throw const ErrorDeConexionNoConfirmada.comprobando();
  }
  if (estado.hasError) {
    throw const ErrorDeConexionNoConfirmada.fallida();
  }
  throw const ErrorDeConexionNoConfirmada.sinRed();
}
