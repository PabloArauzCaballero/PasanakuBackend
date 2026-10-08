//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_retiro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRetiro {
  /// Returns a new [SalidaRetiro] instance.
  SalidaRetiro({

    required  this.solicitudId,

    required  this.posicion,
  });

  @JsonKey(
    
    name: r'solicitudId',
    required: true,
    includeIfNull: false,
  )


  final String solicitudId;



  @JsonKey(
    
    name: r'posicion',
    required: true,
    includeIfNull: false,
  )


  final SalidaRetiroPosicionEnum posicion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRetiro &&
      other.solicitudId == solicitudId &&
      other.posicion == posicion;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        posicion.hashCode;

  factory SalidaRetiro.fromJson(Map<String, dynamic> json) => _$SalidaRetiroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRetiroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRetiroPosicionEnum {
@JsonValue(r'ACREEDORA')
ACREEDORA(r'ACREEDORA'),
@JsonValue(r'DEUDORA')
DEUDORA(r'DEUDORA'),
@JsonValue(r'NEUTRA')
NEUTRA(r'NEUTRA');

const SalidaRetiroPosicionEnum(this.value);

final String value;

@override
String toString() => value;
}


