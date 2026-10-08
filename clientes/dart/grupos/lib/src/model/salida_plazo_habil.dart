//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_grupos/src/model/dia_salteado.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_plazo_habil.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPlazoHabil {
  /// Returns a new [SalidaPlazoHabil] instance.
  SalidaPlazoHabil({

    required  this.fechaLimite,

    required  this.diasSalteados,
  });

  @JsonKey(
    
    name: r'fechaLimite',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaLimite;



  @JsonKey(
    
    name: r'diasSalteados',
    required: true,
    includeIfNull: false,
  )


  final List<DiaSalteado> diasSalteados;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPlazoHabil &&
      other.fechaLimite == fechaLimite &&
      other.diasSalteados == diasSalteados;

    @override
    int get hashCode =>
        fechaLimite.hashCode +
        diasSalteados.hashCode;

  factory SalidaPlazoHabil.fromJson(Map<String, dynamic> json) => _$SalidaPlazoHabilFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPlazoHabilToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

