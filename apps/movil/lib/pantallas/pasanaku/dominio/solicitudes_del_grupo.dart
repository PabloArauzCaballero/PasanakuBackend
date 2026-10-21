import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/idempotencia.dart';
import '../../../proveedores/sesion.dart';

/// Las solicitudes de ingreso que ve y decide el organizador (CU-68). Llegan, entre
/// otras, por el enlace del grupo (2026-10-19), con el nombre visible de quien pide.
/// Las solicitudes pendientes del grupo (organizador), la más antigua primero.
final solicitudesDelGrupoProvider = FutureProvider.autoDispose
    .family<List<SolicitudDeIngreso>, String>((ref, grupoId) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(
          ref.read(dioProvider),
        ).listarSolicitudesDeIngreso(grupoId: grupoId);
        return r.data ?? const [];
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Aceptar o rechazar una solicitud. Cada decisión lleva su clave de idempotencia.
final decidirSolicitudProvider =
    Provider<
      Future<void> Function(String solicitudId, {required bool aceptar})
    >((ref) {
      return (solicitudId, {required aceptar}) async {
        final formulario = 'cu68-decidir-$solicitudId';
        final claves = ref.read(idempotenciaProvider.notifier);
        try {
          await DefaultApi(ref.read(dioProvider)).decidirSolicitudDeIngreso(
            solicitudId: solicitudId,
            idempotencyKey: claves.claveDe(formulario),
            entradaDecisionDeIngreso: EntradaDecisionDeIngreso(
              decision: aceptar
                  ? EntradaDecisionDeIngresoDecisionEnum.ACEPTAR
                  : EntradaDecisionDeIngresoDecisionEnum.RECHAZAR,
              motivo: aceptar ? null : 'El organizador no aceptó la solicitud.',
            ),
          );
          claves.cerrar(formulario);
        } catch (e) {
          throw errorDeDominio(e);
        }
      };
    });
