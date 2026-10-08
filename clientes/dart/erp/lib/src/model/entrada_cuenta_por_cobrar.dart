//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cuenta_por_cobrar.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCuentaPorCobrar {
  /// Returns a new [EntradaCuentaPorCobrar] instance.
  EntradaCuentaPorCobrar({

    required  this.origenTipo,

    required  this.origenId,

    required  this.terceroComercialId,

    required  this.monto,

    required  this.moneda,

    required  this.fechaVencimiento,
  });

  @JsonKey(
    
    name: r'origenTipo',
    required: true,
    includeIfNull: false,
  )


  final String origenTipo;



  @JsonKey(
    
    name: r'origenId',
    required: true,
    includeIfNull: false,
  )


  final String origenId;



  @JsonKey(
    
    name: r'terceroComercialId',
    required: true,
    includeIfNull: false,
  )


  final String terceroComercialId;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaCuentaPorCobrarMonedaEnum moneda;



  @JsonKey(
    
    name: r'fechaVencimiento',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaVencimiento;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCuentaPorCobrar &&
      other.origenTipo == origenTipo &&
      other.origenId == origenId &&
      other.terceroComercialId == terceroComercialId &&
      other.monto == monto &&
      other.moneda == moneda &&
      other.fechaVencimiento == fechaVencimiento;

    @override
    int get hashCode =>
        origenTipo.hashCode +
        origenId.hashCode +
        terceroComercialId.hashCode +
        monto.hashCode +
        moneda.hashCode +
        fechaVencimiento.hashCode;

  factory EntradaCuentaPorCobrar.fromJson(Map<String, dynamic> json) => _$EntradaCuentaPorCobrarFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCuentaPorCobrarToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCuentaPorCobrarMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaCuentaPorCobrarMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


