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

    required  this.facturaPublicidadId,

    required  this.montoTotal,

    required  this.moneda,

    required  this.estado,

     this.cuentaPorCobrarId,
  });

  @JsonKey(
    
    name: r'facturaPublicidadId',
    required: true,
    includeIfNull: false,
  )


  final String facturaPublicidadId;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'montoTotal',
    required: true,
    includeIfNull: false,
  )


  final String montoTotal;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final SalidaLiquidacionMonedaEnum moneda;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaLiquidacionEstadoEnum estado;



  @JsonKey(
    
    name: r'cuentaPorCobrarId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaPorCobrarId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaLiquidacion &&
      other.facturaPublicidadId == facturaPublicidadId &&
      other.montoTotal == montoTotal &&
      other.moneda == moneda &&
      other.estado == estado &&
      other.cuentaPorCobrarId == cuentaPorCobrarId;

    @override
    int get hashCode =>
        facturaPublicidadId.hashCode +
        montoTotal.hashCode +
        moneda.hashCode +
        estado.hashCode +
        (cuentaPorCobrarId == null ? 0 : cuentaPorCobrarId.hashCode);

  factory SalidaLiquidacion.fromJson(Map<String, dynamic> json) => _$SalidaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaLiquidacionMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const SalidaLiquidacionMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaLiquidacionEstadoEnum {
@JsonValue(r'GENERADA')
GENERADA(r'GENERADA'),
@JsonValue(r'FACTURADA')
FACTURADA(r'FACTURADA'),
@JsonValue(r'COBRADA')
COBRADA(r'COBRADA'),
@JsonValue(r'ANULADA')
ANULADA(r'ANULADA');

const SalidaLiquidacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


