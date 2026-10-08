//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_programacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaProgramacion {
  /// Returns a new [SalidaProgramacion] instance.
  SalidaProgramacion({

    required  this.tareaId,

    required  this.estado,

    required  this.claveIdempotencia,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'tareaId',
    required: true,
    includeIfNull: false,
  )


  final String tareaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaProgramacionEstadoEnum estado;



      /// determinista — regla, grupo y minuto programado
  @JsonKey(
    
    name: r'claveIdempotencia',
    required: true,
    includeIfNull: false,
  )


  final String claveIdempotencia;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaProgramacion &&
      other.tareaId == tareaId &&
      other.estado == estado &&
      other.claveIdempotencia == claveIdempotencia &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        tareaId.hashCode +
        estado.hashCode +
        claveIdempotencia.hashCode +
        esNueva.hashCode;

  factory SalidaProgramacion.fromJson(Map<String, dynamic> json) => _$SalidaProgramacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaProgramacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaProgramacionEstadoEnum {
@JsonValue(r'PROGRAMADA')
PROGRAMADA(r'PROGRAMADA'),
@JsonValue(r'REQUIERE_APROBACION')
REQUIERE_APROBACION(r'REQUIERE_APROBACION'),
@JsonValue(r'EN_EJECUCION')
EN_EJECUCION(r'EN_EJECUCION'),
@JsonValue(r'COMPLETADA')
COMPLETADA(r'COMPLETADA'),
@JsonValue(r'FALLIDA')
FALLIDA(r'FALLIDA'),
@JsonValue(r'CANCELADA')
CANCELADA(r'CANCELADA');

const SalidaProgramacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


