//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_identidad/src/model/factor_presentado.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_autenticacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAutenticacion {
  /// Returns a new [EntradaAutenticacion] instance.
  EntradaAutenticacion({

    required  this.telefonoE164,

     this.credencial,

    required  this.huellaDispositivo,

     this.plataforma,

     this.factor,
  });

  @JsonKey(
    
    name: r'telefonoE164',
    required: true,
    includeIfNull: false,
  )


  final String telefonoE164;



  @JsonKey(
    
    name: r'credencial',
    required: false,
    includeIfNull: false,
  )


  final String? credencial;



  @JsonKey(
    
    name: r'huellaDispositivo',
    required: true,
    includeIfNull: false,
  )


  final String huellaDispositivo;



  @JsonKey(
    
    name: r'plataforma',
    required: false,
    includeIfNull: false,
  )


  final EntradaAutenticacionPlataformaEnum? plataforma;



  @JsonKey(
    
    name: r'factor',
    required: false,
    includeIfNull: false,
  )


  final FactorPresentado? factor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAutenticacion &&
      other.telefonoE164 == telefonoE164 &&
      other.credencial == credencial &&
      other.huellaDispositivo == huellaDispositivo &&
      other.plataforma == plataforma &&
      other.factor == factor;

    @override
    int get hashCode =>
        telefonoE164.hashCode +
        credencial.hashCode +
        huellaDispositivo.hashCode +
        plataforma.hashCode +
        factor.hashCode;

  factory EntradaAutenticacion.fromJson(Map<String, dynamic> json) => _$EntradaAutenticacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAutenticacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaAutenticacionPlataformaEnum {
@JsonValue(r'ANDROID')
ANDROID(r'ANDROID'),
@JsonValue(r'IOS')
IOS(r'IOS'),
@JsonValue(r'WEB')
WEB(r'WEB');

const EntradaAutenticacionPlataformaEnum(this.value);

final String value;

@override
String toString() => value;
}


