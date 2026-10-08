//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_auditoria/src/model/indicador.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_tablero.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaTablero {
  /// Returns a new [SalidaTablero] instance.
  SalidaTablero({

    required  this.periodo,

    required  this.provisorio,

    required  this.dimension,

     this.dimensionId,

    required  this.indicadores,
  });

  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



      /// Verdadero si algun indicador del periodo se calculo sin que los cierres cuadraran. Es del tablero entero y no de cada tarjeta: si uno solo es provisorio, la lectura de todos queda condicionada.
  @JsonKey(
    
    name: r'provisorio',
    required: true,
    includeIfNull: false,
  )


  final bool provisorio;



  @JsonKey(
    
    name: r'dimension',
    required: true,
    includeIfNull: false,
  )


  final SalidaTableroDimensionEnum dimension;



  @JsonKey(
    
    name: r'dimensionId',
    required: false,
    includeIfNull: false,
  )


  final String? dimensionId;



  @JsonKey(
    
    name: r'indicadores',
    required: true,
    includeIfNull: false,
  )


  final List<Indicador> indicadores;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaTablero &&
      other.periodo == periodo &&
      other.provisorio == provisorio &&
      other.dimension == dimension &&
      other.dimensionId == dimensionId &&
      other.indicadores == indicadores;

    @override
    int get hashCode =>
        periodo.hashCode +
        provisorio.hashCode +
        dimension.hashCode +
        dimensionId.hashCode +
        indicadores.hashCode;

  factory SalidaTablero.fromJson(Map<String, dynamic> json) => _$SalidaTableroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaTableroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaTableroDimensionEnum {
@JsonValue(r'GLOBAL')
GLOBAL(r'GLOBAL'),
@JsonValue(r'POR_GRUPO')
POR_GRUPO(r'POR_GRUPO'),
@JsonValue(r'POR_ORGANIZADOR')
POR_ORGANIZADOR(r'POR_ORGANIZADOR');

const SalidaTableroDimensionEnum(this.value);

final String value;

@override
String toString() => value;
}


