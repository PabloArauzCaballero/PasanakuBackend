//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_segmento.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaSegmento {
  /// Returns a new [SalidaSegmento] instance.
  SalidaSegmento({

    required  this.segmentoId,

    required  this.codigo,

    required  this.prioridad,
  });

  @JsonKey(
    
    name: r'segmentoId',
    required: true,
    includeIfNull: false,
  )


  final String segmentoId;



  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



  @JsonKey(
    
    name: r'prioridad',
    required: true,
    includeIfNull: false,
  )


  final int prioridad;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaSegmento &&
      other.segmentoId == segmentoId &&
      other.codigo == codigo &&
      other.prioridad == prioridad;

    @override
    int get hashCode =>
        segmentoId.hashCode +
        codigo.hashCode +
        prioridad.hashCode;

  factory SalidaSegmento.fromJson(Map<String, dynamic> json) => _$SalidaSegmentoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaSegmentoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

