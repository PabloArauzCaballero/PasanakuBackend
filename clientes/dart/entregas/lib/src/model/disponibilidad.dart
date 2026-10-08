//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'disponibilidad.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Disponibilidad {
  /// Returns a new [Disponibilidad] instance.
  Disponibilidad({

    required  this.disponible,

    required  this.restanteSegundos,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'disponible',
    required: true,
    includeIfNull: false,
  )


  final bool disponible;



          // minimum: 0
  @JsonKey(
    
    name: r'restanteSegundos',
    required: true,
    includeIfNull: false,
  )


  final int restanteSegundos;



      /// en texto; negar sin explicar convierte una medida de seguridad en una falla
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Disponibilidad &&
      other.disponible == disponible &&
      other.restanteSegundos == restanteSegundos &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        disponible.hashCode +
        restanteSegundos.hashCode +
        motivo.hashCode;

  factory Disponibilidad.fromJson(Map<String, dynamic> json) => _$DisponibilidadFromJson(json);

  Map<String, dynamic> toJson() => _$DisponibilidadToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

