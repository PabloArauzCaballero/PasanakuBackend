//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_resolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaResolucion {
  /// Returns a new [SalidaResolucion] instance.
  SalidaResolucion({

    required  this.apelacionId,

    required  this.estadoDeLaApelacion,

    required  this.estadoDeLaSancion,
  });

  @JsonKey(
    
    name: r'apelacionId',
    required: true,
    includeIfNull: false,
  )


  final String apelacionId;



  @JsonKey(
    
    name: r'estadoDeLaApelacion',
    required: true,
    includeIfNull: false,
  )


  final SalidaResolucionEstadoDeLaApelacionEnum estadoDeLaApelacion;



  @JsonKey(
    
    name: r'estadoDeLaSancion',
    required: true,
    includeIfNull: false,
  )


  final SalidaResolucionEstadoDeLaSancionEnum estadoDeLaSancion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaResolucion &&
      other.apelacionId == apelacionId &&
      other.estadoDeLaApelacion == estadoDeLaApelacion &&
      other.estadoDeLaSancion == estadoDeLaSancion;

    @override
    int get hashCode =>
        apelacionId.hashCode +
        estadoDeLaApelacion.hashCode +
        estadoDeLaSancion.hashCode;

  factory SalidaResolucion.fromJson(Map<String, dynamic> json) => _$SalidaResolucionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaResolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaResolucionEstadoDeLaApelacionEnum {
@JsonValue(r'ACEPTADA')
ACEPTADA(r'ACEPTADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaResolucionEstadoDeLaApelacionEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaResolucionEstadoDeLaSancionEnum {
@JsonValue(r'REVOCADA')
REVOCADA(r'REVOCADA'),
@JsonValue(r'VIGENTE')
VIGENTE(r'VIGENTE');

const SalidaResolucionEstadoDeLaSancionEnum(this.value);

final String value;

@override
String toString() => value;
}


