//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_organizador/src/model/faltante.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_postulacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPostulacion {
  /// Returns a new [SalidaPostulacion] instance.
  SalidaPostulacion({

    required  this.solicitudId,

    required  this.estado,

    required  this.esNueva,

    required  this.faltantes,
  });

  @JsonKey(
    
    name: r'solicitudId',
    required: true,
    includeIfNull: false,
  )


  final String solicitudId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaPostulacionEstadoEnum estado;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;



  @JsonKey(
    
    name: r'faltantes',
    required: true,
    includeIfNull: false,
  )


  final List<Faltante> faltantes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPostulacion &&
      other.solicitudId == solicitudId &&
      other.estado == estado &&
      other.esNueva == esNueva &&
      other.faltantes == faltantes;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        estado.hashCode +
        esNueva.hashCode +
        faltantes.hashCode;

  factory SalidaPostulacion.fromJson(Map<String, dynamic> json) => _$SalidaPostulacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPostulacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPostulacionEstadoEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'EN_REVISION')
EN_REVISION(r'EN_REVISION'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaPostulacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


