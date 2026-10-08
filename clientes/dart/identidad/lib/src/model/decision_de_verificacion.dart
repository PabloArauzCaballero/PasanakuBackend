//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'decision_de_verificacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DecisionDeVerificacion {
  /// Returns a new [DecisionDeVerificacion] instance.
  DecisionDeVerificacion({

    required  this.decision,

     this.motivo,
  });

  @JsonKey(
    
    name: r'decision',
    required: true,
    includeIfNull: false,
  )


  final DecisionDeVerificacionDecisionEnum decision;



      /// Obligatorio al rechazar.
  @JsonKey(
    
    name: r'motivo',
    required: false,
    includeIfNull: false,
  )


  final String? motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DecisionDeVerificacion &&
      other.decision == decision &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        decision.hashCode +
        motivo.hashCode;

  factory DecisionDeVerificacion.fromJson(Map<String, dynamic> json) => _$DecisionDeVerificacionFromJson(json);

  Map<String, dynamic> toJson() => _$DecisionDeVerificacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum DecisionDeVerificacionDecisionEnum {
@JsonValue(r'APROBAR')
APROBAR(r'APROBAR'),
@JsonValue(r'RECHAZAR')
RECHAZAR(r'RECHAZAR');

const DecisionDeVerificacionDecisionEnum(this.value);

final String value;

@override
String toString() => value;
}


