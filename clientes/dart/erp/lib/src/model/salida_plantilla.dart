//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_plantilla.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPlantilla {
  /// Returns a new [SalidaPlantilla] instance.
  SalidaPlantilla({

    required  this.plantillaId,

    required  this.lineas,

    required  this.debes,

    required  this.haberes,
  });

  @JsonKey(
    
    name: r'plantillaId',
    required: true,
    includeIfNull: false,
  )


  final String plantillaId;



  @JsonKey(
    
    name: r'lineas',
    required: true,
    includeIfNull: false,
  )


  final int lineas;



  @JsonKey(
    
    name: r'debes',
    required: true,
    includeIfNull: false,
  )


  final int debes;



  @JsonKey(
    
    name: r'haberes',
    required: true,
    includeIfNull: false,
  )


  final int haberes;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPlantilla &&
      other.plantillaId == plantillaId &&
      other.lineas == lineas &&
      other.debes == debes &&
      other.haberes == haberes;

    @override
    int get hashCode =>
        plantillaId.hashCode +
        lineas.hashCode +
        debes.hashCode +
        haberes.hashCode;

  factory SalidaPlantilla.fromJson(Map<String, dynamic> json) => _$SalidaPlantillaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPlantillaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

