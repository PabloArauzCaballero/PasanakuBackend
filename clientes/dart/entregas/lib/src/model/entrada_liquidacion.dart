//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
import 'package:aportaya_cliente_entregas/src/model/deduccion_entrega.dart';
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

    required  this.grupoId,

    required  this.periodoId,

    required  this.turnoId,

    required  this.cupoId,

    required  this.beneficiarioId,

    required  this.bruto,

    required  this.recaudado,

    required  this.deducciones,

    required  this.metodoDesembolso,

    required  this.fechaProgramada,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'periodoId',
    required: true,
    includeIfNull: false,
  )


  final String periodoId;



  @JsonKey(
    
    name: r'turnoId',
    required: true,
    includeIfNull: false,
  )


  final String turnoId;



  @JsonKey(
    
    name: r'cupoId',
    required: true,
    includeIfNull: false,
  )


  final String cupoId;



  @JsonKey(
    
    name: r'beneficiarioId',
    required: true,
    includeIfNull: false,
  )


  final String beneficiarioId;



  @JsonKey(
    
    name: r'bruto',
    required: true,
    includeIfNull: false,
  )


  final Dinero bruto;



  @JsonKey(
    
    name: r'recaudado',
    required: true,
    includeIfNull: false,
  )


  final Dinero recaudado;



  @JsonKey(
    
    name: r'deducciones',
    required: true,
    includeIfNull: false,
  )


  final List<DeduccionEntrega> deducciones;



  @JsonKey(
    
    name: r'metodoDesembolso',
    required: true,
    includeIfNull: false,
  )


  final EntradaLiquidacionMetodoDesembolsoEnum metodoDesembolso;



  @JsonKey(
    
    name: r'fechaProgramada',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaProgramada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaLiquidacion &&
      other.grupoId == grupoId &&
      other.periodoId == periodoId &&
      other.turnoId == turnoId &&
      other.cupoId == cupoId &&
      other.beneficiarioId == beneficiarioId &&
      other.bruto == bruto &&
      other.recaudado == recaudado &&
      other.deducciones == deducciones &&
      other.metodoDesembolso == metodoDesembolso &&
      other.fechaProgramada == fechaProgramada;

    @override
    int get hashCode =>
        grupoId.hashCode +
        periodoId.hashCode +
        turnoId.hashCode +
        cupoId.hashCode +
        beneficiarioId.hashCode +
        bruto.hashCode +
        recaudado.hashCode +
        deducciones.hashCode +
        metodoDesembolso.hashCode +
        fechaProgramada.hashCode;

  factory EntradaLiquidacion.fromJson(Map<String, dynamic> json) => _$EntradaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaLiquidacionMetodoDesembolsoEnum {
@JsonValue(r'BILLETERA_MOVIL')
BILLETERA_MOVIL(r'BILLETERA_MOVIL'),
@JsonValue(r'COMPENSACION_INTERNA')
COMPENSACION_INTERNA(r'COMPENSACION_INTERNA'),
@JsonValue(r'EFECTIVO_ORGANIZADOR')
EFECTIVO_ORGANIZADOR(r'EFECTIVO_ORGANIZADOR'),
@JsonValue(r'QR_ENVIO')
QR_ENVIO(r'QR_ENVIO'),
@JsonValue(r'TRANSFERENCIA_BANCARIA')
TRANSFERENCIA_BANCARIA(r'TRANSFERENCIA_BANCARIA');

const EntradaLiquidacionMetodoDesembolsoEnum(this.value);

final String value;

@override
String toString() => value;
}


