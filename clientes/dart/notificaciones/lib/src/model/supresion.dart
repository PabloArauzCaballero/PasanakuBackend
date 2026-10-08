//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'supresion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Supresion {
  /// Returns a new [Supresion] instance.
  Supresion({

    required  this.suprimido,
  });

  @JsonKey(
    
    name: r'suprimido',
    required: true,
    includeIfNull: false,
  )


  final bool suprimido;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Supresion &&
      other.suprimido == suprimido;

    @override
    int get hashCode =>
        suprimido.hashCode;

  factory Supresion.fromJson(Map<String, dynamic> json) => _$SupresionFromJson(json);

  Map<String, dynamic> toJson() => _$SupresionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

