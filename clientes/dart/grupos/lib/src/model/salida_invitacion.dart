//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_invitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaInvitacion {
  /// Returns a new [SalidaInvitacion] instance.
  SalidaInvitacion({

     this.invitacionId,

    required  this.mensaje,
  });

  @JsonKey(
    
    name: r'invitacionId',
    required: false,
    includeIfNull: false,
  )


  final String? invitacionId;



  @JsonKey(
    
    name: r'mensaje',
    required: true,
    includeIfNull: false,
  )


  final String mensaje;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaInvitacion &&
      other.invitacionId == invitacionId &&
      other.mensaje == mensaje;

    @override
    int get hashCode =>
        (invitacionId == null ? 0 : invitacionId.hashCode) +
        mensaje.hashCode;

  factory SalidaInvitacion.fromJson(Map<String, dynamic> json) => _$SalidaInvitacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaInvitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

