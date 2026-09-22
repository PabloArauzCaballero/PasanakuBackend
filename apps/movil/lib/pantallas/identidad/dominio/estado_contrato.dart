import 'package:aportaya_cliente_cumplimiento/aportaya_cliente_cumplimiento.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'contratos_vigentes.dart';

/// CU-05 — el contrato se muestra ENTERO antes de poder aceptar (gate del carril).
/// `leidoHastaElFinal` lo pone la pantalla cuando el `ScrollController` llega al
/// final; el botón de aceptar queda deshabilitado hasta entonces, y con los tres
/// consentimientos por separado (D-1: contrato, tarifario, datos personales).
class EstadoContratoAdhesion {
  const EstadoContratoAdhesion({
    this.leidoHastaElFinal = false,
    this.aceptaContrato = false,
    this.aceptaTarifario = false,
    this.aceptaTratamientoDatos = false,
    this.vigentes = const [],
  });

  final bool leidoHastaElFinal;
  final bool aceptaContrato;
  final bool aceptaTarifario;
  final bool aceptaTratamientoDatos;

  /// Los contratos que el servidor publica como vigentes. Llegan de
  /// `GET /cumplimiento/contratos/vigentes` y son lo que convierte tres casillas en
  /// tres ids que el alta puede mandar.
  final List<ContratoVigente> vigentes;

  /// Los ids que el alta manda al servidor como `aceptaContratos`.
  ///
  /// **Son UUID, no códigos.** Antes esto devolvía `['ADHESION', 'TARIFARIO',
  /// 'TRATAMIENTO_DATOS']`: nombres inventados acá, mientras el contrato de CU-01 los
  /// declara `format: uuid`. El servidor los rechazaba al deserializar, y como el alta
  /// nunca llegaba a llamarlo, nadie se enteraba. Cada casilla marcada aporta el id del
  /// contrato vigente de su tipo, que es lo único que permite auditar después cuál se
  /// aceptó y contra qué texto.
  ///
  /// Los tres consentimientos siguen siendo por separado a propósito: aceptar el
  /// contrato no es lo mismo que aceptar el tarifario ni que autorizar el tratamiento
  /// de datos.
  List<String> get aceptados => [
    if (aceptaContrato) ?vigentes.deTipo(ContratoVigenteTipoEnum.BILLETERA)?.id,
    if (aceptaTarifario) ?vigentes.deTipo(ContratoVigenteTipoEnum.TARIFAS)?.id,
    if (aceptaTratamientoDatos)
      ?vigentes.deTipo(ContratoVigenteTipoEnum.TRATAMIENTO_DATOS)?.id,
  ];

  bool get puedeAceptar =>
      leidoHastaElFinal &&
      aceptaContrato &&
      aceptaTarifario &&
      aceptaTratamientoDatos &&
      // Sin los tres ids no hay alta posible. Dejar aceptar igual sería hacer leer un
      // contrato entero para después fallar al enviarlo.
      vigentes.tieneLosDelAlta;

  EstadoContratoAdhesion copiarCon({
    bool? leidoHastaElFinal,
    bool? aceptaContrato,
    bool? aceptaTarifario,
    bool? aceptaTratamientoDatos,
    List<ContratoVigente>? vigentes,
  }) => EstadoContratoAdhesion(
    leidoHastaElFinal: leidoHastaElFinal ?? this.leidoHastaElFinal,
    aceptaContrato: aceptaContrato ?? this.aceptaContrato,
    aceptaTarifario: aceptaTarifario ?? this.aceptaTarifario,
    aceptaTratamientoDatos:
        aceptaTratamientoDatos ?? this.aceptaTratamientoDatos,
    vigentes: vigentes ?? this.vigentes,
  );
}

class ContratoNotifier extends Notifier<EstadoContratoAdhesion> {
  @override
  EstadoContratoAdhesion build() => const EstadoContratoAdhesion();

  /// Los consentimientos vuelven a cero al cerrar el alta: nadie hereda la
  /// aceptación de otra persona por no haber recargado la app.
  void reiniciar() => state = const EstadoContratoAdhesion();

  /// Lo que publicó el servidor. La pantalla lo fija cuando la consulta responde.
  void fijarVigentes(List<ContratoVigente> vigentes) =>
      state = state.copiarCon(vigentes: vigentes);

  void marcarLeidoHastaElFinal() =>
      state = state.copiarCon(leidoHastaElFinal: true);

  void alternarContrato(bool v) => state = state.copiarCon(aceptaContrato: v);
  void alternarTarifario(bool v) => state = state.copiarCon(aceptaTarifario: v);
  void alternarTratamientoDatos(bool v) =>
      state = state.copiarCon(aceptaTratamientoDatos: v);
}

final contratoProvider =
    NotifierProvider<ContratoNotifier, EstadoContratoAdhesion>(
      ContratoNotifier.new,
    );
