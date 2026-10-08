//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'documento.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Documento {
  /// Returns a new [Documento] instance.
  Documento({

    required  this.tipo,

    required  this.numero,

     this.lugarExpedicion,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final DocumentoTipoEnum tipo;



  @JsonKey(
    
    name: r'numero',
    required: true,
    includeIfNull: false,
  )


  final String numero;



      /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
  @JsonKey(
    
    name: r'lugarExpedicion',
    required: false,
    includeIfNull: false,
  )


  final DocumentoLugarExpedicionEnum? lugarExpedicion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Documento &&
      other.tipo == tipo &&
      other.numero == numero &&
      other.lugarExpedicion == lugarExpedicion;

    @override
    int get hashCode =>
        tipo.hashCode +
        numero.hashCode +
        lugarExpedicion.hashCode;

  factory Documento.fromJson(Map<String, dynamic> json) => _$DocumentoFromJson(json);

  Map<String, dynamic> toJson() => _$DocumentoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DocumentoTipoEnum {
@JsonValue(r'CI')
CI(r'CI'),
@JsonValue(r'CEX')
CEX(r'CEX'),
@JsonValue(r'PASAPORTE')
PASAPORTE(r'PASAPORTE');

const DocumentoTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


/// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
enum DocumentoLugarExpedicionEnum {
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'LP')
LP(r'LP'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'SC')
SC(r'SC'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'CB')
CB(r'CB'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'OR')
OR(r'OR'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'PT')
PT(r'PT'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'TJ')
TJ(r'TJ'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'CH')
CH(r'CH'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'BE')
BE(r'BE'),
    /// La extension del carnet: el departamento que lo emitio. Obligatoria para `CI` —el servicio la exige— y sin sentido para un pasaporte, que ya es unico por numero. No se declara `required` aca porque depende del tipo.
@JsonValue(r'PD')
PD(r'PD');

const DocumentoLugarExpedicionEnum(this.value);

final String value;

@override
String toString() => value;
}


