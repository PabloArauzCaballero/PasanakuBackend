//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
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

    required  this.entregaId,

    required  this.bruto,

    required  this.totalDeducciones,

    required  this.neto,

    required  this.estado,
  });

  @JsonKey(
    
    name: r'entregaId',
    required: true,
    includeIfNull: false,
  )


  final String entregaId;



  @JsonKey(
    
    name: r'bruto',
    required: true,
    includeIfNull: false,
  )


  final Dinero bruto;



  @JsonKey(
    
    name: r'totalDeducciones',
    required: true,
    includeIfNull: false,
  )


  final Dinero totalDeducciones;



  @JsonKey(
    
    name: r'neto',
    required: true,
    includeIfNull: false,
  )


  final Dinero neto;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaLiquidacion &&
      other.entregaId == entregaId &&
      other.bruto == bruto &&
      other.totalDeducciones == totalDeducciones &&
      other.neto == neto &&
      other.estado == estado;

    @override
    int get hashCode =>
        entregaId.hashCode +
        bruto.hashCode +
        totalDeducciones.hashCode +
        neto.hashCode +
        estado.hashCode;

  factory SalidaLiquidacion.fromJson(Map<String, dynamic> json) => _$SalidaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

