//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_publicacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPublicacion {
  /// Returns a new [SalidaPublicacion] instance.
  SalidaPublicacion({

    required  this.tarifarioNuevoId,

    required  this.cambioId,

    required  this.version,

    required  this.estado,

    required  this.entraEnVigencia,

    required  this.requierePreaviso,
  });

  @JsonKey(
    
    name: r'tarifarioNuevoId',
    required: true,
    includeIfNull: false,
  )


  final String tarifarioNuevoId;



  @JsonKey(
    
    name: r'cambioId',
    required: true,
    includeIfNull: false,
  )


  final String cambioId;



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaPublicacionEstadoEnum estado;



  @JsonKey(
    
    name: r'entraEnVigencia',
    required: true,
    includeIfNull: false,
  )


  final DateTime entraEnVigencia;



  @JsonKey(
    
    name: r'requierePreaviso',
    required: true,
    includeIfNull: false,
  )


  final bool requierePreaviso;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPublicacion &&
      other.tarifarioNuevoId == tarifarioNuevoId &&
      other.cambioId == cambioId &&
      other.version == version &&
      other.estado == estado &&
      other.entraEnVigencia == entraEnVigencia &&
      other.requierePreaviso == requierePreaviso;

    @override
    int get hashCode =>
        tarifarioNuevoId.hashCode +
        cambioId.hashCode +
        version.hashCode +
        estado.hashCode +
        entraEnVigencia.hashCode +
        requierePreaviso.hashCode;

  factory SalidaPublicacion.fromJson(Map<String, dynamic> json) => _$SalidaPublicacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPublicacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPublicacionEstadoEnum {
@JsonValue(r'BORRADOR')
BORRADOR(r'BORRADOR'),
@JsonValue(r'EN_PREAVISO')
EN_PREAVISO(r'EN_PREAVISO'),
@JsonValue(r'VIGENTE')
VIGENTE(r'VIGENTE');

const SalidaPublicacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


