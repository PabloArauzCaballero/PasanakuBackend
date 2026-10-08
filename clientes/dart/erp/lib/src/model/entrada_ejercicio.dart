//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_ejercicio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEjercicio {
  /// Returns a new [EntradaEjercicio] instance.
  EntradaEjercicio({

    required  this.anio,
  });

          // minimum: 2000
          // maximum: 2999
  @JsonKey(
    
    name: r'anio',
    required: true,
    includeIfNull: false,
  )


  final int anio;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEjercicio &&
      other.anio == anio;

    @override
    int get hashCode =>
        anio.hashCode;

  factory EntradaEjercicio.fromJson(Map<String, dynamic> json) => _$EntradaEjercicioFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEjercicioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

