//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_contrato.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaContrato {
  /// Returns a new [SalidaContrato] instance.
  SalidaContrato({

    required  this.contratoId,

    required  this.version,

    required  this.yaExistia,
  });

  @JsonKey(
    
    name: r'contratoId',
    required: true,
    includeIfNull: false,
  )


  final String contratoId;



  @JsonKey(
    
    name: r'version',
    required: true,
    includeIfNull: false,
  )


  final String version;



  @JsonKey(
    
    name: r'yaExistia',
    required: true,
    includeIfNull: false,
  )


  final bool yaExistia;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaContrato &&
      other.contratoId == contratoId &&
      other.version == version &&
      other.yaExistia == yaExistia;

    @override
    int get hashCode =>
        contratoId.hashCode +
        version.hashCode +
        yaExistia.hashCode;

  factory SalidaContrato.fromJson(Map<String, dynamic> json) => _$SalidaContratoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaContratoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

