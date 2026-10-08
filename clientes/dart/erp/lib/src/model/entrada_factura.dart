//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_factura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaFactura {
  /// Returns a new [EntradaFactura] instance.
  EntradaFactura({

    required  this.terceroComercialId,

     this.ordenCompraId,

     this.centroCostoId,

    required  this.numeroFactura,

    required  this.fechaEmision,

    required  this.fechaVencimiento,

    required  this.monto,

    required  this.moneda,

     this.cuentaGastoId,

     this.aprobadaPor,
  });

  @JsonKey(
    
    name: r'terceroComercialId',
    required: true,
    includeIfNull: false,
  )


  final String terceroComercialId;



  @JsonKey(
    
    name: r'ordenCompraId',
    required: false,
    includeIfNull: false,
  )


  final String? ordenCompraId;



  @JsonKey(
    
    name: r'centroCostoId',
    required: false,
    includeIfNull: false,
  )


  final String? centroCostoId;



  @JsonKey(
    
    name: r'numeroFactura',
    required: true,
    includeIfNull: false,
  )


  final String numeroFactura;



  @JsonKey(
    
    name: r'fechaEmision',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaEmision;



  @JsonKey(
    
    name: r'fechaVencimiento',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaVencimiento;



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


  final EntradaFacturaMonedaEnum moneda;



  @JsonKey(
    
    name: r'cuentaGastoId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaGastoId;



      /// Quien aprueba, en el alta. La tabla es append-only: no hay aprobacion posterior.
  @JsonKey(
    
    name: r'aprobadaPor',
    required: false,
    includeIfNull: false,
  )


  final String? aprobadaPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaFactura &&
      other.terceroComercialId == terceroComercialId &&
      other.ordenCompraId == ordenCompraId &&
      other.centroCostoId == centroCostoId &&
      other.numeroFactura == numeroFactura &&
      other.fechaEmision == fechaEmision &&
      other.fechaVencimiento == fechaVencimiento &&
      other.monto == monto &&
      other.moneda == moneda &&
      other.cuentaGastoId == cuentaGastoId &&
      other.aprobadaPor == aprobadaPor;

    @override
    int get hashCode =>
        terceroComercialId.hashCode +
        ordenCompraId.hashCode +
        centroCostoId.hashCode +
        numeroFactura.hashCode +
        fechaEmision.hashCode +
        fechaVencimiento.hashCode +
        monto.hashCode +
        moneda.hashCode +
        cuentaGastoId.hashCode +
        aprobadaPor.hashCode;

  factory EntradaFactura.fromJson(Map<String, dynamic> json) => _$EntradaFacturaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaFacturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaFacturaMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaFacturaMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


