//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_compensacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCompensacion {
  /// Returns a new [EntradaCompensacion] instance.
  EntradaCompensacion({

    required  this.descripcion,
  });

  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCompensacion &&
      other.descripcion == descripcion;

    @override
    int get hashCode =>
        descripcion.hashCode;

  factory EntradaCompensacion.fromJson(Map<String, dynamic> json) => _$EntradaCompensacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCompensacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

