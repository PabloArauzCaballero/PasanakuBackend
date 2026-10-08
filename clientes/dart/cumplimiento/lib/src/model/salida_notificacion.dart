//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_notificacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaNotificacion {
  /// Returns a new [SalidaNotificacion] instance.
  SalidaNotificacion({

    required  this.notificacionId,

    required  this.canales,
  });

  @JsonKey(
    
    name: r'notificacionId',
    required: true,
    includeIfNull: false,
  )


  final String notificacionId;



  @JsonKey(
    
    name: r'canales',
    required: true,
    includeIfNull: false,
  )


  final List<SalidaNotificacionCanalesEnum> canales;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaNotificacion &&
      other.notificacionId == notificacionId &&
      other.canales == canales;

    @override
    int get hashCode =>
        notificacionId.hashCode +
        canales.hashCode;

  factory SalidaNotificacion.fromJson(Map<String, dynamic> json) => _$SalidaNotificacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaNotificacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaNotificacionCanalesEnum {
@JsonValue(r'IN_APP')
IN_APP(r'IN_APP'),
@JsonValue(r'PUSH')
PUSH(r'PUSH'),
@JsonValue(r'CORREO')
CORREO(r'CORREO');

const SalidaNotificacionCanalesEnum(this.value);

final String value;

@override
String toString() => value;
}


