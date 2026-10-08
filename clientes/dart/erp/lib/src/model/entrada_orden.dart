//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_orden.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaOrden {
  /// Returns a new [EntradaOrden] instance.
  EntradaOrden({

    required  this.terceroComercialId,

     this.centroCostoId,

    required  this.numero,

    required  this.descripcion,

    required  this.monto,

    required  this.moneda,
  });

  @JsonKey(
    
    name: r'terceroComercialId',
    required: true,
    includeIfNull: false,
  )


  final String terceroComercialId;



  @JsonKey(
    
    name: r'centroCostoId',
    required: false,
    includeIfNull: false,
  )


  final String? centroCostoId;



  @JsonKey(
    
    name: r'numero',
    required: true,
    includeIfNull: false,
  )


  final String numero;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



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


  final EntradaOrdenMonedaEnum moneda;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaOrden &&
      other.terceroComercialId == terceroComercialId &&
      other.centroCostoId == centroCostoId &&
      other.numero == numero &&
      other.descripcion == descripcion &&
      other.monto == monto &&
      other.moneda == moneda;

    @override
    int get hashCode =>
        terceroComercialId.hashCode +
        centroCostoId.hashCode +
        numero.hashCode +
        descripcion.hashCode +
        monto.hashCode +
        moneda.hashCode;

  factory EntradaOrden.fromJson(Map<String, dynamic> json) => _$EntradaOrdenFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaOrdenToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaOrdenMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaOrdenMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


