//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_anunciante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAnunciante {
  /// Returns a new [EntradaAnunciante] instance.
  EntradaAnunciante({

    required  this.tipo,

     this.organizadorId,

     this.socioComercialId,

    required  this.razonSocialFacturacion,

     this.limiteGastoMensual,

    required  this.moneda,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaAnuncianteTipoEnum tipo;



      /// Obligatorio si tipo es ORGANIZADOR, y prohibido si no (R-PUB-01).
  @JsonKey(
    
    name: r'organizadorId',
    required: false,
    includeIfNull: false,
  )


  final String? organizadorId;



      /// Obligatorio si tipo es SOCIO_COMERCIAL, y prohibido si no.
  @JsonKey(
    
    name: r'socioComercialId',
    required: false,
    includeIfNull: false,
  )


  final String? socioComercialId;



  @JsonKey(
    
    name: r'razonSocialFacturacion',
    required: true,
    includeIfNull: false,
  )


  final String razonSocialFacturacion;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'limiteGastoMensual',
    required: false,
    includeIfNull: false,
  )


  final String? limiteGastoMensual;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaAnuncianteMonedaEnum moneda;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAnunciante &&
      other.tipo == tipo &&
      other.organizadorId == organizadorId &&
      other.socioComercialId == socioComercialId &&
      other.razonSocialFacturacion == razonSocialFacturacion &&
      other.limiteGastoMensual == limiteGastoMensual &&
      other.moneda == moneda;

    @override
    int get hashCode =>
        tipo.hashCode +
        (organizadorId == null ? 0 : organizadorId.hashCode) +
        (socioComercialId == null ? 0 : socioComercialId.hashCode) +
        razonSocialFacturacion.hashCode +
        limiteGastoMensual.hashCode +
        moneda.hashCode;

  factory EntradaAnunciante.fromJson(Map<String, dynamic> json) => _$EntradaAnuncianteFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAnuncianteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaAnuncianteTipoEnum {
@JsonValue(r'ORGANIZADOR')
ORGANIZADOR(r'ORGANIZADOR'),
@JsonValue(r'SOCIO_COMERCIAL')
SOCIO_COMERCIAL(r'SOCIO_COMERCIAL');

const EntradaAnuncianteTipoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaAnuncianteMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaAnuncianteMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


