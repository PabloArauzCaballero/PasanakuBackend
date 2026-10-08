//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_ejecucion_reembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEjecucionReembolso {
  /// Returns a new [SalidaEjecucionReembolso] instance.
  SalidaEjecucionReembolso({

    required  this.reembolsoId,

    required  this.estado,

    required  this.obligacionId,

    required  this.estadoObligacion,
  });

  @JsonKey(
    
    name: r'reembolsoId',
    required: true,
    includeIfNull: false,
  )


  final String reembolsoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaEjecucionReembolsoEstadoEnum estado;



  @JsonKey(
    
    name: r'obligacionId',
    required: true,
    includeIfNull: false,
  )


  final String obligacionId;



  @JsonKey(
    
    name: r'estadoObligacion',
    required: true,
    includeIfNull: false,
  )


  final String estadoObligacion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEjecucionReembolso &&
      other.reembolsoId == reembolsoId &&
      other.estado == estado &&
      other.obligacionId == obligacionId &&
      other.estadoObligacion == estadoObligacion;

    @override
    int get hashCode =>
        reembolsoId.hashCode +
        estado.hashCode +
        obligacionId.hashCode +
        estadoObligacion.hashCode;

  factory SalidaEjecucionReembolso.fromJson(Map<String, dynamic> json) => _$SalidaEjecucionReembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEjecucionReembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaEjecucionReembolsoEstadoEnum {
@JsonValue(r'EJECUTADO')
EJECUTADO(r'EJECUTADO');

const SalidaEjecucionReembolsoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


