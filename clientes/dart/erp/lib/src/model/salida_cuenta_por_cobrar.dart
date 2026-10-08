//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_cuenta_por_cobrar.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCuentaPorCobrar {
  /// Returns a new [SalidaCuentaPorCobrar] instance.
  SalidaCuentaPorCobrar({

    required  this.cuentaPorCobrarId,
  });

  @JsonKey(
    
    name: r'cuentaPorCobrarId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaPorCobrarId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCuentaPorCobrar &&
      other.cuentaPorCobrarId == cuentaPorCobrarId;

    @override
    int get hashCode =>
        cuentaPorCobrarId.hashCode;

  factory SalidaCuentaPorCobrar.fromJson(Map<String, dynamic> json) => _$SalidaCuentaPorCobrarFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCuentaPorCobrarToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

