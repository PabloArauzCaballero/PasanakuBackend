//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_apelacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaApelacion {
  /// Returns a new [SalidaApelacion] instance.
  SalidaApelacion({

     this.apelacionId,

    required  this.estado,

    required  this.esNueva,
  });

      /// null mientras la apelacion esta abierta: la base no admite guardar una sin resolver. Ver el hueco H-7 del informe del carril
  @JsonKey(
    
    name: r'apelacionId',
    required: false,
    includeIfNull: false,
  )


  final String? apelacionId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaApelacion &&
      other.apelacionId == apelacionId &&
      other.estado == estado &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        (apelacionId == null ? 0 : apelacionId.hashCode) +
        estado.hashCode +
        esNueva.hashCode;

  factory SalidaApelacion.fromJson(Map<String, dynamic> json) => _$SalidaApelacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaApelacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

