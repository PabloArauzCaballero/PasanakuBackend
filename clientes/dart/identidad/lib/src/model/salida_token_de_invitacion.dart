//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_token_de_invitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTokenDeInvitacion {
  /// Returns a new [SalidaTokenDeInvitacion] instance.
  SalidaTokenDeInvitacion({

    required  this.tokenId,

    required  this.token,

    required  this.expiraEn,
  });

  @JsonKey(
    
    name: r'tokenId',
    required: true,
    includeIfNull: false,
  )


  final String tokenId;



  @JsonKey(
    
    name: r'token',
    required: true,
    includeIfNull: false,
  )


  final String token;



  @JsonKey(
    
    name: r'expiraEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiraEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTokenDeInvitacion &&
      other.tokenId == tokenId &&
      other.token == token &&
      other.expiraEn == expiraEn;

    @override
    int get hashCode =>
        tokenId.hashCode +
        token.hashCode +
        expiraEn.hashCode;

  factory SalidaTokenDeInvitacion.fromJson(Map<String, dynamic> json) => _$SalidaTokenDeInvitacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTokenDeInvitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

