//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'archivo_del_expediente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ArchivoDelExpediente {
  /// Returns a new [ArchivoDelExpediente] instance.
  ArchivoDelExpediente({

    required  this.cara,

    required  this.claveObjeto,

    required  this.hashArchivo,

     this.tipoMime,

     this.bytes,
  });

  @JsonKey(
    
    name: r'cara',
    required: true,
    includeIfNull: false,
  )


  final ArchivoDelExpedienteCaraEnum cara;



  @JsonKey(
    
    name: r'claveObjeto',
    required: true,
    includeIfNull: false,
  )


  final String claveObjeto;



  @JsonKey(
    
    name: r'hashArchivo',
    required: true,
    includeIfNull: false,
  )


  final String hashArchivo;



  @JsonKey(
    
    name: r'tipoMime',
    required: false,
    includeIfNull: false,
  )


  final String? tipoMime;



  @JsonKey(
    
    name: r'bytes',
    required: false,
    includeIfNull: false,
  )


  final int? bytes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ArchivoDelExpediente &&
      other.cara == cara &&
      other.claveObjeto == claveObjeto &&
      other.hashArchivo == hashArchivo &&
      other.tipoMime == tipoMime &&
      other.bytes == bytes;

    @override
    int get hashCode =>
        cara.hashCode +
        claveObjeto.hashCode +
        hashArchivo.hashCode +
        tipoMime.hashCode +
        bytes.hashCode;

  factory ArchivoDelExpediente.fromJson(Map<String, dynamic> json) => _$ArchivoDelExpedienteFromJson(json);

  Map<String, dynamic> toJson() => _$ArchivoDelExpedienteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ArchivoDelExpedienteCaraEnum {
@JsonValue(r'ANVERSO')
ANVERSO(r'ANVERSO'),
@JsonValue(r'REVERSO')
REVERSO(r'REVERSO'),
@JsonValue(r'SELFIE')
SELFIE(r'SELFIE');

const ArchivoDelExpedienteCaraEnum(this.value);

final String value;

@override
String toString() => value;
}


