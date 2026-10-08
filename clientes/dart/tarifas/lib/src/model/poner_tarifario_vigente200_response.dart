//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'poner_tarifario_vigente200_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PonerTarifarioVigente200Response {
  /// Returns a new [PonerTarifarioVigente200Response] instance.
  PonerTarifarioVigente200Response({

    required  this.tarifarioId,

    required  this.estado,

    required  this.desde,
  });

  @JsonKey(
    
    name: r'tarifarioId',
    required: true,
    includeIfNull: false,
  )


  final String tarifarioId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final PonerTarifarioVigente200ResponseEstadoEnum estado;



  @JsonKey(
    
    name: r'desde',
    required: true,
    includeIfNull: false,
  )


  final DateTime desde;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PonerTarifarioVigente200Response &&
      other.tarifarioId == tarifarioId &&
      other.estado == estado &&
      other.desde == desde;

    @override
    int get hashCode =>
        tarifarioId.hashCode +
        estado.hashCode +
        desde.hashCode;

  factory PonerTarifarioVigente200Response.fromJson(Map<String, dynamic> json) => _$PonerTarifarioVigente200ResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PonerTarifarioVigente200ResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum PonerTarifarioVigente200ResponseEstadoEnum {
@JsonValue(r'VIGENTE')
VIGENTE(r'VIGENTE');

const PonerTarifarioVigente200ResponseEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


