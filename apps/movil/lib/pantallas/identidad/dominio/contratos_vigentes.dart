import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../dominio/cliente.dart';

/// CU-05 · `GET /cumplimiento/contratos/vigentes`.
///
/// **Sin esto el alta no existe del lado del servidor.** `aceptaContratos` de CU-01 son
/// UUID, y los ids de `cumplimiento.contrato_adhesion` se generan por entorno: no hay
/// forma de cablearlos ni de adivinarlos. La app mandaba `'ADHESION'`, `'TARIFARIO'` y
/// `'TRATAMIENTO_DATOS'`, el servidor los rechazaba al deserializar, y como nadie
/// llamaba al servidor tampoco se notaba.
///
/// La ruta es publica a proposito: se consulta durante el alta, cuando todavia no hay
/// sesion. No devuelve dato de nadie — es lo mismo que ya esta publicado en el sitio.
class ContratosVigentes {
  ContratosVigentes(this._ref);
  final Ref _ref;

  Future<List<ContratoVigente>> consultar() async {
    try {
      final r = await DefaultApi(
        _ref.read(dioProvider),
      ).listarContratosVigentes();
      return r.data?.toList() ?? const [];
    } on DioException catch (e) {
      throw errorDeDominio(e);
    }
  }
}

final contratosVigentesProvider = Provider<ContratosVigentes>(
  ContratosVigentes.new,
);

/// Lo que la pantalla del contrato mira. Se pide una vez y se cachea: durante un alta
/// el tarifario no cambia, y volver a preguntarlo en cada reconstruccion serian tres
/// llamadas por pantalla.
final contratosDelAltaProvider = FutureProvider<List<ContratoVigente>>(
  (ref) => ref.read(contratosVigentesProvider).consultar(),
);

/// Los tres que el alta exige aceptar, en el orden en que se muestran.
///
/// `TARIFAS` y `TRATAMIENTO_DATOS` son contratos aparte de `BILLETERA` porque los tres
/// consentimientos se dan por separado: aceptar el contrato no es aceptar el tarifario
/// ni autorizar el tratamiento de datos, y auditar cual se dio exige poder mirarlos uno
/// por uno.
const tiposDelAlta = [
  ContratoVigenteTipoEnum.BILLETERA,
  ContratoVigenteTipoEnum.TARIFAS,
  ContratoVigenteTipoEnum.TRATAMIENTO_DATOS,
];

extension ContratosDelAlta on List<ContratoVigente> {
  /// El vigente de ese tipo, o `null` si el servidor no publica ninguno.
  ContratoVigente? deTipo(ContratoVigenteTipoEnum tipo) {
    for (final c in this) {
      if (c.tipo == tipo) return c;
    }
    return null;
  }

  /// `true` cuando estan los tres. Sin los tres no se puede completar el alta, y es
  /// mejor decirlo que dejar a alguien firmando dos de tres sin saberlo.
  bool get tieneLosDelAlta => tiposDelAlta.every((t) => deTipo(t) != null);
}
