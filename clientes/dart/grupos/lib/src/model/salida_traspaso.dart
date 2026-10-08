//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_traspaso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTraspaso {
  /// Returns a new [SalidaTraspaso] instance.
  SalidaTraspaso({

    required  this.traspasoId,
  });

  @JsonKey(
    
    name: r'traspasoId',
    required: true,
    includeIfNull: false,
  )


  final String traspasoId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTraspaso &&
      other.traspasoId == traspasoId;

    @override
    int get hashCode =>
        traspasoId.hashCode;

  factory SalidaTraspaso.fromJson(Map<String, dynamic> json) => _$SalidaTraspasoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTraspasoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

