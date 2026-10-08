//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_depreciacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDepreciacion {
  /// Returns a new [EntradaDepreciacion] instance.
  EntradaDepreciacion({

    required  this.periodoContableId,
  });

  @JsonKey(
    
    name: r'periodoContableId',
    required: true,
    includeIfNull: false,
  )


  final String periodoContableId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDepreciacion &&
      other.periodoContableId == periodoContableId;

    @override
    int get hashCode =>
        periodoContableId.hashCode;

  factory EntradaDepreciacion.fromJson(Map<String, dynamic> json) => _$EntradaDepreciacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDepreciacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

