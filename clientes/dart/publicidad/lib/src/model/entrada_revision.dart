//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_revision.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRevision {
  /// Returns a new [EntradaRevision] instance.
  EntradaRevision({

    required  this.decision,

     this.motivo,
  });

  @JsonKey(
    
    name: r'decision',
    required: true,
    includeIfNull: false,
  )


  final EntradaRevisionDecisionEnum decision;



      /// Obligatorio cuando la decision es RECHAZADA.
  @JsonKey(
    
    name: r'motivo',
    required: false,
    includeIfNull: false,
  )


  final String? motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRevision &&
      other.decision == decision &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        decision.hashCode +
        (motivo == null ? 0 : motivo.hashCode);

  factory EntradaRevision.fromJson(Map<String, dynamic> json) => _$EntradaRevisionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRevisionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaRevisionDecisionEnum {
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const EntradaRevisionDecisionEnum(this.value);

final String value;

@override
String toString() => value;
}


