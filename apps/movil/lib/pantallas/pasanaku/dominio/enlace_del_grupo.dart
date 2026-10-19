import 'dart:convert';

import 'package:aportaya_cliente_grupos/aportaya_cliente_grupos.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../dominio/errores.dart';
import '../../../proveedores/sesion.dart';
import 'enlace_para_compartir.dart';

/// El enlace del grupo (CU-69, 2026-10-19): un enlace —y su QR— para que cualquiera pida
/// unirse; el organizador aprueba cada pedido. El servidor guarda solo el hash del
/// código, así que la app del organizador lo recuerda en el almacenamiento seguro para
/// volver a mostrarlo (y lo descarta si venció o lo anularon).
/// El enlace que se comparte: `https://<app web>/g/<código>`. Android lo abre en la
/// app instalada (App Links); sin la app, abre la app web en la misma pantalla.
String? enlaceDelGrupo(String codigo, {String? baseWeb}) {
  final base = baseWeb ?? baseDeLaAppWeb(api: baseDelGateway);
  if (base == null || !esCodigoDeEnlaceDelGrupo(codigo)) return null;
  return '$base/g/$codigo';
}

String mensajeDelEnlaceDelGrupo(String enlace, {String? nombreDelGrupo}) {
  final grupo = nombreDelGrupo == null || nombreDelGrupo.trim().isEmpty
      ? 'mi pasanaku'
      : 'mi pasanaku «${nombreDelGrupo.trim()}»';
  return 'Te invito a $grupo en Aporteo. Abrí este enlace y pedí unirte: $enlace';
}

class EnlaceGuardado {
  const EnlaceGuardado({required this.codigo, required this.expiraEn});
  final String codigo;
  final DateTime expiraEn;
}

/// Generar, recordar, verificar y anular el enlace del grupo (organizador).
class EnlacesDelGrupo {
  EnlacesDelGrupo(this._ref);
  final Ref _ref;

  static String _clave(String grupoId) => 'enlace-del-grupo-$grupoId';

  DefaultApi get _api => DefaultApi(_ref.read(dioProvider));

  /// El enlace recordado si sigue vivo; `null` si no hay, venció o lo anularon.
  Future<EnlaceGuardado?> vigente(String grupoId) async {
    final almacen = _ref.read(almacenSeguroProvider);
    final crudo = await almacen.leer(_clave(grupoId));
    if (crudo == null) return null;
    final EnlaceGuardado guardado;
    try {
      final j = jsonDecode(crudo) as Map<String, dynamic>;
      guardado = EnlaceGuardado(
        codigo: j['codigo'] as String,
        expiraEn: DateTime.parse(j['expiraEn'] as String),
      );
    } on Object {
      await almacen.borrar(_clave(grupoId));
      return null;
    }
    if (!guardado.expiraEn.isAfter(DateTime.now()) ||
        !esCodigoDeEnlaceDelGrupo(guardado.codigo)) {
      await almacen.borrar(_clave(grupoId));
      return null;
    }
    try {
      await _api.consultarEnlaceDelGrupo(
        entradaCodigoDeEnlace: EntradaCodigoDeEnlace(codigo: guardado.codigo),
      );
    } catch (e) {
      final error = errorDeDominio(e);
      // Anulado o vencido en el servidor: se olvida. Sin red, se muestra igual.
      if (error is ErrorDeApi && error.codigo == codigoEnlaceInvalido) {
        await almacen.borrar(_clave(grupoId));
        return null;
      }
    }
    return guardado;
  }

  /// Genera un enlace nuevo; el anterior deja de servir.
  Future<EnlaceGuardado> generar(String grupoId) async {
    try {
      final r = await _api.generarEnlaceDelGrupo(grupoId: grupoId);
      final salida = r.data!;
      final guardado = EnlaceGuardado(
        codigo: salida.codigo,
        expiraEn: salida.expiraEn,
      );
      await _ref
          .read(almacenSeguroProvider)
          .guardar(
            _clave(grupoId),
            jsonEncode({
              'codigo': guardado.codigo,
              'expiraEn': guardado.expiraEn.toUtc().toIso8601String(),
            }),
          );
      return guardado;
    } catch (e) {
      throw errorDeDominio(e);
    }
  }

  Future<void> anular(String grupoId) async {
    try {
      await _api.anularEnlaceDelGrupo(grupoId: grupoId);
      await _ref.read(almacenSeguroProvider).borrar(_clave(grupoId));
    } catch (e) {
      throw errorDeDominio(e);
    }
  }
}

const codigoEnlaceInvalido = 'AP-CU69-11';

final enlacesDelGrupoProvider = Provider<EnlacesDelGrupo>(EnlacesDelGrupo.new);

final enlaceVigenteProvider = FutureProvider.autoDispose
    .family<EnlaceGuardado?, String>((ref, grupoId) {
      ref.watch(generacionDeSesionProvider);
      return ref.read(enlacesDelGrupoProvider).vigente(grupoId);
    });

/// Lo que ve quien abre el enlace: el grupo, sin datos de sus integrantes.
final detalleDelEnlaceProvider = FutureProvider.autoDispose
    .family<DetalleEnlaceDelGrupo, String>((ref, codigo) async {
      ref.watch(generacionDeSesionProvider);
      try {
        final r = await DefaultApi(ref.read(dioProvider))
            .consultarEnlaceDelGrupo(
              entradaCodigoDeEnlace: EntradaCodigoDeEnlace(codigo: codigo),
            );
        return r.data!;
      } catch (e) {
        throw errorDeDominio(e);
      }
    });

/// Pedir unirse con el enlace: queda una solicitud PENDIENTE que el organizador aprueba.
final pedirUnirseProvider = Provider<Future<String> Function(String codigo)>((
  ref,
) {
  return (codigo) async {
    try {
      final r = await DefaultApi(ref.read(dioProvider))
          .solicitarIngresoPorEnlace(
            entradaSolicitudPorEnlace: EntradaSolicitudPorEnlace(
              codigo: codigo,
            ),
          );
      return r.data!.solicitudId;
    } catch (e) {
      throw errorDeDominio(e);
    }
  };
});
