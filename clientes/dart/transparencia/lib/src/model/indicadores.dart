//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'indicadores.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Indicadores {
  /// Returns a new [Indicadores] instance.
  Indicadores({

    required  this.puntualidad,

    required  this.incumplimiento,

    required  this.montoAportado,

    required  this.gruposCompletados,

    required  this.gruposAbandonados,

    required  this.incumplimientosAbiertos,

    required  this.antiguedadMeses,
  });

  @JsonKey(
    
    name: r'puntualidad',
    required: true,
    includeIfNull: false,
  )


  final String puntualidad;



  @JsonKey(
    
    name: r'incumplimiento',
    required: true,
    includeIfNull: false,
  )


  final String incumplimiento;



      /// Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
  @JsonKey(
    
    name: r'montoAportado',
    required: true,
    includeIfNull: false,
  )


  final String montoAportado;



          // minimum: 0
  @JsonKey(
    
    name: r'gruposCompletados',
    required: true,
    includeIfNull: false,
  )


  final int gruposCompletados;



          // minimum: 0
  @JsonKey(
    
    name: r'gruposAbandonados',
    required: true,
    includeIfNull: false,
  )


  final int gruposAbandonados;



          // minimum: 0
  @JsonKey(
    
    name: r'incumplimientosAbiertos',
    required: true,
    includeIfNull: false,
  )


  final int incumplimientosAbiertos;



          // minimum: 0
  @JsonKey(
    
    name: r'antiguedadMeses',
    required: true,
    includeIfNull: false,
  )


  final int antiguedadMeses;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Indicadores &&
      other.puntualidad == puntualidad &&
      other.incumplimiento == incumplimiento &&
      other.montoAportado == montoAportado &&
      other.gruposCompletados == gruposCompletados &&
      other.gruposAbandonados == gruposAbandonados &&
      other.incumplimientosAbiertos == incumplimientosAbiertos &&
      other.antiguedadMeses == antiguedadMeses;

    @override
    int get hashCode =>
        puntualidad.hashCode +
        incumplimiento.hashCode +
        montoAportado.hashCode +
        gruposCompletados.hashCode +
        gruposAbandonados.hashCode +
        incumplimientosAbiertos.hashCode +
        antiguedadMeses.hashCode;

  factory Indicadores.fromJson(Map<String, dynamic> json) => _$IndicadoresFromJson(json);

  Map<String, dynamic> toJson() => _$IndicadoresToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

