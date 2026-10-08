//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_aceptacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAceptacion {
  /// Returns a new [SalidaAceptacion] instance.
  SalidaAceptacion({

    required  this.aceptacionId,

    required  this.hashEvidencia,

    required  this.aceptadoEn,
  });

  @JsonKey(

    name: r'aceptacionId',
    required: true,
    includeIfNull: false,
  )


  final String aceptacionId;



  @JsonKey(

    name: r'hashEvidencia',
    required: true,
    includeIfNull: false,
  )


  final String hashEvidencia;



  @JsonKey(

    name: r'aceptadoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime aceptadoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAceptacion &&
      other.aceptacionId == aceptacionId &&
      other.hashEvidencia == hashEvidencia &&
      other.aceptadoEn == aceptadoEn;

    @override
    int get hashCode =>
        aceptacionId.hashCode +
        hashEvidencia.hashCode +
        aceptadoEn.hashCode;

  factory SalidaAceptacion.fromJson(Map<String, dynamic> json) => _$SalidaAceptacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAceptacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

