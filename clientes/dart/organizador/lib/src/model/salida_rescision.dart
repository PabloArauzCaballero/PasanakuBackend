//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_rescision.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRescision {
  /// Returns a new [SalidaRescision] instance.
  SalidaRescision({

    required  this.contratoId,

    required  this.rescindidoEn,

    required  this.estadoDelOrganizador,
  });

  @JsonKey(
    
    name: r'contratoId',
    required: true,
    includeIfNull: false,
  )


  final String contratoId;



  @JsonKey(
    
    name: r'rescindidoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime rescindidoEn;



  @JsonKey(
    
    name: r'estadoDelOrganizador',
    required: true,
    includeIfNull: false,
  )


  final String estadoDelOrganizador;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRescision &&
      other.contratoId == contratoId &&
      other.rescindidoEn == rescindidoEn &&
      other.estadoDelOrganizador == estadoDelOrganizador;

    @override
    int get hashCode =>
        contratoId.hashCode +
        rescindidoEn.hashCode +
        estadoDelOrganizador.hashCode;

  factory SalidaRescision.fromJson(Map<String, dynamic> json) => _$SalidaRescisionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRescisionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

