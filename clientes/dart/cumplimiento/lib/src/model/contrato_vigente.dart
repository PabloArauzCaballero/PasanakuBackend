//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'contrato_vigente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ContratoVigente {
  /// Returns a new [ContratoVigente] instance.
  ContratoVigente({

    required  this.id,

    required  this.codigo,

    required  this.version,

    required  this.tipo,

    required  this.urlDocumento,

    required  this.hashDocumento,

    required  this.vigenteDesde,

     this.numeroRegistro,

     this.fechaRegistro,
  });

  @JsonKey(

    name: r'id',
    required: true,
    includeIfNull: false,
  )


  final String id;



  @JsonKey(

    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(

    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;



  @JsonKey(

    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final ContratoVigenteTipoEnum tipo;



  @JsonKey(

    name: r'urlDocumento',
    required: true,
    includeIfNull: false,
  )


  final String urlDocumento;



      /// SHA-256 del documento publicado. Es lo que permite comprobar que el texto que se leyo en la app es el mismo que quedo registrado en la aceptacion.
  @JsonKey(

    name: r'hashDocumento',
    required: true,
    includeIfNull: false,
  )


  final String hashDocumento;



  @JsonKey(

    name: r'vigenteDesde',
    required: true,
    includeIfNull: false,
  )


  final DateTime vigenteDesde;



      /// El numero con el que ASFI registro el contrato de adhesion. Ausente mientras el registro no se haya obtenido: no se inventa.
  @JsonKey(

    name: r'numeroRegistro',
    required: false,
    includeIfNull: false,
  )


  final String? numeroRegistro;



  @JsonKey(

    name: r'fechaRegistro',
    required: false,
    includeIfNull: false,
  )


  final DateTime? fechaRegistro;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ContratoVigente &&
      other.id == id &&
      other.codigo == codigo &&
      other.version == version &&
      other.tipo == tipo &&
      other.urlDocumento == urlDocumento &&
      other.hashDocumento == hashDocumento &&
      other.vigenteDesde == vigenteDesde &&
      other.numeroRegistro == numeroRegistro &&
      other.fechaRegistro == fechaRegistro;

    @override
    int get hashCode =>
        id.hashCode +
        codigo.hashCode +
        version.hashCode +
        tipo.hashCode +
        urlDocumento.hashCode +
        hashDocumento.hashCode +
        vigenteDesde.hashCode +
        numeroRegistro.hashCode +
        fechaRegistro.hashCode;

  factory ContratoVigente.fromJson(Map<String, dynamic> json) => _$ContratoVigenteFromJson(json);

  Map<String, dynamic> toJson() => _$ContratoVigenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ContratoVigenteTipoEnum {
@JsonValue(r'BILLETERA')
BILLETERA(r'BILLETERA'),
@JsonValue(r'GRUPO_PASANAKU')
GRUPO_PASANAKU(r'GRUPO_PASANAKU'),
@JsonValue(r'TARIFAS')
TARIFAS(r'TARIFAS'),
@JsonValue(r'TRATAMIENTO_DATOS')
TRATAMIENTO_DATOS(r'TRATAMIENTO_DATOS');

const ContratoVigenteTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


