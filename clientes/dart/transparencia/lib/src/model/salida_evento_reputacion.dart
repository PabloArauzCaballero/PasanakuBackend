//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_evento_reputacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEventoReputacion {
  /// Returns a new [SalidaEventoReputacion] instance.
  SalidaEventoReputacion({

    required  this.eventoId,

    required  this.impacto,

     this.factorAfectado,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'eventoId',
    required: true,
    includeIfNull: false,
  )


  final String eventoId;



      /// Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
  @JsonKey(
    
    name: r'impacto',
    required: true,
    includeIfNull: false,
  )


  final String impacto;



  @JsonKey(
    
    name: r'factorAfectado',
    required: false,
    includeIfNull: false,
  )


  final String? factorAfectado;



      /// false cuando el hecho ya estaba puntuado (R-REP-01). El reintento es inocuo.
  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEventoReputacion &&
      other.eventoId == eventoId &&
      other.impacto == impacto &&
      other.factorAfectado == factorAfectado &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        eventoId.hashCode +
        impacto.hashCode +
        (factorAfectado == null ? 0 : factorAfectado.hashCode) +
        esNuevo.hashCode;

  factory SalidaEventoReputacion.fromJson(Map<String, dynamic> json) => _$SalidaEventoReputacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEventoReputacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

