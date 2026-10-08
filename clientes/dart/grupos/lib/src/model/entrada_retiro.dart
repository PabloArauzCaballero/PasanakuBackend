//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_retiro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRetiro {
  /// Returns a new [EntradaRetiro] instance.
  EntradaRetiro({

    required  this.participanteId,

    required  this.motivo,

     this.aceptaPlanDePago = false,
  });

  @JsonKey(
    
    name: r'participanteId',
    required: true,
    includeIfNull: false,
  )


  final String participanteId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    defaultValue: false,
    name: r'aceptaPlanDePago',
    required: false,
    includeIfNull: false,
  )


  final bool? aceptaPlanDePago;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRetiro &&
      other.participanteId == participanteId &&
      other.motivo == motivo &&
      other.aceptaPlanDePago == aceptaPlanDePago;

    @override
    int get hashCode =>
        participanteId.hashCode +
        motivo.hashCode +
        aceptaPlanDePago.hashCode;

  factory EntradaRetiro.fromJson(Map<String, dynamic> json) => _$EntradaRetiroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRetiroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

