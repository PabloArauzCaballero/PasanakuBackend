//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/metrica_de_riesgo.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_evaluacion_riesgo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEvaluacionRiesgo {
  /// Returns a new [EntradaEvaluacionRiesgo] instance.
  EntradaEvaluacionRiesgo({

    required  this.ambito,

     this.ambitoId,

     this.periodoId,

     this.periodoCerrado = true,

    required  this.metricas,

    required  this.codigoDeAlerta,

    required  this.observaciones,

     this.puntajeDeRiesgo,
  });

      /// Catalogo cerrado de ck_alerta_riesgo_ambito.
  @JsonKey(
    
    name: r'ambito',
    required: true,
    includeIfNull: false,
  )


  final EntradaEvaluacionRiesgoAmbitoEnum ambito;



  @JsonKey(
    
    name: r'ambitoId',
    required: false,
    includeIfNull: false,
  )


  final String? ambitoId;



  @JsonKey(
    
    name: r'periodoId',
    required: false,
    includeIfNull: false,
  )


  final String? periodoId;



  @JsonKey(
    defaultValue: true,
    name: r'periodoCerrado',
    required: false,
    includeIfNull: false,
  )


  final bool? periodoCerrado;



  @JsonKey(
    
    name: r'metricas',
    required: true,
    includeIfNull: false,
  )


  final List<MetricaDeRiesgo> metricas;



      /// De codigo de metrica al codigo de alerta. Es catalogo, no constante.
  @JsonKey(
    
    name: r'codigoDeAlerta',
    required: true,
    includeIfNull: false,
  )


  final Map<String, EntradaEvaluacionRiesgoCodigoDeAlertaEnum> codigoDeAlerta;



          // minimum: 0
  @JsonKey(
    
    name: r'observaciones',
    required: true,
    includeIfNull: false,
  )


  final int observaciones;



      /// NO sale de aca hacia el usuario. Quien conoce el numero lo puede jugar.
  @JsonKey(
    
    name: r'puntajeDeRiesgo',
    required: false,
    includeIfNull: false,
  )


  final String? puntajeDeRiesgo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEvaluacionRiesgo &&
      other.ambito == ambito &&
      other.ambitoId == ambitoId &&
      other.periodoId == periodoId &&
      other.periodoCerrado == periodoCerrado &&
      other.metricas == metricas &&
      other.codigoDeAlerta == codigoDeAlerta &&
      other.observaciones == observaciones &&
      other.puntajeDeRiesgo == puntajeDeRiesgo;

    @override
    int get hashCode =>
        ambito.hashCode +
        (ambitoId == null ? 0 : ambitoId.hashCode) +
        (periodoId == null ? 0 : periodoId.hashCode) +
        periodoCerrado.hashCode +
        metricas.hashCode +
        codigoDeAlerta.hashCode +
        observaciones.hashCode +
        (puntajeDeRiesgo == null ? 0 : puntajeDeRiesgo.hashCode);

  factory EntradaEvaluacionRiesgo.fromJson(Map<String, dynamic> json) => _$EntradaEvaluacionRiesgoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEvaluacionRiesgoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Catalogo cerrado de ck_alerta_riesgo_ambito.
enum EntradaEvaluacionRiesgoAmbitoEnum {
    /// Catalogo cerrado de ck_alerta_riesgo_ambito.
@JsonValue(r'GRUPO')
GRUPO(r'GRUPO'),
    /// Catalogo cerrado de ck_alerta_riesgo_ambito.
@JsonValue(r'ORGANIZADOR')
ORGANIZADOR(r'ORGANIZADOR'),
    /// Catalogo cerrado de ck_alerta_riesgo_ambito.
@JsonValue(r'USUARIO')
USUARIO(r'USUARIO');

const EntradaEvaluacionRiesgoAmbitoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaEvaluacionRiesgoCodigoDeAlertaEnum {
@JsonValue(r'CAIDA_ABRUPTA_SCORE')
CAIDA_ABRUPTA_SCORE(r'CAIDA_ABRUPTA_SCORE'),
@JsonValue(r'GRUPO_INVIABLE')
GRUPO_INVIABLE(r'GRUPO_INVIABLE'),
@JsonValue(r'MORA_CONCENTRADA')
MORA_CONCENTRADA(r'MORA_CONCENTRADA'),
@JsonValue(r'RETIRO_MASIVO')
RETIRO_MASIVO(r'RETIRO_MASIVO');

const EntradaEvaluacionRiesgoCodigoDeAlertaEnum(this.value);

final String value;

@override
String toString() => value;
}


