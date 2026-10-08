//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'morosos.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Morosos {
  /// Returns a new [Morosos] instance.
  Morosos({

    required  this.grupoId,

    required  this.morosos,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'morosos',
    required: true,
    includeIfNull: false,
  )


  final int morosos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Morosos &&
      other.grupoId == grupoId &&
      other.morosos == morosos;

    @override
    int get hashCode =>
        grupoId.hashCode +
        morosos.hashCode;

  factory Morosos.fromJson(Map<String, dynamic> json) => _$MorososFromJson(json);

  Map<String, dynamic> toJson() => _$MorososToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

