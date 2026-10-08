//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_pep.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPep {
  /// Returns a new [SalidaPep] instance.
  SalidaPep({

    required  this.declaracionId,

    required  this.exigeDiligenciaReforzada,

    required  this.nivelRiesgo,
  });

  @JsonKey(

    name: r'declaracionId',
    required: true,
    includeIfNull: false,
  )


  final String declaracionId;



  @JsonKey(

    name: r'exigeDiligenciaReforzada',
    required: true,
    includeIfNull: false,
  )


  final bool exigeDiligenciaReforzada;



  @JsonKey(

    name: r'nivelRiesgo',
    required: true,
    includeIfNull: false,
  )


  final SalidaPepNivelRiesgoEnum nivelRiesgo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPep &&
      other.declaracionId == declaracionId &&
      other.exigeDiligenciaReforzada == exigeDiligenciaReforzada &&
      other.nivelRiesgo == nivelRiesgo;

    @override
    int get hashCode =>
        declaracionId.hashCode +
        exigeDiligenciaReforzada.hashCode +
        nivelRiesgo.hashCode;

  factory SalidaPep.fromJson(Map<String, dynamic> json) => _$SalidaPepFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPepToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPepNivelRiesgoEnum {
@JsonValue(r'BAJO')
BAJO(r'BAJO'),
@JsonValue(r'MEDIO')
MEDIO(r'MEDIO'),
@JsonValue(r'ALTO')
ALTO(r'ALTO');

const SalidaPepNivelRiesgoEnum(this.value);

final String value;

@override
String toString() => value;
}


