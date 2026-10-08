//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_postulacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPostulacion {
  /// Returns a new [EntradaPostulacion] instance.
  EntradaPostulacion({

    required  this.motivacion,

    required  this.experienciaDeclarada,

    required  this.kycReforzadoId,

    required  this.reputacion,

     this.medidos,
  });

  @JsonKey(
    
    name: r'motivacion',
    required: true,
    includeIfNull: false,
  )


  final String motivacion;



  @JsonKey(
    
    name: r'experienciaDeclarada',
    required: true,
    includeIfNull: false,
  )


  final String experienciaDeclarada;



      /// sin diligencia reforzada no se administra plata ajena (R-UIF-09)
  @JsonKey(
    
    name: r'kycReforzadoId',
    required: true,
    includeIfNull: false,
  )


  final String kycReforzadoId;



  @JsonKey(
    
    name: r'reputacion',
    required: true,
    includeIfNull: false,
  )


  final String reputacion;



      /// lo que se sabe del postulante por codigo de requisito. Un requisito obligatorio SIN dato no se da por cumplido (invariante 9)
  @JsonKey(
    
    name: r'medidos',
    required: false,
    includeIfNull: false,
  )


  final Map<String, String>? medidos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPostulacion &&
      other.motivacion == motivacion &&
      other.experienciaDeclarada == experienciaDeclarada &&
      other.kycReforzadoId == kycReforzadoId &&
      other.reputacion == reputacion &&
      other.medidos == medidos;

    @override
    int get hashCode =>
        motivacion.hashCode +
        experienciaDeclarada.hashCode +
        kycReforzadoId.hashCode +
        reputacion.hashCode +
        medidos.hashCode;

  factory EntradaPostulacion.fromJson(Map<String, dynamic> json) => _$EntradaPostulacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPostulacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

