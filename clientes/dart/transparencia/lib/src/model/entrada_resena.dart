//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_resena.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaResena {
  /// Returns a new [EntradaResena] instance.
  EntradaResena({

    required  this.grupoId,

    required  this.autorParticipanteId,

    required  this.evaluadoUsuarioId,

    required  this.calificacion,

    required  this.dimension,

     this.comentario,

    required  this.cicloCerrado,

     this.cerradoEn,

    required  this.diasDeVentana,

     this.autorFueExpulsado = false,

     this.resenasPreviasDelAutor = 0,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'autorParticipanteId',
    required: true,
    includeIfNull: false,
  )


  final String autorParticipanteId;



  @JsonKey(
    
    name: r'evaluadoUsuarioId',
    required: true,
    includeIfNull: false,
  )


  final String evaluadoUsuarioId;



          // minimum: 1
          // maximum: 5
  @JsonKey(
    
    name: r'calificacion',
    required: true,
    includeIfNull: false,
  )


  final int calificacion;



      /// Catalogo cerrado de ck_resena_participante_dimension.
  @JsonKey(
    
    name: r'dimension',
    required: true,
    includeIfNull: false,
  )


  final EntradaResenaDimensionEnum dimension;



  @JsonKey(
    
    name: r'comentario',
    required: false,
    includeIfNull: false,
  )


  final String? comentario;



  @JsonKey(
    
    name: r'cicloCerrado',
    required: true,
    includeIfNull: false,
  )


  final bool cicloCerrado;



  @JsonKey(
    
    name: r'cerradoEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? cerradoEn;



          // minimum: 1
  @JsonKey(
    
    name: r'diasDeVentana',
    required: true,
    includeIfNull: false,
  )


  final int diasDeVentana;



  @JsonKey(
    defaultValue: false,
    name: r'autorFueExpulsado',
    required: false,
    includeIfNull: false,
  )


  final bool? autorFueExpulsado;



          // minimum: 0
  @JsonKey(
    defaultValue: 0,
    name: r'resenasPreviasDelAutor',
    required: false,
    includeIfNull: false,
  )


  final int? resenasPreviasDelAutor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaResena &&
      other.grupoId == grupoId &&
      other.autorParticipanteId == autorParticipanteId &&
      other.evaluadoUsuarioId == evaluadoUsuarioId &&
      other.calificacion == calificacion &&
      other.dimension == dimension &&
      other.comentario == comentario &&
      other.cicloCerrado == cicloCerrado &&
      other.cerradoEn == cerradoEn &&
      other.diasDeVentana == diasDeVentana &&
      other.autorFueExpulsado == autorFueExpulsado &&
      other.resenasPreviasDelAutor == resenasPreviasDelAutor;

    @override
    int get hashCode =>
        grupoId.hashCode +
        autorParticipanteId.hashCode +
        evaluadoUsuarioId.hashCode +
        calificacion.hashCode +
        dimension.hashCode +
        (comentario == null ? 0 : comentario.hashCode) +
        cicloCerrado.hashCode +
        (cerradoEn == null ? 0 : cerradoEn.hashCode) +
        diasDeVentana.hashCode +
        autorFueExpulsado.hashCode +
        resenasPreviasDelAutor.hashCode;

  factory EntradaResena.fromJson(Map<String, dynamic> json) => _$EntradaResenaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaResenaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Catalogo cerrado de ck_resena_participante_dimension.
enum EntradaResenaDimensionEnum {
    /// Catalogo cerrado de ck_resena_participante_dimension.
@JsonValue(r'PUNTUALIDAD')
PUNTUALIDAD(r'PUNTUALIDAD'),
    /// Catalogo cerrado de ck_resena_participante_dimension.
@JsonValue(r'COMUNICACION')
COMUNICACION(r'COMUNICACION'),
    /// Catalogo cerrado de ck_resena_participante_dimension.
@JsonValue(r'ORGANIZACION')
ORGANIZACION(r'ORGANIZACION');

const EntradaResenaDimensionEnum(this.value);

final String value;

@override
String toString() => value;
}


