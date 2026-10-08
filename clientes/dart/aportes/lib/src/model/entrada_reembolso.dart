//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_reembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaReembolso {
  /// Returns a new [EntradaReembolso] instance.
  EntradaReembolso({

    required  this.monto,

    required  this.motivo,
  });

  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final EntradaReembolsoMotivoEnum motivo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaReembolso &&
      other.monto == monto &&
      other.motivo == motivo;

    @override
    int get hashCode =>
        monto.hashCode +
        motivo.hashCode;

  factory EntradaReembolso.fromJson(Map<String, dynamic> json) => _$EntradaReembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaReembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaReembolsoMotivoEnum {
@JsonValue(r'DISPUTA')
DISPUTA(r'DISPUTA'),
@JsonValue(r'DUPLICADO')
DUPLICADO(r'DUPLICADO'),
@JsonValue(r'ERROR_MONTO')
ERROR_MONTO(r'ERROR_MONTO'),
@JsonValue(r'GRUPO_CANCELADO')
GRUPO_CANCELADO(r'GRUPO_CANCELADO');

const EntradaReembolsoMotivoEnum(this.value);

final String value;

@override
String toString() => value;
}


