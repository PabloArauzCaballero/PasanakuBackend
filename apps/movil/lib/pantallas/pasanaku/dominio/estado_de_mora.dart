import 'package:aportaya_cliente_aportes/aportaya_cliente_aportes.dart'
    as aportes;
import 'package:aportaya_cliente_garantia/aportaya_cliente_garantia.dart'
    as garantia;
import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart' as grupos;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';

/// El estado de aportes del participante (`aportes.consultarEstadoDelParticipante`)
/// y su restricción vigente en la lista interna (`garantia.consultarRestriccion`),
/// **solo con los hechos que el backend guarda** (`alDia`, `deudaVigente`,
/// `montoQueLaLevanta`): ni esta pantalla ni ninguna otra de este carril muestran
/// una probabilidad de incumplimiento como si fuera un hecho (skill
/// `alertas-riesgo-temprano`, gate propio de la ficha F5). Ninguno de los dos
/// contratos expone una fecha límite ni días de mora por participante — no se
/// inventa ese número; se muestra lo que hay (deuda vigente, si está al día, cuánto
/// levantaría la restricción) y se declara el hueco en el informe.
final estadoDelParticipanteProvider = FutureProvider.autoDispose
    .family<aportes.EstadoDelParticipante, String>((ref, participanteId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await aportes.DefaultApi(
          dio,
        ).consultarEstadoDelParticipante(participanteId: participanteId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

final restriccionVigenteProvider = FutureProvider.autoDispose
    .family<garantia.RestriccionVigente, String>((ref, usuarioId) async {
      final dio = ref.watch(dioProvider);
      try {
        final r = await garantia.DefaultApi(
          dio,
        ).consultarRestriccion(usuarioId: usuarioId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Quien soy y en qué grupo participo, resuelto con la SESIÓN: la pestaña «Grupos» abre sus pantallas
/// sin ids en la ruta (B36). `participanteId` queda vacío si todavía no participa de ningún grupo.
class MiParticipacion {
  const MiParticipacion({required this.usuarioId, required this.participanteId});
  final String usuarioId;
  final String participanteId;
}

final miParticipacionProvider = FutureProvider.autoDispose<MiParticipacion>((
  ref,
) async {
  final usuarioId = await ref.watch(sesionProvider).usuarioId() ?? '';
  final dio = ref.watch(dioProvider);
  try {
    final r = await grupos.DefaultApi(dio).listarMisParticipaciones();
    final primera = (r.data ?? const <grupos.Participacion>[]).firstOrNull;
    return MiParticipacion(
      usuarioId: usuarioId,
      participanteId: primera?.participanteId ?? '',
    );
  } catch (e) {
    throw errorDeDominio(e);
  }
});
