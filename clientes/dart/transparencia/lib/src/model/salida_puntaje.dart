//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/componente.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_puntaje.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPuntaje {
  /// Returns a new [SalidaPuntaje] instance.
  SalidaPuntaje({

    required  this.puntajeId,

    required  this.puntaje,

    required  this.nivelDeConfianza,

    required  this.componentes,

     this.puntajeAnterior,

    required  this.eventosConsiderados,
  });

  @JsonKey(
    
    name: r'puntajeId',
    required: true,
    includeIfNull: false,
  )


  final String puntajeId;



      /// Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
  @JsonKey(
    
    name: r'puntaje',
    required: true,
    includeIfNull: false,
  )


  final String puntaje;



  @JsonKey(
    
    name: r'nivelDeConfianza',
    required: true,
    includeIfNull: false,
  )


  final SalidaPuntajeNivelDeConfianzaEnum nivelDeConfianza;



      /// Su suma es el total (R-REP-03). Es lo que hace que el numero se pueda apelar.
  @JsonKey(
    
    name: r'componentes',
    required: true,
    includeIfNull: false,
  )


  final List<Componente> componentes;



  @JsonKey(
    
    name: r'puntajeAnterior',
    required: false,
    includeIfNull: false,
  )


  final String? puntajeAnterior;



          // minimum: 0
  @JsonKey(
    
    name: r'eventosConsiderados',
    required: true,
    includeIfNull: false,
  )


  final int eventosConsiderados;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPuntaje &&
      other.puntajeId == puntajeId &&
      other.puntaje == puntaje &&
      other.nivelDeConfianza == nivelDeConfianza &&
      other.componentes == componentes &&
      other.puntajeAnterior == puntajeAnterior &&
      other.eventosConsiderados == eventosConsiderados;

    @override
    int get hashCode =>
        puntajeId.hashCode +
        puntaje.hashCode +
        nivelDeConfianza.hashCode +
        componentes.hashCode +
        (puntajeAnterior == null ? 0 : puntajeAnterior.hashCode) +
        eventosConsiderados.hashCode;

  factory SalidaPuntaje.fromJson(Map<String, dynamic> json) => _$SalidaPuntajeFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPuntajeToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPuntajeNivelDeConfianzaEnum {
@JsonValue(r'SIN_HISTORIAL')
SIN_HISTORIAL(r'SIN_HISTORIAL'),
@JsonValue(r'RESTRINGIDO')
RESTRINGIDO(r'RESTRINGIDO'),
@JsonValue(r'EN_OBSERVACION')
EN_OBSERVACION(r'EN_OBSERVACION'),
@JsonValue(r'BASICO')
BASICO(r'BASICO'),
@JsonValue(r'CONFIABLE')
CONFIABLE(r'CONFIABLE'),
@JsonValue(r'MUY_CONFIABLE')
MUY_CONFIABLE(r'MUY_CONFIABLE'),
@JsonValue(r'REFERENTE')
REFERENTE(r'REFERENTE');

const SalidaPuntajeNivelDeConfianzaEnum(this.value);

final String value;

@override
String toString() => value;
}


