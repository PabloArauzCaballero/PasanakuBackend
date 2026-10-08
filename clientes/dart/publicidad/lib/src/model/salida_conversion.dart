//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_conversion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaConversion {
  /// Returns a new [SalidaConversion] instance.
  SalidaConversion({

    required  this.conversionId,
  });

  @JsonKey(
    
    name: r'conversionId',
    required: true,
    includeIfNull: false,
  )


  final String conversionId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaConversion &&
      other.conversionId == conversionId;

    @override
    int get hashCode =>
        conversionId.hashCode;

  factory SalidaConversion.fromJson(Map<String, dynamic> json) => _$SalidaConversionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaConversionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

