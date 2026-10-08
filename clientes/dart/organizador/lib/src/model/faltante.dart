//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'faltante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class Faltante {
  /// Returns a new [Faltante] instance.
  Faltante({

    required  this.codigo,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



      /// en texto; el postulante lo lee y tiene derecho a saber que le falta
  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is Faltante &&
      other.codigo == codigo &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        codigo.hashCode +
        motivo.hashCode;

  factory Faltante.fromJson(Map<String, dynamic> json) => _$FaltanteFromJson(json);

  Map<String, dynamic> toJson() => _$FaltanteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

