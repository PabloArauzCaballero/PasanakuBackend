//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'restriccion_vigente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class RestriccionVigente {
  /// Returns a new [RestriccionVigente] instance.
  RestriccionVigente({

    required  this.vigente,

     this.nivel,

    required  this.montoQueLaLevanta,
  });

  @JsonKey(
    
    name: r'vigente',
    required: true,
    includeIfNull: false,
  )


  final bool vigente;



  @JsonKey(
    
    name: r'nivel',
    required: false,
    includeIfNull: false,
  )


  final String? nivel;



  @JsonKey(
    
    name: r'montoQueLaLevanta',
    required: true,
    includeIfNull: false,
  )


  final String montoQueLaLevanta;





    @override
    bool operator ==(Object other) => identical(this, other) || other is RestriccionVigente &&
      other.vigente == vigente &&
      other.nivel == nivel &&
      other.montoQueLaLevanta == montoQueLaLevanta;

    @override
    int get hashCode =>
        vigente.hashCode +
        (nivel == null ? 0 : nivel.hashCode) +
        montoQueLaLevanta.hashCode;

  factory RestriccionVigente.fromJson(Map<String, dynamic> json) => _$RestriccionVigenteFromJson(json);

  Map<String, dynamic> toJson() => _$RestriccionVigenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

