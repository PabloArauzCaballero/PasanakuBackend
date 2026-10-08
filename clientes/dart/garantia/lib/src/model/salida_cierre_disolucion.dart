//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_cierre_disolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCierreDisolucion {
  /// Returns a new [SalidaCierreDisolucion] instance.
  SalidaCierreDisolucion({

    required  this.disolucionId,

    required  this.estado,

    required  this.cerradaEn,
  });

  @JsonKey(
    
    name: r'disolucionId',
    required: true,
    includeIfNull: false,
  )


  final String disolucionId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaCierreDisolucionEstadoEnum estado;



  @JsonKey(
    
    name: r'cerradaEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime cerradaEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCierreDisolucion &&
      other.disolucionId == disolucionId &&
      other.estado == estado &&
      other.cerradaEn == cerradaEn;

    @override
    int get hashCode =>
        disolucionId.hashCode +
        estado.hashCode +
        cerradaEn.hashCode;

  factory SalidaCierreDisolucion.fromJson(Map<String, dynamic> json) => _$SalidaCierreDisolucionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCierreDisolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCierreDisolucionEstadoEnum {
@JsonValue(r'CERRADA')
CERRADA(r'CERRADA');

const SalidaCierreDisolucionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


