//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_estado_financiero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEstadoFinanciero {
  /// Returns a new [SalidaEstadoFinanciero] instance.
  SalidaEstadoFinanciero({

    required  this.estadoFinancieroId,

    required  this.tipo,

    required  this.cuadra,

    required  this.hashContenido,

    required  this.generadoEn,
  });

  @JsonKey(
    
    name: r'estadoFinancieroId',
    required: true,
    includeIfNull: false,
  )


  final String estadoFinancieroId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final SalidaEstadoFinancieroTipoEnum tipo;



  @JsonKey(
    
    name: r'cuadra',
    required: true,
    includeIfNull: false,
  )


  final bool cuadra;



  @JsonKey(
    
    name: r'hashContenido',
    required: true,
    includeIfNull: false,
  )


  final String hashContenido;



  @JsonKey(
    
    name: r'generadoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime generadoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEstadoFinanciero &&
      other.estadoFinancieroId == estadoFinancieroId &&
      other.tipo == tipo &&
      other.cuadra == cuadra &&
      other.hashContenido == hashContenido &&
      other.generadoEn == generadoEn;

    @override
    int get hashCode =>
        estadoFinancieroId.hashCode +
        tipo.hashCode +
        cuadra.hashCode +
        hashContenido.hashCode +
        generadoEn.hashCode;

  factory SalidaEstadoFinanciero.fromJson(Map<String, dynamic> json) => _$SalidaEstadoFinancieroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEstadoFinancieroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaEstadoFinancieroTipoEnum {
@JsonValue(r'BALANCE_GENERAL')
BALANCE_GENERAL(r'BALANCE_GENERAL'),
@JsonValue(r'ESTADO_RESULTADOS')
ESTADO_RESULTADOS(r'ESTADO_RESULTADOS');

const SalidaEstadoFinancieroTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


