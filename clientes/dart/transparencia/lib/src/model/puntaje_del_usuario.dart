//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'puntaje_del_usuario.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class PuntajeDelUsuario {
  /// Returns a new [PuntajeDelUsuario] instance.
  PuntajeDelUsuario({

    required  this.tieneHistorial,

    required  this.puntaje,

    required  this.nivelDeConfianza,
  });

  @JsonKey(
    
    name: r'tieneHistorial',
    required: true,
    includeIfNull: false,
  )


  final bool tieneHistorial;



  @JsonKey(
    
    name: r'puntaje',
    required: true,
    includeIfNull: false,
  )


  final String puntaje;



  @JsonKey(
    
    name: r'nivelDeConfianza',
    required: true,
    includeIfNull: false,
  )


  final String nivelDeConfianza;





    @override
    bool operator ==(Object other) => identical(this, other) || other is PuntajeDelUsuario &&
      other.tieneHistorial == tieneHistorial &&
      other.puntaje == puntaje &&
      other.nivelDeConfianza == nivelDeConfianza;

    @override
    int get hashCode =>
        tieneHistorial.hashCode +
        puntaje.hashCode +
        nivelDeConfianza.hashCode;

  factory PuntajeDelUsuario.fromJson(Map<String, dynamic> json) => _$PuntajeDelUsuarioFromJson(json);

  Map<String, dynamic> toJson() => _$PuntajeDelUsuarioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

