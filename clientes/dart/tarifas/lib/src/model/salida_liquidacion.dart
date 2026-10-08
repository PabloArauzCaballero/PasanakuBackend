//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_liquidacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaLiquidacion {
  /// Returns a new [SalidaLiquidacion] instance.
  SalidaLiquidacion({

    required  this.liquidacionId,

    required  this.periodo,

    required  this.totalCobrado,

    required  this.ingresoNeto,

    required  this.cuadraContraMayor,

    required  this.yaExistia,
  });

  @JsonKey(
    
    name: r'liquidacionId',
    required: true,
    includeIfNull: false,
  )


  final String liquidacionId;



  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



  @JsonKey(
    
    name: r'totalCobrado',
    required: true,
    includeIfNull: false,
  )


  final String totalCobrado;



  @JsonKey(
    
    name: r'ingresoNeto',
    required: true,
    includeIfNull: false,
  )


  final String ingresoNeto;



  @JsonKey(
    
    name: r'cuadraContraMayor',
    required: true,
    includeIfNull: false,
  )


  final bool cuadraContraMayor;



  @JsonKey(
    
    name: r'yaExistia',
    required: true,
    includeIfNull: false,
  )


  final bool yaExistia;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaLiquidacion &&
      other.liquidacionId == liquidacionId &&
      other.periodo == periodo &&
      other.totalCobrado == totalCobrado &&
      other.ingresoNeto == ingresoNeto &&
      other.cuadraContraMayor == cuadraContraMayor &&
      other.yaExistia == yaExistia;

    @override
    int get hashCode =>
        liquidacionId.hashCode +
        periodo.hashCode +
        totalCobrado.hashCode +
        ingresoNeto.hashCode +
        cuadraContraMayor.hashCode +
        yaExistia.hashCode;

  factory SalidaLiquidacion.fromJson(Map<String, dynamic> json) => _$SalidaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

