import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'cliente.dart';

/// La versión de contrato que el cliente Dart generado (`clientes/dart`) trae
/// compilada. Se sube manualmente en cada micro-PR de contrato — no hay forma de
/// leerla del `pubspec.lock` del cliente sin tocarlo, y ese paquete es generado.
const String versionDeContratoCompilada = '1';

/// Al iniciar, pregunta al gateway (`GET /version`) si el contrato compilado sigue
/// siendo compatible. Si no, la persona ve un aviso para actualizar en vez de
/// errores `4xx` sueltos en cada pantalla — regla del shell (`planes/12` §alcance).
enum EstadoContrato { compatible, desactualizado, sinVerificar }

final verificacionContratoProvider = FutureProvider<EstadoContrato>((
  ref,
) async {
  final dio = ref.watch(dioProvider);
  try {
    final r = await dio.get<Map<String, dynamic>>('/version');
    final delGateway = r.data?['contrato'] as String?;
    if (delGateway == null) return EstadoContrato.sinVerificar;
    return delGateway == versionDeContratoCompilada
        ? EstadoContrato.compatible
        : EstadoContrato.desactualizado;
  } on DioException {
    // Sin gateway al iniciar: no se bloquea el arranque por esto — las pantallas
    // ya muestran su propio `EstadoError`/`EstadoVacio` cuando pidan datos.
    return EstadoContrato.sinVerificar;
  }
});
