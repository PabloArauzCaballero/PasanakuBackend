//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'levantar_restriccion_request.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LevantarRestriccionRequest {
  /// Returns a new [LevantarRestriccionRequest] instance.
  LevantarRestriccionRequest({

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LevantarRestriccionRequest &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        motivo.hashCode;

  factory LevantarRestriccionRequest.fromJson(Map<String, dynamic> json) => _$LevantarRestriccionRequestFromJson(json);

  Map<String, dynamic> toJson() => _$LevantarRestriccionRequestToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

