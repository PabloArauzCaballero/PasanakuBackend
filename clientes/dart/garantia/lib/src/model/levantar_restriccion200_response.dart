//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'levantar_restriccion200_response.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LevantarRestriccion200Response {
  /// Returns a new [LevantarRestriccion200Response] instance.
  LevantarRestriccion200Response({

    required  this.levantada,
  });

  @JsonKey(
    
    name: r'levantada',
    required: true,
    includeIfNull: false,
  )


  final bool levantada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LevantarRestriccion200Response &&
      other.levantada == levantada;

    @override
    int get hashCode =>
        levantada.hashCode;

  factory LevantarRestriccion200Response.fromJson(Map<String, dynamic> json) => _$LevantarRestriccion200ResponseFromJson(json);

  Map<String, dynamic> toJson() => _$LevantarRestriccion200ResponseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

