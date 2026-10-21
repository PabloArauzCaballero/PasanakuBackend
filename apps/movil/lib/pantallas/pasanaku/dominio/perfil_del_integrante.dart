import 'package:aportaya_cliente_identidad/aportaya_cliente_identidad.dart'
    as identidad;
import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart'
    as transparencia;
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/sesion.dart';

/// De quién es el perfil y desde qué grupo se mira: el servidor solo lo muestra
/// a quien comparte ese grupo (decisión del dueño, 2026-10-21).
typedef IntegranteDelGrupo = ({String grupoId, String usuarioId});

/// El teléfono del integrante. `null` cuando el servidor responde 403: quien mira
/// no comparte el grupo (o no se pudo comprobar) y la pantalla lo dice tal cual,
/// sin ofrecer reintentar algo que no va a cambiar.
final contactoDelIntegranteProvider = FutureProvider.autoDispose
    .family<identidad.ContactoEnGrupo?, IntegranteDelGrupo>((ref, i) async {
      ref.watch(generacionDeSesionProvider);
      final dio = ref.watch(dioProvider);
      try {
        final r = await identidad.DefaultApi(
          dio,
        ).consultarContactoEnGrupo(grupoId: i.grupoId, usuarioId: i.usuarioId);
        return r.data!;
      } catch (e) {
        return _nuloSiNoCorresponde(errorDeDominio(e));
      }
    });

/// La reputación del integrante, tal como `transparencia` la calcula. Sin
/// historial no es un error ni un cero: es `tieneHistorial: false` (CU-71).
final reputacionDelIntegranteProvider = FutureProvider.autoDispose
    .family<transparencia.PerfilDeReputacionEnGrupo?, IntegranteDelGrupo>((
      ref,
      i,
    ) async {
      ref.watch(generacionDeSesionProvider);
      final dio = ref.watch(dioProvider);
      try {
        final r = await transparencia.DefaultApi(
          dio,
        ).consultarPerfilEnGrupo(usuarioId: i.usuarioId, grupoId: i.grupoId);
        return r.data!;
      } catch (e) {
        return _nuloSiNoCorresponde(errorDeDominio(e));
      }
    });

Null _nuloSiNoCorresponde(Object error) {
  if (error is ErrorDeApi && error.estado == 403) return null;
  throw error;
}
