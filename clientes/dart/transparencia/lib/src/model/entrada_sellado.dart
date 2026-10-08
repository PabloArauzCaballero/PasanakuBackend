//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/hecho_a_sellar.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_sellado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaSellado {
  /// Returns a new [EntradaSellado] instance.
  EntradaSellado({

    required  this.grupoId,

    required  this.motivo,

    required  this.hechos,

    required  this.desde,

    required  this.hasta,

    required  this.excepcionesDeConciliacionAbiertas,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final EntradaSelladoMotivoEnum motivo;



  @JsonKey(
    
    name: r'hechos',
    required: true,
    includeIfNull: false,
  )


  final List<HechoASellar> hechos;



  @JsonKey(
    
    name: r'desde',
    required: true,
    includeIfNull: false,
  )


  final DateTime desde;



  @JsonKey(
    
    name: r'hasta',
    required: true,
    includeIfNull: false,
  )


  final DateTime hasta;



      /// Lo resuelve nucleo-financiero y llega resuelto: no se lee su esquema (invariante 11) ni se lo consulta dentro de la transaccion (invariante 6).
          // minimum: 0
  @JsonKey(
    
    name: r'excepcionesDeConciliacionAbiertas',
    required: true,
    includeIfNull: false,
  )


  final int excepcionesDeConciliacionAbiertas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaSellado &&
      other.grupoId == grupoId &&
      other.motivo == motivo &&
      other.hechos == hechos &&
      other.desde == desde &&
      other.hasta == hasta &&
      other.excepcionesDeConciliacionAbiertas == excepcionesDeConciliacionAbiertas;

    @override
    int get hashCode =>
        grupoId.hashCode +
        motivo.hashCode +
        hechos.hashCode +
        desde.hashCode +
        hasta.hashCode +
        excepcionesDeConciliacionAbiertas.hashCode;

  factory EntradaSellado.fromJson(Map<String, dynamic> json) => _$EntradaSelladoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaSelladoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaSelladoMotivoEnum {
@JsonValue(r'CIERRE_PERIODO')
CIERRE_PERIODO(r'CIERRE_PERIODO'),
@JsonValue(r'ENTREGA')
ENTREGA(r'ENTREGA'),
@JsonValue(r'HITO')
HITO(r'HITO'),
@JsonValue(r'DISOLUCION')
DISOLUCION(r'DISOLUCION');

const EntradaSelladoMotivoEnum(this.value);

final String value;

@override
String toString() => value;
}


