import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// El puntaje de reputación propio (D-20 "Tu nivel"). `tieneHistorial: false` es
/// `SIN_HISTORIAL` — no es un castigo, es la verdad (CU-71): la pantalla lo trata
/// como un estado normal, no como un error ni como un vacío que invite a reintentar.
final puntajeProvider = FutureProvider.autoDispose
    .family<PuntajeDelUsuario, String>((ref, usuarioId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await DefaultApi(dio).consultarPuntaje(usuarioId: usuarioId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });
