//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_cobro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaCobro {
  /// Returns a new [EntradaCobro] instance.
  EntradaCobro({

    required  this.monto,

    required  this.formaCobro,
  });

      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;



  @JsonKey(
    
    name: r'formaCobro',
    required: true,
    includeIfNull: false,
  )


  final EntradaCobroFormaCobroEnum formaCobro;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCobro &&
      other.monto == monto &&
      other.formaCobro == formaCobro;

    @override
    int get hashCode =>
        monto.hashCode +
        formaCobro.hashCode;

  factory EntradaCobro.fromJson(Map<String, dynamic> json) => _$EntradaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCobroFormaCobroEnum {
@JsonValue(r'TRANSFERENCIA')
TRANSFERENCIA(r'TRANSFERENCIA'),
@JsonValue(r'QR')
QR(r'QR'),
@JsonValue(r'TARJETA')
TARJETA(r'TARJETA'),
@JsonValue(r'EFECTIVO')
EFECTIVO(r'EFECTIVO');

const EntradaCobroFormaCobroEnum(this.value);

final String value;

@override
String toString() => value;
}


