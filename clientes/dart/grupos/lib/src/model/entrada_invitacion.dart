//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_invitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaInvitacion {
  /// Returns a new [EntradaInvitacion] instance.
  EntradaInvitacion({

    required  this.telefonoInvitado,

     this.nombreSugerido,

    required  this.canal,
  });

  @JsonKey(
    
    name: r'telefonoInvitado',
    required: true,
    includeIfNull: false,
  )


  final String telefonoInvitado;



  @JsonKey(
    
    name: r'nombreSugerido',
    required: false,
    includeIfNull: false,
  )


  final String? nombreSugerido;



  @JsonKey(
    
    name: r'canal',
    required: true,
    includeIfNull: false,
  )


  final EntradaInvitacionCanalEnum canal;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaInvitacion &&
      other.telefonoInvitado == telefonoInvitado &&
      other.nombreSugerido == nombreSugerido &&
      other.canal == canal;

    @override
    int get hashCode =>
        telefonoInvitado.hashCode +
        (nombreSugerido == null ? 0 : nombreSugerido.hashCode) +
        canal.hashCode;

  factory EntradaInvitacion.fromJson(Map<String, dynamic> json) => _$EntradaInvitacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaInvitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaInvitacionCanalEnum {
@JsonValue(r'ENLACE')
ENLACE(r'ENLACE'),
@JsonValue(r'SMS')
SMS(r'SMS'),
@JsonValue(r'WHATSAPP')
WHATSAPP(r'WHATSAPP');

const EntradaInvitacionCanalEnum(this.value);

final String value;

@override
String toString() => value;
}


