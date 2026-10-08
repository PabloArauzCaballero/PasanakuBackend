//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'factor_presentado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class FactorPresentado {
  /// Returns a new [FactorPresentado] instance.
  FactorPresentado({

    required  this.tipo,

    required  this.valor,
  });

  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final FactorPresentadoTipoEnum tipo;



  @JsonKey(
    
    name: r'valor',
    required: true,
    includeIfNull: false,
  )


  final String valor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is FactorPresentado &&
      other.tipo == tipo &&
      other.valor == valor;

    @override
    int get hashCode =>
        tipo.hashCode +
        valor.hashCode;

  factory FactorPresentado.fromJson(Map<String, dynamic> json) => _$FactorPresentadoFromJson(json);

  Map<String, dynamic> toJson() => _$FactorPresentadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum FactorPresentadoTipoEnum {
@JsonValue(r'OTP')
OTP(r'OTP'),
@JsonValue(r'BIOMETRIA')
BIOMETRIA(r'BIOMETRIA'),
@JsonValue(r'TOTP')
TOTP(r'TOTP');

const FactorPresentadoTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


