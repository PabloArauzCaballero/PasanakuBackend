//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'tarifario_vigente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class TarifarioVigente {
  /// Returns a new [TarifarioVigente] instance.
  TarifarioVigente({

    required  this.vigente,

     this.tarifarioId,
  });

  @JsonKey(
    
    name: r'vigente',
    required: true,
    includeIfNull: false,
  )


  final bool vigente;



  @JsonKey(
    
    name: r'tarifarioId',
    required: false,
    includeIfNull: false,
  )


  final String? tarifarioId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is TarifarioVigente &&
      other.vigente == vigente &&
      other.tarifarioId == tarifarioId;

    @override
    int get hashCode =>
        vigente.hashCode +
        (tarifarioId == null ? 0 : tarifarioId.hashCode);

  factory TarifarioVigente.fromJson(Map<String, dynamic> json) => _$TarifarioVigenteFromJson(json);

  Map<String, dynamic> toJson() => _$TarifarioVigenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

