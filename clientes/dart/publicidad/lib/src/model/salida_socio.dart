//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_socio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSocio {
  /// Returns a new [SalidaSocio] instance.
  SalidaSocio({

    required  this.socioComercialId,

    required  this.estado,
  });

  @JsonKey(
    
    name: r'socioComercialId',
    required: true,
    includeIfNull: false,
  )


  final String socioComercialId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaSocioEstadoEnum estado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSocio &&
      other.socioComercialId == socioComercialId &&
      other.estado == estado;

    @override
    int get hashCode =>
        socioComercialId.hashCode +
        estado.hashCode;

  factory SalidaSocio.fromJson(Map<String, dynamic> json) => _$SalidaSocioFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSocioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaSocioEstadoEnum {
@JsonValue(r'POSTULADO')
POSTULADO(r'POSTULADO'),
@JsonValue(r'ACTIVO')
ACTIVO(r'ACTIVO'),
@JsonValue(r'SUSPENDIDO')
SUSPENDIDO(r'SUSPENDIDO'),
@JsonValue(r'DADO_DE_BAJA')
DADO_DE_BAJA(r'DADO_DE_BAJA');

const SalidaSocioEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


