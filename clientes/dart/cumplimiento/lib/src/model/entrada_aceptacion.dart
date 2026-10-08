//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_aceptacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAceptacion {
  /// Returns a new [EntradaAceptacion] instance.
  EntradaAceptacion({

    required  this.usuarioId,

    required  this.version,

     this.tokenFirma,

     this.consentimientos,
  });

  @JsonKey(

    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(

    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final int version;



  @JsonKey(

    name: r'tokenFirma',
    required: false,
    includeIfNull: false,
  )


  final String? tokenFirma;



  @JsonKey(

    name: r'consentimientos',
    required: false,
    includeIfNull: false,
  )


  final List<String>? consentimientos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAceptacion &&
      other.usuarioId == usuarioId &&
      other.version == version &&
      other.tokenFirma == tokenFirma &&
      other.consentimientos == consentimientos;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        version.hashCode +
        tokenFirma.hashCode +
        consentimientos.hashCode;

  factory EntradaAceptacion.fromJson(Map<String, dynamic> json) => _$EntradaAceptacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAceptacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

