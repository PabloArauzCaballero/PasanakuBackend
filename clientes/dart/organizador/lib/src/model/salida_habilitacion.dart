//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_organizador/src/model/faltante.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_habilitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaHabilitacion {
  /// Returns a new [SalidaHabilitacion] instance.
  SalidaHabilitacion({

    required  this.organizadorId,

    required  this.estado,

    required  this.faltantes,
  });

  @JsonKey(
    
    name: r'organizadorId',
    required: true,
    includeIfNull: false,
  )


  final String organizadorId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaHabilitacionEstadoEnum estado;



  @JsonKey(
    
    name: r'faltantes',
    required: true,
    includeIfNull: false,
  )


  final List<Faltante> faltantes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaHabilitacion &&
      other.organizadorId == organizadorId &&
      other.estado == estado &&
      other.faltantes == faltantes;

    @override
    int get hashCode =>
        organizadorId.hashCode +
        estado.hashCode +
        faltantes.hashCode;

  factory SalidaHabilitacion.fromJson(Map<String, dynamic> json) => _$SalidaHabilitacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaHabilitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaHabilitacionEstadoEnum {
@JsonValue(r'POSTULADO')
POSTULADO(r'POSTULADO'),
@JsonValue(r'CAPACITACION_PENDIENTE')
CAPACITACION_PENDIENTE(r'CAPACITACION_PENDIENTE'),
@JsonValue(r'HABILITADO')
HABILITADO(r'HABILITADO'),
@JsonValue(r'LIMITADO')
LIMITADO(r'LIMITADO'),
@JsonValue(r'SUSPENDIDO')
SUSPENDIDO(r'SUSPENDIDO'),
@JsonValue(r'EN_EVALUACION')
EN_EVALUACION(r'EN_EVALUACION'),
@JsonValue(r'DESHABILITADO')
DESHABILITADO(r'DESHABILITADO'),
@JsonValue(r'RETIRADO')
RETIRADO(r'RETIRADO');

const SalidaHabilitacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


