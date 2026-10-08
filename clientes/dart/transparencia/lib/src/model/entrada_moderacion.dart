//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_moderacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaModeracion {
  /// Returns a new [EntradaModeracion] instance.
  EntradaModeracion({

    required  this.decision,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'decision',
    required: true,
    includeIfNull: false,
  )


  final EntradaModeracionDecisionEnum decision;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaModeracion &&
      other.decision == decision &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        decision.hashCode +
        motivo.hashCode;

  factory EntradaModeracion.fromJson(Map<String, dynamic> json) => _$EntradaModeracionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaModeracionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaModeracionDecisionEnum {
@JsonValue(r'APROBAR')
APROBAR(r'APROBAR'),
@JsonValue(r'RECHAZAR')
RECHAZAR(r'RECHAZAR'),
@JsonValue(r'PUBLICAR_SIN_COMENTARIO')
PUBLICAR_SIN_COMENTARIO(r'PUBLICAR_SIN_COMENTARIO');

const EntradaModeracionDecisionEnum(this.value);

final String value;

@override
String toString() => value;
}


