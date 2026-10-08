//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_orden_desembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaOrdenDesembolso {
  /// Returns a new [SalidaOrdenDesembolso] instance.
  SalidaOrdenDesembolso({

    required  this.ordenId,

    required  this.estado,

    required  this.claveIdempotencia,

    required  this.intentos,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'ordenId',
    required: true,
    includeIfNull: false,
  )


  final String ordenId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaOrdenDesembolsoEstadoEnum estado;



  @JsonKey(
    
    name: r'claveIdempotencia',
    required: true,
    includeIfNull: false,
  )


  final String claveIdempotencia;



          // minimum: 0
  @JsonKey(
    
    name: r'intentos',
    required: true,
    includeIfNull: false,
  )


  final int intentos;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaOrdenDesembolso &&
      other.ordenId == ordenId &&
      other.estado == estado &&
      other.claveIdempotencia == claveIdempotencia &&
      other.intentos == intentos &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        ordenId.hashCode +
        estado.hashCode +
        claveIdempotencia.hashCode +
        intentos.hashCode +
        esNueva.hashCode;

  factory SalidaOrdenDesembolso.fromJson(Map<String, dynamic> json) => _$SalidaOrdenDesembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaOrdenDesembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaOrdenDesembolsoEstadoEnum {
@JsonValue(r'CREADA')
CREADA(r'CREADA'),
@JsonValue(r'ENVIADA_A_PROVEEDOR')
ENVIADA_A_PROVEEDOR(r'ENVIADA_A_PROVEEDOR'),
@JsonValue(r'EN_PROCESO')
EN_PROCESO(r'EN_PROCESO'),
@JsonValue(r'ACREDITADA')
ACREDITADA(r'ACREDITADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA'),
@JsonValue(r'DEVUELTA_POR_BANCO')
DEVUELTA_POR_BANCO(r'DEVUELTA_POR_BANCO'),
@JsonValue(r'CANCELADA')
CANCELADA(r'CANCELADA');

const SalidaOrdenDesembolsoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


