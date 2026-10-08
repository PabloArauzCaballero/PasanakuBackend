//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_entrega.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaEntrega {
  /// Returns a new [EntradaEntrega] instance.
  EntradaEntrega({

     this.usuarioId,
  });

      /// Nulo si quien mira no tiene sesion. La entrega se registra igual.
  @JsonKey(
    
    name: r'usuarioId',
    required: false,
    includeIfNull: false,
  )


  final String? usuarioId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaEntrega &&
      other.usuarioId == usuarioId;

    @override
    int get hashCode =>
        (usuarioId == null ? 0 : usuarioId.hashCode);

  factory EntradaEntrega.fromJson(Map<String, dynamic> json) => _$EntradaEntregaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaEntregaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

