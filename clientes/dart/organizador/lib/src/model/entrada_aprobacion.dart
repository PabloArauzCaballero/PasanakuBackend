//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_aprobacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAprobacion {
  /// Returns a new [EntradaAprobacion] instance.
  EntradaAprobacion({

     this.medidos,
  });

  @JsonKey(
    
    name: r'medidos',
    required: false,
    includeIfNull: false,
  )


  final Map<String, String>? medidos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAprobacion &&
      other.medidos == medidos;

    @override
    int get hashCode =>
        medidos.hashCode;

  factory EntradaAprobacion.fromJson(Map<String, dynamic> json) => _$EntradaAprobacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAprobacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

