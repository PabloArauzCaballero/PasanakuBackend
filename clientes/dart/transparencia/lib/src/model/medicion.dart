//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'medicion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Medicion {
  /// Returns a new [Medicion] instance.
  Medicion({

    required  this.codigo,

    required  this.valorCrudo,

    required  this.valorNormalizado,
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





    @override
    bool operator ==(Object other) => identical(this, other) || other is Medicion &&
      other.codigo == codigo &&
      other.valorCrudo == valorCrudo &&
      other.valorNormalizado == valorNormalizado;

    @override
    int get hashCode =>
        codigo.hashCode +
        valorCrudo.hashCode +
        valorNormalizado.hashCode;

  factory Medicion.fromJson(Map<String, dynamic> json) => _$MedicionFromJson(json);

  Map<String, dynamic> toJson() => _$MedicionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

