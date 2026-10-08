//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_permuta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPermuta {
  /// Returns a new [SalidaPermuta] instance.
  SalidaPermuta({

    required  this.solicitudId,

    required  this.estado,
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


  final SalidaPermutaEstadoEnum estado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPermuta &&
      other.solicitudId == solicitudId &&
      other.estado == estado;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        estado.hashCode;

  factory SalidaPermuta.fromJson(Map<String, dynamic> json) => _$SalidaPermutaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPermutaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPermutaEstadoEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'ACEPTADA')
ACEPTADA(r'ACEPTADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA'),
@JsonValue(r'EJECUTADA')
EJECUTADA(r'EJECUTADA');

const SalidaPermutaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


