import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// CU-61 · verificación pública del sorteo. Ruta **sin sesión** en `transparencia`;
/// recomputa con el mismo átomo que sorteó (CU-60), así que la app nunca reproduce
/// el barajado por su cuenta — mostrar un veredicto propio, distinto al del
/// servidor, sería justamente el riesgo que el compromiso-revelación evita.
final verificarSorteoProvider = FutureProvider.autoDispose
    .family<SalidaVerificacionSorteo, String>((ref, sorteoId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(dio).verificarSorteo(sorteoId: sorteoId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });
