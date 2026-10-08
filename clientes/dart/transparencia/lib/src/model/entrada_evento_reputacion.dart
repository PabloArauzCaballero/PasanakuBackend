//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_evento_reputacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEventoReputacion {
  /// Returns a new [EntradaEventoReputacion] instance.
  EntradaEventoReputacion({

    required  this.usuarioId,

     this.grupoId,

     this.participanteId,

    required  this.tipo,

    required  this.referenciaTipo,

     this.referenciaId,

    required  this.descripcion,

     this.esReversible = false,

    required  this.ocurridoEn,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'grupoId',
    required: false,
    includeIfNull: false,
  )


  final String? grupoId;



  @JsonKey(
    
    name: r'participanteId',
    required: false,
    includeIfNull: false,
  )


  final String? participanteId;



      /// Catalogo cerrado de ck_evento_reputacion_tipo.
  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaEventoReputacionTipoEnum tipo;



  @JsonKey(
    
    name: r'referenciaTipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaEventoReputacionReferenciaTipoEnum referenciaTipo;



  @JsonKey(
    
    name: r'referenciaId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaId;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    defaultValue: false,
    name: r'esReversible',
    required: false,
    includeIfNull: false,
  )


  final bool? esReversible;



  @JsonKey(
    
    name: r'ocurridoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime ocurridoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEventoReputacion &&
      other.usuarioId == usuarioId &&
      other.grupoId == grupoId &&
      other.participanteId == participanteId &&
      other.tipo == tipo &&
      other.referenciaTipo == referenciaTipo &&
      other.referenciaId == referenciaId &&
      other.descripcion == descripcion &&
      other.esReversible == esReversible &&
      other.ocurridoEn == ocurridoEn;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        (grupoId == null ? 0 : grupoId.hashCode) +
        (participanteId == null ? 0 : participanteId.hashCode) +
        tipo.hashCode +
        referenciaTipo.hashCode +
        (referenciaId == null ? 0 : referenciaId.hashCode) +
        descripcion.hashCode +
        esReversible.hashCode +
        ocurridoEn.hashCode;

  factory EntradaEventoReputacion.fromJson(Map<String, dynamic> json) => _$EntradaEventoReputacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEventoReputacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Catalogo cerrado de ck_evento_reputacion_tipo.
enum EntradaEventoReputacionTipoEnum {
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'ABANDONO_DE_GRUPO')
ABANDONO_DE_GRUPO(r'ABANDONO_DE_GRUPO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'APORTE_ANTICIPADO')
APORTE_ANTICIPADO(r'APORTE_ANTICIPADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'APORTE_CON_RETRASO_GRAVE')
APORTE_CON_RETRASO_GRAVE(r'APORTE_CON_RETRASO_GRAVE'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'APORTE_CON_RETRASO_LEVE')
APORTE_CON_RETRASO_LEVE(r'APORTE_CON_RETRASO_LEVE'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'APORTE_NO_REALIZADO')
APORTE_NO_REALIZADO(r'APORTE_NO_REALIZADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'APORTE_PUNTUAL')
APORTE_PUNTUAL(r'APORTE_PUNTUAL'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'AVAL_EJECUTADO')
AVAL_EJECUTADO(r'AVAL_EJECUTADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'COBERTURA_CONSUMIDA')
COBERTURA_CONSUMIDA(r'COBERTURA_CONSUMIDA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'CONFIRMACION_ENTREGA_A_TIEMPO')
CONFIRMACION_ENTREGA_A_TIEMPO(r'CONFIRMACION_ENTREGA_A_TIEMPO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'DEUDA_CASTIGADA')
DEUDA_CASTIGADA(r'DEUDA_CASTIGADA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'DEUDA_RECUPERADA')
DEUDA_RECUPERADA(r'DEUDA_RECUPERADA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'EXPULSION')
EXPULSION(r'EXPULSION'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'FRAUDE_CONFIRMADO')
FRAUDE_CONFIRMADO(r'FRAUDE_CONFIRMADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'GRUPO_COMPLETADO')
GRUPO_COMPLETADO(r'GRUPO_COMPLETADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'KYC_COMPLETADO')
KYC_COMPLETADO(r'KYC_COMPLETADO'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'RESENA_NEGATIVA')
RESENA_NEGATIVA(r'RESENA_NEGATIVA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'RESENA_POSITIVA')
RESENA_POSITIVA(r'RESENA_POSITIVA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'SANCION_APLICADA')
SANCION_APLICADA(r'SANCION_APLICADA'),
    /// Catalogo cerrado de ck_evento_reputacion_tipo.
@JsonValue(r'SANCION_REVOCADA')
SANCION_REVOCADA(r'SANCION_REVOCADA');

const EntradaEventoReputacionTipoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaEventoReputacionReferenciaTipoEnum {
@JsonValue(r'COBERTURA_INCUMPLIMIENTO')
COBERTURA_INCUMPLIMIENTO(r'COBERTURA_INCUMPLIMIENTO'),
@JsonValue(r'ENTREGA_FONDO')
ENTREGA_FONDO(r'ENTREGA_FONDO'),
@JsonValue(r'GRUPO')
GRUPO(r'GRUPO'),
@JsonValue(r'OBLIGACION_APORTE')
OBLIGACION_APORTE(r'OBLIGACION_APORTE'),
@JsonValue(r'PARTICIPANTE')
PARTICIPANTE(r'PARTICIPANTE'),
@JsonValue(r'REGISTRO_INCUMPLIMIENTO')
REGISTRO_INCUMPLIMIENTO(r'REGISTRO_INCUMPLIMIENTO'),
@JsonValue(r'RESENA_PARTICIPANTE')
RESENA_PARTICIPANTE(r'RESENA_PARTICIPANTE'),
@JsonValue(r'TRASPASO_CUPO')
TRASPASO_CUPO(r'TRASPASO_CUPO');

const EntradaEventoReputacionReferenciaTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


