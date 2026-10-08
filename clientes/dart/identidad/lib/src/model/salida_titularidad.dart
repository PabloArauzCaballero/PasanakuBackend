//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_titularidad.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTitularidad {
  /// Returns a new [SalidaTitularidad] instance.
  SalidaTitularidad({

    required  this.coincide,
  });

  @JsonKey(
    
    name: r'coincide',
    required: true,
    includeIfNull: false,
  )


  final bool coincide;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTitularidad &&
      other.coincide == coincide;

    @override
    int get hashCode =>
        coincide.hashCode;

  factory SalidaTitularidad.fromJson(Map<String, dynamic> json) => _$SalidaTitularidadFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTitularidadToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

