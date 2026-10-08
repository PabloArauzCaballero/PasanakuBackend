//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_compromiso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCompromiso {
  /// Returns a new [EntradaCompromiso] instance.
  EntradaCompromiso({

     this.entropias,
  });

      /// Lo que aportan los participantes al azar del sorteo. Cuantas mas manos distintas, menos posible es que una sola elija el resultado.
  @JsonKey(
    
    name: r'entropias',
    required: false,
    includeIfNull: false,
  )


  final List<String>? entropias;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCompromiso &&
      other.entropias == entropias;

    @override
    int get hashCode =>
        entropias.hashCode;

  factory EntradaCompromiso.fromJson(Map<String, dynamic> json) => _$EntradaCompromisoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCompromisoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

