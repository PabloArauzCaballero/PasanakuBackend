//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/metrica_de_riesgo.dart';
import 'package:aportaya_cliente_transparencia/src/model/alerta_de_riesgo.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_evaluacion_riesgo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEvaluacionRiesgo {
  /// Returns a new [SalidaEvaluacionRiesgo] instance.
  SalidaEvaluacionRiesgo({

    required  this.alertas,

    required  this.nivelRiesgo,

    required  this.versionModelo,

    required  this.metricasEnAlerta,
  });

  @JsonKey(
    
    name: r'alertas',
    required: true,
    includeIfNull: false,
  )


  final List<AlertaDeRiesgo> alertas;



      /// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
  @JsonKey(
    
    name: r'nivelRiesgo',
    required: true,
    includeIfNull: false,
  )


  final SalidaEvaluacionRiesgoNivelRiesgoEnum nivelRiesgo;



  @JsonKey(
    
    name: r'versionModelo',
    required: true,
    includeIfNull: false,
  )


  final String versionModelo;



  @JsonKey(
    
    name: r'metricasEnAlerta',
    required: true,
    includeIfNull: false,
  )


  final List<MetricaDeRiesgo> metricasEnAlerta;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEvaluacionRiesgo &&
      other.alertas == alertas &&
      other.nivelRiesgo == nivelRiesgo &&
      other.versionModelo == versionModelo &&
      other.metricasEnAlerta == metricasEnAlerta;

    @override
    int get hashCode =>
        alertas.hashCode +
        nivelRiesgo.hashCode +
        versionModelo.hashCode +
        metricasEnAlerta.hashCode;

  factory SalidaEvaluacionRiesgo.fromJson(Map<String, dynamic> json) => _$SalidaEvaluacionRiesgoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEvaluacionRiesgoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
enum SalidaEvaluacionRiesgoNivelRiesgoEnum {
    /// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
@JsonValue(r'BAJO')
BAJO(r'BAJO'),
    /// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
@JsonValue(r'MEDIO')
MEDIO(r'MEDIO'),
    /// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
@JsonValue(r'ALTO')
ALTO(r'ALTO'),
    /// Sin historial es SIN_DATOS, no riesgo alto. La ausencia de datos es ausencia de datos.
@JsonValue(r'SIN_DATOS')
SIN_DATOS(r'SIN_DATOS');

const SalidaEvaluacionRiesgoNivelRiesgoEnum(this.value);

final String value;

@override
String toString() => value;
}


