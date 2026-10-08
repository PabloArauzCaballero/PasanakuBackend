//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_pago.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPago {
  /// Returns a new [EntradaPago] instance.
  EntradaPago({

    required  this.monto,

    required  this.moneda,

    required  this.formaPago,

    required  this.autorizadoPor,
  });

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


  final EntradaPagoMonedaEnum moneda;



  @JsonKey(
    
    name: r'formaPago',
    required: true,
    includeIfNull: false,
  )


  final EntradaPagoFormaPagoEnum formaPago;



      /// Distinto de quien aprobo la factura (R-CTB-05)
  @JsonKey(
    
    name: r'autorizadoPor',
    required: true,
    includeIfNull: false,
  )


  final String autorizadoPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPago &&
      other.monto == monto &&
      other.moneda == moneda &&
      other.formaPago == formaPago &&
      other.autorizadoPor == autorizadoPor;

    @override
    int get hashCode =>
        monto.hashCode +
        moneda.hashCode +
        formaPago.hashCode +
        autorizadoPor.hashCode;

  factory EntradaPago.fromJson(Map<String, dynamic> json) => _$EntradaPagoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPagoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPagoMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaPagoMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaPagoFormaPagoEnum {
@JsonValue(r'TRANSFERENCIA')
TRANSFERENCIA(r'TRANSFERENCIA'),
@JsonValue(r'CHEQUE')
CHEQUE(r'CHEQUE'),
@JsonValue(r'EFECTIVO')
EFECTIVO(r'EFECTIVO'),
@JsonValue(r'QR')
QR(r'QR');

const EntradaPagoFormaPagoEnum(this.value);

final String value;

@override
String toString() => value;
}


