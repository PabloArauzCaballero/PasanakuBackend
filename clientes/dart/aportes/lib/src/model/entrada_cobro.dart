//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
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

     this.recargo,

    required  this.canal,

    required  this.referenciaProveedor,

     this.proveedorId,

     this.esManual = false,
  });

  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;



  @JsonKey(
    
    name: r'recargo',
    required: false,
    includeIfNull: false,
  )


  final Dinero? recargo;



  @JsonKey(
    
    name: r'canal',
    required: true,
    includeIfNull: false,
  )


  final EntradaCobroCanalEnum canal;



  @JsonKey(
    
    name: r'referenciaProveedor',
    required: true,
    includeIfNull: false,
  )


  final String referenciaProveedor;



  @JsonKey(
    
    name: r'proveedorId',
    required: false,
    includeIfNull: false,
  )


  final String? proveedorId;



  @JsonKey(
    defaultValue: false,
    name: r'esManual',
    required: false,
    includeIfNull: false,
  )


  final bool? esManual;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaCobro &&
      other.monto == monto &&
      other.recargo == recargo &&
      other.canal == canal &&
      other.referenciaProveedor == referenciaProveedor &&
      other.proveedorId == proveedorId &&
      other.esManual == esManual;

    @override
    int get hashCode =>
        monto.hashCode +
        recargo.hashCode +
        canal.hashCode +
        referenciaProveedor.hashCode +
        proveedorId.hashCode +
        esManual.hashCode;

  factory EntradaCobro.fromJson(Map<String, dynamic> json) => _$EntradaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaCobroCanalEnum {
@JsonValue(r'BILLETERA_MOVIL')
BILLETERA_MOVIL(r'BILLETERA_MOVIL'),
@JsonValue(r'COMPENSACION_INTERNA')
COMPENSACION_INTERNA(r'COMPENSACION_INTERNA'),
@JsonValue(r'EFECTIVO_AL_ORGANIZADOR')
EFECTIVO_AL_ORGANIZADOR(r'EFECTIVO_AL_ORGANIZADOR'),
@JsonValue(r'FONDO_GARANTIA')
FONDO_GARANTIA(r'FONDO_GARANTIA'),
@JsonValue(r'QR_INTEROPERABLE')
QR_INTEROPERABLE(r'QR_INTEROPERABLE'),
@JsonValue(r'TARJETA')
TARJETA(r'TARJETA'),
@JsonValue(r'TRANSFERENCIA_BANCARIA')
TRANSFERENCIA_BANCARIA(r'TRANSFERENCIA_BANCARIA');

const EntradaCobroCanalEnum(this.value);

final String value;

@override
String toString() => value;
}


