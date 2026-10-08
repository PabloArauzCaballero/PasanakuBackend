//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_declaracion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDeclaracion {
  /// Returns a new [SalidaDeclaracion] instance.
  SalidaDeclaracion({

    required  this.expedienteId,

    required  this.codigoExpediente,

    required  this.estado,

    required  this.puedeDescargarHasta,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'expedienteId',
    required: true,
    includeIfNull: false,
  )


  final String expedienteId;



  @JsonKey(
    
    name: r'codigoExpediente',
    required: true,
    includeIfNull: false,
  )


  final String codigoExpediente;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



      /// se calcula al NOTIFICAR y se guarda; no se recalcula (R-GAR-01)
  @JsonKey(
    
    name: r'puedeDescargarHasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime puedeDescargarHasta;



  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDeclaracion &&
      other.expedienteId == expedienteId &&
      other.codigoExpediente == codigoExpediente &&
      other.estado == estado &&
      other.puedeDescargarHasta == puedeDescargarHasta &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        expedienteId.hashCode +
        codigoExpediente.hashCode +
        estado.hashCode +
        puedeDescargarHasta.hashCode +
        esNuevo.hashCode;

  factory SalidaDeclaracion.fromJson(Map<String, dynamic> json) => _$SalidaDeclaracionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDeclaracionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

