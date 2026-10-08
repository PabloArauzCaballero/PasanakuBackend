//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_acuerdo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaAcuerdo {
  /// Returns a new [EntradaAcuerdo] instance.
  EntradaAcuerdo({

    required  this.grupoId,

    required  this.tipo,

    required  this.descripcion,

     this.referenciaAfectadaId,

     this.diasVotacion = 5,
  });

  @JsonKey(
    
    name: r'grupoId',
    required: true,
    includeIfNull: false,
  )


  final String grupoId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaAcuerdoTipoEnum tipo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    
    name: r'referenciaAfectadaId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaAfectadaId;



          // minimum: 1
          // maximum: 15
  @JsonKey(
    defaultValue: 5,
    name: r'diasVotacion',
    required: false,
    includeIfNull: false,
  )


  final int? diasVotacion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaAcuerdo &&
      other.grupoId == grupoId &&
      other.tipo == tipo &&
      other.descripcion == descripcion &&
      other.referenciaAfectadaId == referenciaAfectadaId &&
      other.diasVotacion == diasVotacion;

    @override
    int get hashCode =>
        grupoId.hashCode +
        tipo.hashCode +
        descripcion.hashCode +
        (referenciaAfectadaId == null ? 0 : referenciaAfectadaId.hashCode) +
        diasVotacion.hashCode;

  factory EntradaAcuerdo.fromJson(Map<String, dynamic> json) => _$EntradaAcuerdoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaAcuerdoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaAcuerdoTipoEnum {
@JsonValue(r'CONDONACION')
CONDONACION(r'CONDONACION'),
@JsonValue(r'EXPULSION')
EXPULSION(r'EXPULSION'),
@JsonValue(r'PERMUTA')
PERMUTA(r'PERMUTA'),
@JsonValue(r'TRASPASO_CUPO')
TRASPASO_CUPO(r'TRASPASO_CUPO'),
@JsonValue(r'CAMBIO_REGLAMENTO')
CAMBIO_REGLAMENTO(r'CAMBIO_REGLAMENTO'),
@JsonValue(r'DISOLUCION')
DISOLUCION(r'DISOLUCION'),
@JsonValue(r'REPETIR_SORTEO')
REPETIR_SORTEO(r'REPETIR_SORTEO');

const EntradaAcuerdoTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


