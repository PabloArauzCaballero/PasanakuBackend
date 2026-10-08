//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_organizador/src/model/metrica.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_evaluacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEvaluacion {
  /// Returns a new [EntradaEvaluacion] instance.
  EntradaEvaluacion({

    required  this.periodo,

    required  this.metricas,

    required  this.morosidad,

    required  this.tasaFinalizacion,

    required  this.satisfaccion,

    required  this.tiempoRespuestaHoras,

    required  this.incidenciasAbiertas,

    required  this.coberturasConsumidas,

    required  this.limiteDeGruposDelNivel,

    required  this.limiteDeMontoDelNivel,
  });

  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



  @JsonKey(
    
    name: r'metricas',
    required: true,
    includeIfNull: false,
  )


  final List<Metrica> metricas;



  @JsonKey(
    
    name: r'morosidad',
    required: true,
    includeIfNull: false,
  )


  final String morosidad;



  @JsonKey(
    
    name: r'tasaFinalizacion',
    required: true,
    includeIfNull: false,
  )


  final String tasaFinalizacion;



  @JsonKey(
    
    name: r'satisfaccion',
    required: true,
    includeIfNull: false,
  )


  final String satisfaccion;



  @JsonKey(
    
    name: r'tiempoRespuestaHoras',
    required: true,
    includeIfNull: false,
  )


  final String tiempoRespuestaHoras;



          // minimum: 0
  @JsonKey(
    
    name: r'incidenciasAbiertas',
    required: true,
    includeIfNull: false,
  )


  final int incidenciasAbiertas;



          // minimum: 0
  @JsonKey(
    
    name: r'coberturasConsumidas',
    required: true,
    includeIfNull: false,
  )


  final int coberturasConsumidas;



          // minimum: 1
  @JsonKey(
    
    name: r'limiteDeGruposDelNivel',
    required: true,
    includeIfNull: false,
  )


  final int limiteDeGruposDelNivel;



  @JsonKey(
    
    name: r'limiteDeMontoDelNivel',
    required: true,
    includeIfNull: false,
  )


  final String limiteDeMontoDelNivel;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEvaluacion &&
      other.periodo == periodo &&
      other.metricas == metricas &&
      other.morosidad == morosidad &&
      other.tasaFinalizacion == tasaFinalizacion &&
      other.satisfaccion == satisfaccion &&
      other.tiempoRespuestaHoras == tiempoRespuestaHoras &&
      other.incidenciasAbiertas == incidenciasAbiertas &&
      other.coberturasConsumidas == coberturasConsumidas &&
      other.limiteDeGruposDelNivel == limiteDeGruposDelNivel &&
      other.limiteDeMontoDelNivel == limiteDeMontoDelNivel;

    @override
    int get hashCode =>
        periodo.hashCode +
        metricas.hashCode +
        morosidad.hashCode +
        tasaFinalizacion.hashCode +
        satisfaccion.hashCode +
        tiempoRespuestaHoras.hashCode +
        incidenciasAbiertas.hashCode +
        coberturasConsumidas.hashCode +
        limiteDeGruposDelNivel.hashCode +
        limiteDeMontoDelNivel.hashCode;

  factory EntradaEvaluacion.fromJson(Map<String, dynamic> json) => _$EntradaEvaluacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEvaluacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

