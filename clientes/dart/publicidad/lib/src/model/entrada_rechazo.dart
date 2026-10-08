//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_rechazo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRechazo {
  /// Returns a new [EntradaRechazo] instance.
  EntradaRechazo({

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRechazo &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        motivo.hashCode;

  factory EntradaRechazo.fromJson(Map<String, dynamic> json) => _$EntradaRechazoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRechazoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

