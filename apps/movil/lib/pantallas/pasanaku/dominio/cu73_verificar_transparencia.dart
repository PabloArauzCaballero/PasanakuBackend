import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// CU-73 · verificar la cadena de transparencia de un grupo. Ruta pública: recorre
/// los bloques y devuelve el primero que falla. Un grupo sin bloques todavía no es
/// un error de integridad (así lo aclara el CU) — la pantalla lo distingue de una
/// cadena rota, nunca los mezcla en el mismo mensaje.
final verificarCadenaProvider = FutureProvider.autoDispose
    .family<SalidaVerificacionCadena, String>((ref, grupoId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(dio).verificarCadena(grupoId: grupoId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });
