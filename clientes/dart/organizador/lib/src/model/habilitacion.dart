//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'habilitacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Habilitacion {
  /// Returns a new [Habilitacion] instance.
  Habilitacion({

    required  this.habilitado,

    required  this.nivel,

    required  this.limiteDeGrupos,

    required  this.limiteDeMonto,

    required  this.gruposActivos,
  });

  @JsonKey(
    
    name: r'habilitado',
    required: true,
    includeIfNull: false,
  )


  final bool habilitado;



  @JsonKey(
    
    name: r'nivel',
    required: true,
    includeIfNull: false,
  )


  final String nivel;



  @JsonKey(
    
    name: r'limiteDeGrupos',
    required: true,
    includeIfNull: false,
  )


  final int limiteDeGrupos;



  @JsonKey(
    
    name: r'limiteDeMonto',
    required: true,
    includeIfNull: false,
  )


  final String limiteDeMonto;



  @JsonKey(
    
    name: r'gruposActivos',
    required: true,
    includeIfNull: false,
  )


  final int gruposActivos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Habilitacion &&
      other.habilitado == habilitado &&
      other.nivel == nivel &&
      other.limiteDeGrupos == limiteDeGrupos &&
      other.limiteDeMonto == limiteDeMonto &&
      other.gruposActivos == gruposActivos;

    @override
    int get hashCode =>
        habilitado.hashCode +
        nivel.hashCode +
        limiteDeGrupos.hashCode +
        limiteDeMonto.hashCode +
        gruposActivos.hashCode;

  factory Habilitacion.fromJson(Map<String, dynamic> json) => _$HabilitacionFromJson(json);

  Map<String, dynamic> toJson() => _$HabilitacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

