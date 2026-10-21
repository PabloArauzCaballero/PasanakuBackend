import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import 'mis_grupos.dart';

/// Cuántos grupos organizo y en cuántos participo, contra su tope (2026-10-20). Los
/// topes los aplica el servidor; esto es para mostrarlos antes de intentar algo que
/// va a ser rechazado. Sin respuesta, la pantalla sigue sin los contadores.
final misLimitesProvider = FutureProvider.autoDispose<MisLimites>((ref) async {
  ref.watch(generacionDeSesionProvider);
  // Cuando la lista de grupos se refresca (al crear uno o al pedir unirse) los
  // contadores también: dependen de lo mismo.
  ref.watch(misGruposProvider);
  try {
    final r = await DefaultApi(ref.read(dioProvider)).consultarMisLimites();
    return r.data!;
  } catch (e) {
    throw errorDeDominio(e);
  }
});

extension Topes on MisLimites {
  /// Llegó al tope de grupos que organiza a la vez.
  bool get organizarAlTope {
    final tope = limiteDeOrganizar;
    return tope != null && organizados >= tope;
  }

  /// Llegó al tope de grupos en los que participa sin organizar.
  bool get participarAlTope => participaciones >= limiteDeParticipar;
}
