import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// El grupo visto por dentro (2026-10-20): resumen, una fila por cupo con nombre,
/// estado y turno, si me toca firmar y si puedo sortear. Lo decide el servidor; la app
/// solo pinta lo que viene.
final detalleDelGrupoProvider = FutureProvider.autoDispose
    .family<DetalleDelGrupo, String>((ref, grupoId) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(
          ref.read(dioProvider),
        ).consultarDetalleDelGrupo(grupoId: grupoId);
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Firmar el reglamento: se manda el hash del texto que la persona leyó. Devuelve si
/// con esta firma el grupo quedó completo.
final firmarReglamentoProvider =
    Provider<Future<bool> Function(String grupoId, String hash)>((ref) {
      return (grupoId, hash) async {
        try {
          final r = await DefaultApi(ref.read(dioProvider))
              .firmarReglamentoDelGrupo(
                grupoId: grupoId,
                entradaFirmaDeReglamento: EntradaFirmaDeReglamento(
                  hashReglamento: hash,
                  aceptaReglamento: true,
                ),
              );
          return r.data!.grupoConformado;
        } catch (e) {
          throw errorDeDominio(e);
        }
      };
    });

/// CU-60 en un toque del organizador: compromete (publica solo el hash) y revela con
/// la semilla sellada en el servidor. Las dos fases siguen separadas del lado del
/// servidor, que es donde importa. Devuelve los cupos en el orden de cobro.
final sortearTurnosProvider =
    Provider<Future<List<String>> Function(String grupoId, String? sorteoId)>((
      ref,
    ) {
      return (grupoId, sorteoId) async {
        final api = DefaultApi(ref.read(dioProvider));
        final claves = ref.read(idempotenciaProvider.notifier);
        try {
          var id = sorteoId;
          if (id == null) {
            final formulario = 'cu60-comprometer-$grupoId';
            final c = await api.comprometerSorteo(
              grupoId: grupoId,
              idempotencyKey: claves.claveDe(formulario),
            );
            claves.cerrar(formulario);
            id = c.data!.sorteoId;
          }
          final formulario = 'cu60-revelar-$id';
          final r = await api.revelarSorteo(
            grupoId: grupoId,
            idempotencyKey: claves.claveDe(formulario),
            entradaRevelacion: EntradaRevelacion(sorteoId: id),
          );
          claves.cerrar(formulario);
          return r.data!.cuposEnOrden;
        } catch (e) {
          throw errorDeDominio(e);
        }
      };
    });
