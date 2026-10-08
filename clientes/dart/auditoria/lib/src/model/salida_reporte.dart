//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_reporte.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaReporte {
  /// Returns a new [SalidaReporte] instance.
  SalidaReporte({

    required  this.ejecucionId,

    required  this.estado,

    required  this.filasGeneradas,

    required  this.hashResultado,

     this.exportacionId,
  });

  @JsonKey(
    
    name: r'ejecucionId',
    required: true,
    includeIfNull: false,
  )


  final String ejecucionId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaReporteEstadoEnum estado;



      /// Cero es un resultado valido, no un error.
  @JsonKey(
    
    name: r'filasGeneradas',
    required: true,
    includeIfNull: false,
  )


  final int filasGeneradas;



      /// SHA-256 de la forma canonica del resultado. Es lo que permite probar que el archivo que alguien presenta es el que salio de aca.
  @JsonKey(
    
    name: r'hashResultado',
    required: true,
    includeIfNull: false,
  )


  final String hashResultado;



      /// Presente solo si se pidio formato. El archivo se cifra y caduca cuando la definicion tiene datos sensibles; no lo elige quien exporta.
  @JsonKey(
    
    name: r'exportacionId',
    required: false,
    includeIfNull: false,
  )


  final String? exportacionId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaReporte &&
      other.ejecucionId == ejecucionId &&
      other.estado == estado &&
      other.filasGeneradas == filasGeneradas &&
      other.hashResultado == hashResultado &&
      other.exportacionId == exportacionId;

    @override
    int get hashCode =>
        ejecucionId.hashCode +
        estado.hashCode +
        filasGeneradas.hashCode +
        hashResultado.hashCode +
        exportacionId.hashCode;

  factory SalidaReporte.fromJson(Map<String, dynamic> json) => _$SalidaReporteFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaReporteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaReporteEstadoEnum {
@JsonValue(r'COMPLETADA')
COMPLETADA(r'COMPLETADA'),
@JsonValue(r'EJECUTANDO')
EJECUTANDO(r'EJECUTANDO'),
@JsonValue(r'EN_COLA')
EN_COLA(r'EN_COLA'),
@JsonValue(r'EXPIRADA')
EXPIRADA(r'EXPIRADA'),
@JsonValue(r'FALLIDA')
FALLIDA(r'FALLIDA');

const SalidaReporteEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


