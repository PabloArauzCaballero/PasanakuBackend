import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import 'sesion.dart';

/// Pide un token nuevo con el refresco guardado, sin cerrar la sesión. El token trae los
/// permisos vigentes: así, cuando aprueban la identidad, la persona activa la billetera y
/// los grupos sin volver a ingresar. Usa el mismo transporte que la app y ningún
/// interceptor (igual que `instalarInterceptores`). `true` si se renovó.
final renovarAccesoProvider = Provider<Future<bool> Function()>((ref) {
  return () {
    final dio = ref.read(dioProvider);
    final sinInterceptores = Dio(BaseOptions(baseUrl: dio.options.baseUrl))
      ..httpClientAdapter = dio.httpClientAdapter;
    return Refrescador(ref.read(sesionProvider), sinInterceptores).refrescar();
  };
});
