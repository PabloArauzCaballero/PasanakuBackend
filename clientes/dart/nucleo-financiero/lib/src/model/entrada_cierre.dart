//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cierre.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCierre {
  /// Returns a new [EntradaCierre] instance.
  EntradaCierre({

    required  this.cuentaBilleteraId,

    required  this.motivo,

    required  this.destinoSaldo,
  });

  @JsonKey(
    
    name: r'cuentaBilleteraId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaBilleteraId;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    
    name: r'destinoSaldo',
    required: true,
    includeIfNull: false,
  )


  final EntradaCierreDestinoSaldoEnum destinoSaldo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCierre &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.motivo == motivo &&
      other.destinoSaldo == destinoSaldo;

    @override
    int get hashCode =>
        cuentaBilleteraId.hashCode +
        motivo.hashCode +
        destinoSaldo.hashCode;

  factory EntradaCierre.fromJson(Map<String, dynamic> json) => _$EntradaCierreFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCierreToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCierreDestinoSaldoEnum {
@JsonValue(r'RETIRO')
RETIRO(r'RETIRO'),
@JsonValue(r'TRANSFERENCIA')
TRANSFERENCIA(r'TRANSFERENCIA');

const EntradaCierreDestinoSaldoEnum(this.value);

final String value;

@override
String toString() => value;
}


