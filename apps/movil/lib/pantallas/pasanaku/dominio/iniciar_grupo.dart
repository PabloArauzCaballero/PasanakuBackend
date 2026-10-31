import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/idempotencia.dart';
import 'detalle_del_grupo.dart';
import 'mis_grupos.dart';

/// Iniciar el grupo al 75 % (CU-69, `POST /grupos/{id}/inicio`). El servidor decide si
/// alcanza, achica el grupo a los cupos ocupados y lo pasa a `CONFORMADO`; la app solo
/// pide y vuelve a preguntar el detalle y la lista de grupos.
///
/// La clave de idempotencia sobrevive a un fallo de red (el resultado es desconocido:
/// la misma clave devuelve el resultado original) y se cierra con un éxito o con un
/// 422, que es un «no» definitivo: el próximo intento es otra operación.
final iniciarGrupoProvider =
    Provider<Future<GrupoIniciado> Function(String grupoId)>((ref) {
      return (grupoId) async {
        final claves = ref.read(idempotenciaProvider.notifier);
        final formulario = 'cu69-iniciar-$grupoId';
        try {
          final r = await DefaultApi(ref.read(dioProvider)).iniciarGrupo(
            grupoId: grupoId,
            idempotencyKey: claves.claveDe(formulario),
          );
          claves.cerrar(formulario);
          ref.invalidate(detalleDelGrupoProvider(grupoId));
          ref.invalidate(misGruposProvider);
          return r.data!;
        } catch (e) {
          final error = errorDeDominio(e);
          if (error is ErrorDeApi && error.estado == 422) {
            claves.cerrar(formulario);
            // Lo que se ve quedó viejo (alguien entró, firmó o ya se inició).
            ref.invalidate(detalleDelGrupoProvider(grupoId));
          }
          throw error;
        }
      };
    });
