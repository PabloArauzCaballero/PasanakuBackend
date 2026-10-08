//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_liquidacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaLiquidacion {
  /// Returns a new [EntradaLiquidacion] instance.
  EntradaLiquidacion({

    required  this.diasSinCerrar,

    required  this.excepcionesAbiertas,

    required  this.saldoDeLaCuentaDeIngresos,
  });

      /// de nucleo-financiero; este servicio no lee ese esquema
          // minimum: 0
  @JsonKey(
    
    name: r'diasSinCerrar',
    required: true,
    includeIfNull: false,
  )


  final int diasSinCerrar;



      /// de aportes; idem
          // minimum: 0
  @JsonKey(
    
    name: r'excepcionesAbiertas',
    required: true,
    includeIfNull: false,
  )


  final int excepcionesAbiertas;



  @JsonKey(
    
    name: r'saldoDeLaCuentaDeIngresos',
    required: true,
    includeIfNull: false,
  )


  final String saldoDeLaCuentaDeIngresos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaLiquidacion &&
      other.diasSinCerrar == diasSinCerrar &&
      other.excepcionesAbiertas == excepcionesAbiertas &&
      other.saldoDeLaCuentaDeIngresos == saldoDeLaCuentaDeIngresos;

    @override
    int get hashCode =>
        diasSinCerrar.hashCode +
        excepcionesAbiertas.hashCode +
        saldoDeLaCuentaDeIngresos.hashCode;

  factory EntradaLiquidacion.fromJson(Map<String, dynamic> json) => _$EntradaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

