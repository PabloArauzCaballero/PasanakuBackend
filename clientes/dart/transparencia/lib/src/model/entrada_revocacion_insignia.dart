//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_revocacion_insignia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRevocacionInsignia {
  /// Returns a new [EntradaRevocacionInsignia] instance.
  EntradaRevocacionInsignia({

    required  this.motivoRevocacion,
  });

      /// Obligatorio. Revocar sin motivo deja a la persona sin saber por que.
  @JsonKey(
    
    name: r'motivoRevocacion',
    required: true,
    includeIfNull: false,
  )


  final String motivoRevocacion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRevocacionInsignia &&
      other.motivoRevocacion == motivoRevocacion;

    @override
    int get hashCode =>
        motivoRevocacion.hashCode;

  factory EntradaRevocacionInsignia.fromJson(Map<String, dynamic> json) => _$EntradaRevocacionInsigniaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRevocacionInsigniaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

