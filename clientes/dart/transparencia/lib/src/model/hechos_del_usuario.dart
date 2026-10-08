//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'hechos_del_usuario.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class HechosDelUsuario {
  /// Returns a new [HechosDelUsuario] instance.
  HechosDelUsuario({

    required  this.ciclosCompletados,

    required  this.ciclosCompletadosSinCobertura,

    required  this.aportesPuntualesConsecutivos,

    required  this.diasCorridosSinMora,

    required  this.regularizacionesCumplidasEnPlazo,

    required  this.gruposCerradosComoOrganizador,

    required  this.sancionesFirmesComoOrganizador,

    required  this.entregasFueraDePlazoComoOrganizador,

     this.desempenoComoOrganizador,

    required  this.invitadosQueCompletaronUnCiclo,

    required  this.mesesDeAntiguedad,

    required  this.identidadVigente,

    required  this.tieneDeudaCastigada,
  });

          // minimum: 0
  @JsonKey(
    
    name: r'ciclosCompletados',
    required: true,
    includeIfNull: false,
  )


  final int ciclosCompletados;



          // minimum: 0
  @JsonKey(
    
    name: r'ciclosCompletadosSinCobertura',
    required: true,
    includeIfNull: false,
  )


  final int ciclosCompletadosSinCobertura;



          // minimum: 0
  @JsonKey(
    
    name: r'aportesPuntualesConsecutivos',
    required: true,
    includeIfNull: false,
  )


  final int aportesPuntualesConsecutivos;



          // minimum: 0
  @JsonKey(
    
    name: r'diasCorridosSinMora',
    required: true,
    includeIfNull: false,
  )


  final int diasCorridosSinMora;



          // minimum: 0
  @JsonKey(
    
    name: r'regularizacionesCumplidasEnPlazo',
    required: true,
    includeIfNull: false,
  )


  final int regularizacionesCumplidasEnPlazo;



          // minimum: 0
  @JsonKey(
    
    name: r'gruposCerradosComoOrganizador',
    required: true,
    includeIfNull: false,
  )


  final int gruposCerradosComoOrganizador;



          // minimum: 0
  @JsonKey(
    
    name: r'sancionesFirmesComoOrganizador',
    required: true,
    includeIfNull: false,
  )


  final int sancionesFirmesComoOrganizador;



          // minimum: 0
  @JsonKey(
    
    name: r'entregasFueraDePlazoComoOrganizador',
    required: true,
    includeIfNull: false,
  )


  final int entregasFueraDePlazoComoOrganizador;



  @JsonKey(
    
    name: r'desempenoComoOrganizador',
    required: false,
    includeIfNull: false,
  )


  final String? desempenoComoOrganizador;



          // minimum: 0
  @JsonKey(
    
    name: r'invitadosQueCompletaronUnCiclo',
    required: true,
    includeIfNull: false,
  )


  final int invitadosQueCompletaronUnCiclo;



          // minimum: 0
  @JsonKey(
    
    name: r'mesesDeAntiguedad',
    required: true,
    includeIfNull: false,
  )


  final int mesesDeAntiguedad;



  @JsonKey(
    
    name: r'identidadVigente',
    required: true,
    includeIfNull: false,
  )


  final bool identidadVigente;



  @JsonKey(
    
    name: r'tieneDeudaCastigada',
    required: true,
    includeIfNull: false,
  )


  final bool tieneDeudaCastigada;





    @override
    bool operator ==(Object other) => identical(this, other) || other is HechosDelUsuario &&
      other.ciclosCompletados == ciclosCompletados &&
      other.ciclosCompletadosSinCobertura == ciclosCompletadosSinCobertura &&
      other.aportesPuntualesConsecutivos == aportesPuntualesConsecutivos &&
      other.diasCorridosSinMora == diasCorridosSinMora &&
      other.regularizacionesCumplidasEnPlazo == regularizacionesCumplidasEnPlazo &&
      other.gruposCerradosComoOrganizador == gruposCerradosComoOrganizador &&
      other.sancionesFirmesComoOrganizador == sancionesFirmesComoOrganizador &&
      other.entregasFueraDePlazoComoOrganizador == entregasFueraDePlazoComoOrganizador &&
      other.desempenoComoOrganizador == desempenoComoOrganizador &&
      other.invitadosQueCompletaronUnCiclo == invitadosQueCompletaronUnCiclo &&
      other.mesesDeAntiguedad == mesesDeAntiguedad &&
      other.identidadVigente == identidadVigente &&
      other.tieneDeudaCastigada == tieneDeudaCastigada;

    @override
    int get hashCode =>
        ciclosCompletados.hashCode +
        ciclosCompletadosSinCobertura.hashCode +
        aportesPuntualesConsecutivos.hashCode +
        diasCorridosSinMora.hashCode +
        regularizacionesCumplidasEnPlazo.hashCode +
        gruposCerradosComoOrganizador.hashCode +
        sancionesFirmesComoOrganizador.hashCode +
        entregasFueraDePlazoComoOrganizador.hashCode +
        (desempenoComoOrganizador == null ? 0 : desempenoComoOrganizador.hashCode) +
        invitadosQueCompletaronUnCiclo.hashCode +
        mesesDeAntiguedad.hashCode +
        identidadVigente.hashCode +
        tieneDeudaCastigada.hashCode;

  factory HechosDelUsuario.fromJson(Map<String, dynamic> json) => _$HechosDelUsuarioFromJson(json);

  Map<String, dynamic> toJson() => _$HechosDelUsuarioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

