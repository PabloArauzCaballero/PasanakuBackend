//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_tercero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTercero {
  /// Returns a new [SalidaTercero] instance.
  SalidaTercero({

    required  this.terceroComercialId,
  });

  @JsonKey(
    
    name: r'terceroComercialId',
    required: true,
    includeIfNull: false,
  )


  final String terceroComercialId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTercero &&
      other.terceroComercialId == terceroComercialId;

    @override
    int get hashCode =>
        terceroComercialId.hashCode;

  factory SalidaTercero.fromJson(Map<String, dynamic> json) => _$SalidaTerceroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTerceroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

