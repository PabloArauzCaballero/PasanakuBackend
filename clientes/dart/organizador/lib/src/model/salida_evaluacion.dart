//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_evaluacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEvaluacion {
  /// Returns a new [SalidaEvaluacion] instance.
  SalidaEvaluacion({

    required  this.evaluacionId,

    required  this.nivelActual,

    required  this.nivelSugerido,

     this.puntaje,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'evaluacionId',
    required: true,
    includeIfNull: false,
  )


  final String evaluacionId;



  @JsonKey(
    
    name: r'nivelActual',
    required: true,
    includeIfNull: false,
  )


  final SalidaEvaluacionNivelActualEnum nivelActual;



  @JsonKey(
    
    name: r'nivelSugerido',
    required: true,
    includeIfNull: false,
  )


  final SalidaEvaluacionNivelSugeridoEnum nivelSugerido;



  @JsonKey(
    
    name: r'puntaje',
    required: false,
    includeIfNull: false,
  )


  final String? puntaje;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEvaluacion &&
      other.evaluacionId == evaluacionId &&
      other.nivelActual == nivelActual &&
      other.nivelSugerido == nivelSugerido &&
      other.puntaje == puntaje &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        evaluacionId.hashCode +
        nivelActual.hashCode +
        nivelSugerido.hashCode +
        (puntaje == null ? 0 : puntaje.hashCode) +
        esNueva.hashCode;

  factory SalidaEvaluacion.fromJson(Map<String, dynamic> json) => _$SalidaEvaluacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEvaluacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaEvaluacionNivelActualEnum {
@JsonValue(r'APRENDIZ')
APRENDIZ(r'APRENDIZ'),
@JsonValue(r'ESTANDAR')
ESTANDAR(r'ESTANDAR'),
@JsonValue(r'SENIOR')
SENIOR(r'SENIOR'),
@JsonValue(r'MAESTRO')
MAESTRO(r'MAESTRO');

const SalidaEvaluacionNivelActualEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaEvaluacionNivelSugeridoEnum {
@JsonValue(r'APRENDIZ')
APRENDIZ(r'APRENDIZ'),
@JsonValue(r'ESTANDAR')
ESTANDAR(r'ESTANDAR'),
@JsonValue(r'SENIOR')
SENIOR(r'SENIOR'),
@JsonValue(r'MAESTRO')
MAESTRO(r'MAESTRO');

const SalidaEvaluacionNivelSugeridoEnum(this.value);

final String value;

@override
String toString() => value;
}


