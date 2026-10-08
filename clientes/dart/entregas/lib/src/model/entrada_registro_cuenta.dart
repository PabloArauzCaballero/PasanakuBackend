//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_registro_cuenta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRegistroCuenta {
  /// Returns a new [EntradaRegistroCuenta] instance.
  EntradaRegistroCuenta({

    required  this.tipoCuenta,

    required  this.entidadFinanciera,

    required  this.numeroCuenta,

    required  this.numeroCifrado,

    required  this.titularNombre,

    required  this.titularDocumento,

    required  this.moneda,
  });

  @JsonKey(
    
    name: r'tipoCuenta',
    required: true,
    includeIfNull: false,
  )


  final EntradaRegistroCuentaTipoCuentaEnum tipoCuenta;



  @JsonKey(
    
    name: r'entidadFinanciera',
    required: true,
    includeIfNull: false,
  )


  final String entidadFinanciera;



      /// El numero en claro, y **nunca se persiste asi** (CU-18, R-SEG-01). Entra solo para derivar el enmascarado que se muestra y el hash con el que se detecta la cuenta repetida; las dos derivaciones ocurren ANTES de tocar la base, asi que el numero no llega a ninguna consulta.
  @JsonKey(
    
    name: r'numeroCuenta',
    required: true,
    includeIfNull: false,
  )


  final String numeroCuenta;



      /// ya cifrado por el almacen de llaves. **El numero en claro nunca viaja en este campo ni se persiste** (R-SEG-01)
  @JsonKey(
    
    name: r'numeroCifrado',
    required: true,
    includeIfNull: false,
  )


  final String numeroCifrado;



  @JsonKey(
    
    name: r'titularNombre',
    required: true,
    includeIfNull: false,
  )


  final String titularNombre;



  @JsonKey(
    
    name: r'titularDocumento',
    required: true,
    includeIfNull: false,
  )


  final String titularDocumento;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaRegistroCuentaMonedaEnum moneda;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRegistroCuenta &&
      other.tipoCuenta == tipoCuenta &&
      other.entidadFinanciera == entidadFinanciera &&
      other.numeroCuenta == numeroCuenta &&
      other.numeroCifrado == numeroCifrado &&
      other.titularNombre == titularNombre &&
      other.titularDocumento == titularDocumento &&
      other.moneda == moneda;

    @override
    int get hashCode =>
        tipoCuenta.hashCode +
        entidadFinanciera.hashCode +
        numeroCuenta.hashCode +
        numeroCifrado.hashCode +
        titularNombre.hashCode +
        titularDocumento.hashCode +
        moneda.hashCode;

  factory EntradaRegistroCuenta.fromJson(Map<String, dynamic> json) => _$EntradaRegistroCuentaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRegistroCuentaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaRegistroCuentaTipoCuentaEnum {
@JsonValue(r'AHORRO')
AHORRO(r'AHORRO'),
@JsonValue(r'CORRIENTE')
CORRIENTE(r'CORRIENTE'),
@JsonValue(r'BILLETERA')
BILLETERA(r'BILLETERA');

const EntradaRegistroCuentaTipoCuentaEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaRegistroCuentaMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaRegistroCuentaMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


