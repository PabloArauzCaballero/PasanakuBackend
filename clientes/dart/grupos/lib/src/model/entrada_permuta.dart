//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_permuta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPermuta {
  /// Returns a new [EntradaPermuta] instance.
  EntradaPermuta({

    required  this.turnoOrigenId,

    required  this.turnoDestinoId,

    required  this.contraparteId,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'turnoOrigenId',
    required: true,
    includeIfNull: false,
  )


  final String turnoOrigenId;



  @JsonKey(
    
    name: r'turnoDestinoId',
    required: true,
    includeIfNull: false,
  )


  final String turnoDestinoId;



  @JsonKey(
    
    name: r'contraparteId',
    required: true,
    includeIfNull: false,
  )


  final String contraparteId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPermuta &&
      other.turnoOrigenId == turnoOrigenId &&
      other.turnoDestinoId == turnoDestinoId &&
      other.contraparteId == contraparteId &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        turnoOrigenId.hashCode +
        turnoDestinoId.hashCode +
        contraparteId.hashCode +
        motivo.hashCode;

  factory EntradaPermuta.fromJson(Map<String, dynamic> json) => _$EntradaPermutaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPermutaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

