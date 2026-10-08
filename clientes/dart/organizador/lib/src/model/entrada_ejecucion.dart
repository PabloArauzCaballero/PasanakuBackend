//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_ejecucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEjecucion {
  /// Returns a new [EntradaEjecucion] instance.
  EntradaEjecucion({

    required  this.iniciada,

    required  this.resultado,

    required  this.registrosAfectados,

    required  this.detalleJson,

     this.mensajeError,
  });

  @JsonKey(
    
    name: r'iniciada',
    required: true,
    includeIfNull: false,
  )


  final DateTime iniciada;



  @JsonKey(
    
    name: r'resultado',
    required: true,
    includeIfNull: false,
  )


  final EntradaEjecucionResultadoEnum resultado;



          // minimum: 0
  @JsonKey(
    
    name: r'registrosAfectados',
    required: true,
    includeIfNull: false,
  )


  final int registrosAfectados;



  @JsonKey(
    
    name: r'detalleJson',
    required: true,
    includeIfNull: false,
  )


  final String detalleJson;



  @JsonKey(
    
    name: r'mensajeError',
    required: false,
    includeIfNull: false,
  )


  final String? mensajeError;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEjecucion &&
      other.iniciada == iniciada &&
      other.resultado == resultado &&
      other.registrosAfectados == registrosAfectados &&
      other.detalleJson == detalleJson &&
      other.mensajeError == mensajeError;

    @override
    int get hashCode =>
        iniciada.hashCode +
        resultado.hashCode +
        registrosAfectados.hashCode +
        detalleJson.hashCode +
        (mensajeError == null ? 0 : mensajeError.hashCode);

  factory EntradaEjecucion.fromJson(Map<String, dynamic> json) => _$EntradaEjecucionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEjecucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaEjecucionResultadoEnum {
@JsonValue(r'EXITO')
EXITO(r'EXITO'),
@JsonValue(r'PARCIAL')
PARCIAL(r'PARCIAL'),
@JsonValue(r'ERROR')
ERROR(r'ERROR');

const EntradaEjecucionResultadoEnum(this.value);

final String value;

@override
String toString() => value;
}


