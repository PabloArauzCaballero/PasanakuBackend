import 'package:flutter_riverpod/flutter_riverpod.dart';

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
  yield await conectividad.hayConexion();
  yield* conectividad.cambios;
});
