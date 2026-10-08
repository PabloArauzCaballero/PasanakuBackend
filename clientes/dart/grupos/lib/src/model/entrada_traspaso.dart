//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_traspaso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaTraspaso {
  /// Returns a new [EntradaTraspaso] instance.
  EntradaTraspaso({

    required  this.usuarioEntranteId,

    required  this.motivo,

     this.acuerdoId,
  });

  @JsonKey(
    
    name: r'usuarioEntranteId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioEntranteId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    
    name: r'acuerdoId',
    required: false,
    includeIfNull: false,
  )


  final String? acuerdoId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaTraspaso &&
      other.usuarioEntranteId == usuarioEntranteId &&
      other.motivo == motivo &&
      other.acuerdoId == acuerdoId;

    @override
    int get hashCode =>
        usuarioEntranteId.hashCode +
        motivo.hashCode +
        (acuerdoId == null ? 0 : acuerdoId.hashCode);

  factory EntradaTraspaso.fromJson(Map<String, dynamic> json) => _$EntradaTraspasoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaTraspasoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

