import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// Dónde está la votación del traspaso de un cupo (CU-63/CU-64). El grupo **siempre**
/// vota el traspaso (decisión 2026-10-30): sin una votación aprobada sobre ESTE cupo,
/// el servidor lo rechaza.
sealed class VotacionDelCupo {
  const VotacionDelCupo();
}

/// Nadie la pidió todavía (o la última venció o se rechazó).
final class SinVotacion extends VotacionDelCupo {
  const SinVotacion();
}

/// Abierta: el grupo está votando.
final class VotacionAbierta extends VotacionDelCupo {
  const VotacionAbierta(this.cierraEn);
  final DateTime cierraEn;
}

/// Aprobada y sin usar: autoriza el traspaso.
final class VotacionAprobada extends VotacionDelCupo {
  const VotacionAprobada(this.acuerdoId);
  final String acuerdoId;
}

/// El cupo y quién entra: el grupo vota el traspaso a ESA persona (decisión 2026-10-30).
typedef CupoDelGrupo = ({
  String grupoId,
  String cupoId,
  String? usuarioEntranteId,
});

/// La votación del cupo, leída del servidor: manda la aprobada para quien entra; si no, la
/// abierta (hay una sola abierta por cupo, sea para quien sea).
final votacionDelCupoProvider = FutureProvider.autoDispose
    .family<VotacionDelCupo, CupoDelGrupo>((ref, c) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(ref.read(dioProvider)).listarAcuerdos(
          grupoId: c.grupoId,
          tipo: 'TRASPASO_CUPO',
          referenciaAfectadaId: c.cupoId,
        );
        final acuerdos = r.data ?? const <AcuerdoDelGrupo>[];
        final aprobada = acuerdos
            .where(
              (a) =>
                  a.estado == AcuerdoDelGrupoEstadoEnum.APROBADO &&
                  a.usuarioEntranteId != null &&
                  a.usuarioEntranteId == c.usuarioEntranteId,
            )
            .firstOrNull;
        if (aprobada != null) return VotacionAprobada(aprobada.acuerdoId);
        final abierta = acuerdos
            .where((a) => a.estado == AcuerdoDelGrupoEstadoEnum.ABIERTO)
            .firstOrNull;
        if (abierta != null) return VotacionAbierta(abierta.cierraEn);
        return const SinVotacion();
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Pedir la votación del traspaso: abre un acuerdo TRASPASO_CUPO sobre el cupo y quien
/// entra. Una clave de idempotencia por cupo y persona: tocar dos veces no abre dos
/// votaciones.
class PedirVotacion extends AsyncNotifier<void> {
  @override
  Future<void> build() async {
    ref.watch(generacionDeSesionProvider);
  }

  Future<void> pedir(CupoDelGrupo c, String descripcion) async {
    if (state.isLoading) return;
    final claves = ref.read(idempotenciaProvider.notifier);
    final formulario = 'cu63-votar-traspaso-${c.cupoId}-${c.usuarioEntranteId}';
    state = const AsyncLoading<void>();
    try {
      await DefaultApi(ref.read(dioProvider)).proponerAcuerdo(
        idempotencyKey: claves.claveDe(formulario),
        entradaAcuerdo: EntradaAcuerdo(
          grupoId: c.grupoId,
          tipo: EntradaAcuerdoTipoEnum.TRASPASO_CUPO,
          descripcion: descripcion,
          referenciaAfectadaId: c.cupoId,
          usuarioEntranteId: c.usuarioEntranteId,
        ),
      );
      claves.cerrar(formulario);
      ref.invalidate(votacionDelCupoProvider(c));
      state = const AsyncData(null);
    } catch (e) {
      final error = errorDeDominio(e);
      if (error is ErrorDeApi && error.estado == 422) claves.cerrar(formulario);
      state = AsyncError(error, StackTrace.current);
    }
  }
}

final pedirVotacionProvider =
    AsyncNotifierProvider.autoDispose<PedirVotacion, void>(PedirVotacion.new);
