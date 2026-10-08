//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_alcance_limites_sandbox.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAlcanceLimitesSandbox {
  /// Returns a new [SalidaAlcanceLimitesSandbox] instance.
  SalidaAlcanceLimitesSandbox({

    required  this.usuarios,

    required  this.montoOperacion,
  });

  @JsonKey(

    name: r'usuarios',
    required: true,
    includeIfNull: false,
  )


  final int usuarios;



  @JsonKey(

    name: r'montoOperacion',
    required: true,
    includeIfNull: false,
  )


  final String montoOperacion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAlcanceLimitesSandbox &&
      other.usuarios == usuarios &&
      other.montoOperacion == montoOperacion;

    @override
    int get hashCode =>
        usuarios.hashCode +
        montoOperacion.hashCode;

  factory SalidaAlcanceLimitesSandbox.fromJson(Map<String, dynamic> json) => _$SalidaAlcanceLimitesSandboxFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAlcanceLimitesSandboxToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

