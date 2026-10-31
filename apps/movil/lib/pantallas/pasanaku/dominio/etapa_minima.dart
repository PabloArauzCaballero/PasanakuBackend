import 'package:aportaya_cliente_transparencia/aportaya_cliente_transparencia.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';
import '../../../proveedores/sesion.dart';
import 'consultar_puntaje.dart';

/// Lo que el formulario de crear grupo puede ofrecer como etapa mínima: las etapas
/// del catálogo hasta la propia, incluida, y cuál es la propia (la que vale si la
/// persona no elige nada).
class OpcionesDeEtapa {
  const OpcionesDeEtapa({required this.elegibles, required this.propia});
  final List<EtapaDelCatalogo> elegibles;
  final EtapaDelCatalogo? propia;
}

/// Regla de la pantalla, pura para probarla sin red: nadie pide una etapa más alta que
/// la suya (`AP-CU20-08`). Sin etapa propia conocida no se ofrece nada: no se puede
/// saber el techo, y el servidor usa la etapa actual de quien organiza.
OpcionesDeEtapa opcionesDeEtapa(
  List<EtapaDelCatalogo> catalogo,
  String? codigoPropio,
) {
  final ordenadas = [...catalogo]..sort((a, b) => a.orden.compareTo(b.orden));
  final propia = ordenadas.where((a) => a.codigo == codigoPropio).firstOrNull;
  if (propia == null) {
    return const OpcionesDeEtapa(elegibles: [], propia: null);
  }
  return OpcionesDeEtapa(
    elegibles: [
      for (final a in ordenadas)
        if (a.orden <= propia.orden) a,
    ],
    propia: propia,
  );
}

/// El catálogo cerrado (`GET /reputacion/etapas`) y la etapa propia, que viene en el
/// puntaje (`consultarPuntaje.etapa`; sin historial es la del puntaje base, nunca la
/// del cero). Lo calcula `transparencia`; la app solo recorta por encima de la propia.
final opcionesDeEtapaProvider = FutureProvider.autoDispose<OpcionesDeEtapa>((
  ref,
) async {
  ref.watch(generacionDeSesionProvider);
  final usuarioId = await ref.watch(sesionProvider).usuarioId();
  try {
    final catalogo = await DefaultApi(ref.read(dioProvider)).listarEtapas();
    final puntaje = usuarioId == null
        ? null
        : await ref.watch(puntajeProvider(usuarioId).future);
    return opcionesDeEtapa(catalogo.data ?? const [], puntaje?.etapa?.codigo);
  } catch (e) {
    throw errorDeDominio(e);
  }
});

/// La etapa que la persona eligió en el formulario; `null` mientras no toque nada, y
/// entonces el servidor usa la suya (contrato de `crearGrupo.etapaMinima`). La mantiene
/// viva `CrearGrupo` mientras la pantalla exista; al salir se olvida (`autoDispose`).
class EtapaMinimaElegida extends Notifier<String?> {
  @override
  String? build() => null;

  void elegir(String codigo) => state = codigo;
}

final etapaMinimaElegidaProvider =
    NotifierProvider.autoDispose<EtapaMinimaElegida, String?>(
      EtapaMinimaElegida.new,
    );
