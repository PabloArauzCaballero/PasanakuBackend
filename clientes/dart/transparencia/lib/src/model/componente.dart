//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'componente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Componente {
  /// Returns a new [Componente] instance.
  Componente({

    required  this.codigo,

    required  this.valorCrudo,

    required  this.valorNormalizado,

    required  this.contribucion,

    required  this.tendencia,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'valorCrudo',
    required: true,
    includeIfNull: false,
  )


  final String valorCrudo;



  @JsonKey(
    
    name: r'valorNormalizado',
    required: true,
    includeIfNull: false,
  )


  final String valorNormalizado;



      /// Cadena decimal, nunca number. Un puntaje en coma flotante deja de sumar con sus componentes y R-REP-03 lo rechaza.
  @JsonKey(
    
    name: r'contribucion',
    required: true,
    includeIfNull: false,
  )


  final String contribucion;



  @JsonKey(
    
    name: r'tendencia',
    required: true,
    includeIfNull: false,
  )


  final ComponenteTendenciaEnum tendencia;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Componente &&
      other.codigo == codigo &&
      other.valorCrudo == valorCrudo &&
      other.valorNormalizado == valorNormalizado &&
      other.contribucion == contribucion &&
      other.tendencia == tendencia;

    @override
    int get hashCode =>
        codigo.hashCode +
        valorCrudo.hashCode +
        valorNormalizado.hashCode +
        contribucion.hashCode +
        tendencia.hashCode;

  factory Componente.fromJson(Map<String, dynamic> json) => _$ComponenteFromJson(json);

  Map<String, dynamic> toJson() => _$ComponenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ComponenteTendenciaEnum {
@JsonValue(r'SUBE')
SUBE(r'SUBE'),
@JsonValue(r'BAJA')
BAJA(r'BAJA'),
@JsonValue(r'ESTABLE')
ESTABLE(r'ESTABLE');

const ComponenteTendenciaEnum(this.value);

final String value;

@override
String toString() => value;
}


