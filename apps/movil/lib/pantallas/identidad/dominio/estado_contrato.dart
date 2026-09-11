import 'package:flutter_riverpod/flutter_riverpod.dart';

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
  });

  final bool leidoHastaElFinal;
  final bool aceptaContrato;
  final bool aceptaTarifario;
  final bool aceptaTratamientoDatos;

  bool get puedeAceptar =>
      leidoHastaElFinal &&
      aceptaContrato &&
      aceptaTarifario &&
      aceptaTratamientoDatos;

  EstadoContratoAdhesion copiarCon({
    bool? leidoHastaElFinal,
    bool? aceptaContrato,
    bool? aceptaTarifario,
    bool? aceptaTratamientoDatos,
  }) => EstadoContratoAdhesion(
    leidoHastaElFinal: leidoHastaElFinal ?? this.leidoHastaElFinal,
    aceptaContrato: aceptaContrato ?? this.aceptaContrato,
    aceptaTarifario: aceptaTarifario ?? this.aceptaTarifario,
    aceptaTratamientoDatos:
        aceptaTratamientoDatos ?? this.aceptaTratamientoDatos,
  );
}

class ContratoNotifier extends Notifier<EstadoContratoAdhesion> {
  @override
  EstadoContratoAdhesion build() => const EstadoContratoAdhesion();

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
