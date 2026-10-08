//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_segmento.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaSegmento {
  /// Returns a new [EntradaSegmento] instance.
  EntradaSegmento({

    required  this.codigo,

    required  this.descripcion,

    required  this.criterio,

    required  this.prioridad,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



      /// hechos exigidos y su minimo. Evaluable, no texto libre. No puede referenciar datos sensibles ni categorias protegidas
  @JsonKey(
    
    name: r'criterio',
    required: true,
    includeIfNull: false,
  )


  final Map<String, int> criterio;



      /// unica entre los activos; resuelve el empate al definir, no al cobrar
          // minimum: 1
          // maximum: 99
  @JsonKey(
    
    name: r'prioridad',
    required: true,
    includeIfNull: false,
  )


  final int prioridad;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaSegmento &&
      other.codigo == codigo &&
      other.descripcion == descripcion &&
      other.criterio == criterio &&
      other.prioridad == prioridad;

    @override
    int get hashCode =>
        codigo.hashCode +
        descripcion.hashCode +
        criterio.hashCode +
        prioridad.hashCode;

  factory EntradaSegmento.fromJson(Map<String, dynamic> json) => _$EntradaSegmentoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaSegmentoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

