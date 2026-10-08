//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'metrica_de_riesgo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class MetricaDeRiesgo {
  /// Returns a new [MetricaDeRiesgo] instance.
  MetricaDeRiesgo({

    required  this.codigo,

    required  this.valor,

    required  this.unidad,

     this.umbral,

    required  this.mayorEsPeor,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'valor',
    required: true,
    includeIfNull: false,
  )


  final String valor;



  @JsonKey(
    
    name: r'unidad',
    required: true,
    includeIfNull: false,
  )


  final String unidad;



  @JsonKey(
    
    name: r'umbral',
    required: false,
    includeIfNull: false,
  )


  final String? umbral;



      /// Sin este dato una tasa de pago del 100% se leeria como alerta. Una metrica sin umbral configurado no dispara nada.
  @JsonKey(
    
    name: r'mayorEsPeor',
    required: true,
    includeIfNull: false,
  )


  final bool mayorEsPeor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is MetricaDeRiesgo &&
      other.codigo == codigo &&
      other.valor == valor &&
      other.unidad == unidad &&
      other.umbral == umbral &&
      other.mayorEsPeor == mayorEsPeor;

    @override
    int get hashCode =>
        codigo.hashCode +
        valor.hashCode +
        unidad.hashCode +
        (umbral == null ? 0 : umbral.hashCode) +
        mayorEsPeor.hashCode;

  factory MetricaDeRiesgo.fromJson(Map<String, dynamic> json) => _$MetricaDeRiesgoFromJson(json);

  Map<String, dynamic> toJson() => _$MetricaDeRiesgoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

