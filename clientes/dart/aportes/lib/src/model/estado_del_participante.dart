//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'estado_del_participante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EstadoDelParticipante {
  /// Returns a new [EstadoDelParticipante] instance.
  EstadoDelParticipante({

    required  this.alDia,

    required  this.totalAportado,

    required  this.deudaVigente,

    required  this.porAportar,

    required  this.obligacionesAbiertas,

    required  this.moneda,
  });

  @JsonKey(
    
    name: r'alDia',
    required: true,
    includeIfNull: false,
  )


  final bool alDia;



  @JsonKey(
    
    name: r'totalAportado',
    required: true,
    includeIfNull: false,
  )


  final String totalAportado;



  @JsonKey(
    
    name: r'deudaVigente',
    required: true,
    includeIfNull: false,
  )


  final String deudaVigente;



  @JsonKey(
    
    name: r'porAportar',
    required: true,
    includeIfNull: false,
  )


  final String porAportar;



  @JsonKey(
    
    name: r'obligacionesAbiertas',
    required: true,
    includeIfNull: false,
  )


  final int obligacionesAbiertas;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EstadoDelParticipanteMonedaEnum moneda;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EstadoDelParticipante &&
      other.alDia == alDia &&
      other.totalAportado == totalAportado &&
      other.deudaVigente == deudaVigente &&
      other.porAportar == porAportar &&
      other.obligacionesAbiertas == obligacionesAbiertas &&
      other.moneda == moneda;

    @override
    int get hashCode =>
        alDia.hashCode +
        totalAportado.hashCode +
        deudaVigente.hashCode +
        porAportar.hashCode +
        obligacionesAbiertas.hashCode +
        moneda.hashCode;

  factory EstadoDelParticipante.fromJson(Map<String, dynamic> json) => _$EstadoDelParticipanteFromJson(json);

  Map<String, dynamic> toJson() => _$EstadoDelParticipanteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EstadoDelParticipanteMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EstadoDelParticipanteMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


