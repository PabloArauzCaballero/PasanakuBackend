import 'package:aportaya_cliente_nucleofinanciero/aportaya_cliente_nucleofinanciero.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/cliente.dart';
import '../proveedores/sesion.dart';

/// CU-15 · emitir extracto y certificado de saldo. Es una consulta (sin efecto): no
/// lleva clave de idempotencia. `hashArchivo` es la evidencia de integridad del PDF
/// que emite el servidor; la app no genera ningún archivo por su cuenta.
class ParametrosExtracto {
  const ParametrosExtracto({
    required this.cuentaId,
    required this.desde,
    required this.hasta,
  });
  final String cuentaId;
  final DateTime desde;
  final DateTime hasta;

  // Es la clave del proveedor: sin igualdad por valor, cada redibujo pedía el
  // extracto de nuevo y «deslizar para recargar» no alcanzaba al que se ve.
  @override
  bool operator ==(Object other) =>
      other is ParametrosExtracto &&
      other.cuentaId == cuentaId &&
      other.desde == desde &&
      other.hasta == hasta;

  @override
  int get hashCode => Object.hash(cuentaId, desde, hasta);
}

final extractoProvider = FutureProvider.autoDispose
    .family<SalidaExtracto, ParametrosExtracto>((ref, p) async {
      ref.watch(generacionDeSesionProvider);
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(
          dio,
        ).emitirExtracto(cuentaId: p.cuentaId, desde: p.desde, hasta: p.hasta);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });
