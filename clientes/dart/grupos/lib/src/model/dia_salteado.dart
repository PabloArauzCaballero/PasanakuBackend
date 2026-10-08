//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'dia_salteado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class DiaSalteado {
  /// Returns a new [DiaSalteado] instance.
  DiaSalteado({

    required  this.fecha,

    required  this.descripcion,
  });

  @JsonKey(
    
    name: r'fecha',
    required: true,
    includeIfNull: false,
  )


  final DateTime fecha;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is DiaSalteado &&
      other.fecha == fecha &&
      other.descripcion == descripcion;

    @override
    int get hashCode =>
        fecha.hashCode +
        descripcion.hashCode;

  factory DiaSalteado.fromJson(Map<String, dynamic> json) => _$DiaSalteadoFromJson(json);

  Map<String, dynamic> toJson() => _$DiaSalteadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

