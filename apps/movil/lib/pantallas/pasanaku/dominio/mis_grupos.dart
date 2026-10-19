import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';

/// Un grupo en el que participo, tal como lo devuelve `listarMisParticipaciones`.
///
/// `nombreDelGrupo`, `montoAporte`, `moneda` y `esOrganizador` son campos nuevos y
/// opcionales del contrato de `grupos`: todavía no están en el cliente Dart generado de
/// esta rama, así que se leen del JSON con el mismo `Dio` de la app. Si el servidor
/// todavía no los manda, la tarjeta se arma igual con lo que hay.
class GrupoMio {
  const GrupoMio({
    required this.grupoId,
    required this.participanteId,
    required this.estado,
    this.nombre,
    this.montoAporte,
    this.moneda,
    this.esOrganizador = false,
  });

  final String grupoId;
  final String participanteId;
  final String estado;
  final String? nombre;

  /// Cadena decimal (nunca `double`), como la manda el servidor.
  final String? montoAporte;
  final String? moneda;
  final bool esOrganizador;

  static GrupoMio desdeJson(Map<String, dynamic> j) => GrupoMio(
    grupoId: j['grupoId'] as String,
    participanteId: j['participanteId'] as String,
    estado: j['estado'] as String,
    nombre: j['nombreDelGrupo'] as String?,
    montoAporte: j['montoAporte'] as String?,
    moneda: j['moneda'] as String?,
    esOrganizador: j['esOrganizador'] == true,
  );
}

/// Todos mis grupos vigentes, el más reciente primero (orden del servidor).
final misGruposProvider = FutureProvider.autoDispose<List<GrupoMio>>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  try {
    final r = await ref
        .read(dioProvider)
        .get<List<dynamic>>('/grupos/participaciones');
    return [
      for (final j in r.data ?? const <dynamic>[])
        GrupoMio.desdeJson(j as Map<String, dynamic>),
    ];
  } catch (e) {
    throw errorDeDominio(e);
  }
});
