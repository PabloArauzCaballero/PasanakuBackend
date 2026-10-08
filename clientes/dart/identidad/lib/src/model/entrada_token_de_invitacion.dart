//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_token_de_invitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTokenDeInvitacion {
  /// Returns a new [EntradaTokenDeInvitacion] instance.
  EntradaTokenDeInvitacion({

    required  this.canal,

    required  this.destinoEnmascarado,
  });

  @JsonKey(
    
    name: r'canal',
    required: true,
    includeIfNull: false,
  )


  final EntradaTokenDeInvitacionCanalEnum canal;



      /// El destino YA enmascarado. Sin enmascarar seria un telefono en claro viajando entre servicios y quedando en cada log del camino.
  @JsonKey(
    
    name: r'destinoEnmascarado',
    required: true,
    includeIfNull: false,
  )


  final String destinoEnmascarado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTokenDeInvitacion &&
      other.canal == canal &&
      other.destinoEnmascarado == destinoEnmascarado;

    @override
    int get hashCode =>
        canal.hashCode +
        destinoEnmascarado.hashCode;

  factory EntradaTokenDeInvitacion.fromJson(Map<String, dynamic> json) => _$EntradaTokenDeInvitacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTokenDeInvitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaTokenDeInvitacionCanalEnum {
@JsonValue(r'ENLACE')
ENLACE(r'ENLACE'),
@JsonValue(r'SMS')
SMS(r'SMS'),
@JsonValue(r'WHATSAPP')
WHATSAPP(r'WHATSAPP'),
@JsonValue(r'CORREO')
CORREO(r'CORREO');

const EntradaTokenDeInvitacionCanalEnum(this.value);

final String value;

@override
String toString() => value;
}


