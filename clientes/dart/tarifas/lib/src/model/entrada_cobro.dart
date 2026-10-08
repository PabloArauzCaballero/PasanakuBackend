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

    required  this.formaCobro,

    required  this.exitoso,

     this.error,
  });

  @JsonKey(
    
    name: r'formaCobro',
    required: true,
    includeIfNull: false,
  )


  final EntradaCobroFormaCobroEnum formaCobro;



  @JsonKey(
    
    name: r'exitoso',
    required: true,
    includeIfNull: false,
  )


  final bool exitoso;



  @JsonKey(
    
    name: r'error',
    required: false,
    includeIfNull: false,
  )


  final String? error;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCobro &&
      other.formaCobro == formaCobro &&
      other.exitoso == exitoso &&
      other.error == error;

    @override
    int get hashCode =>
        formaCobro.hashCode +
        exitoso.hashCode +
        (error == null ? 0 : error.hashCode);

  factory EntradaCobro.fromJson(Map<String, dynamic> json) => _$EntradaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCobroFormaCobroEnum {
@JsonValue(r'COBRO_EXTERNO')
COBRO_EXTERNO(r'COBRO_EXTERNO'),
@JsonValue(r'COMPENSACION')
COMPENSACION(r'COMPENSACION'),
@JsonValue(r'DEBITO_DE_BILLETERA')
DEBITO_DE_BILLETERA(r'DEBITO_DE_BILLETERA'),
@JsonValue(r'DEDUCCION_DE_ENTREGA')
DEDUCCION_DE_ENTREGA(r'DEDUCCION_DE_ENTREGA'),
@JsonValue(r'OBLIGACION_DE_APORTE')
OBLIGACION_DE_APORTE(r'OBLIGACION_DE_APORTE');

const EntradaCobroFormaCobroEnum(this.value);

final String value;

@override
String toString() => value;
}


