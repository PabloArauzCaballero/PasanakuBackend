//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_garantia/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_cobertura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCobertura {
  /// Returns a new [SalidaCobertura] instance.
  SalidaCobertura({

     this.coberturaId,

     this.deudaId,

    required  this.montoCubierto,

    required  this.limiteQueMando,

    required  this.requiereAprobacion,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'coberturaId',
    required: false,
    includeIfNull: false,
  )


  final String? coberturaId;



      /// cubrir NO perdona — queda la deuda contra quien incumplio
  @JsonKey(
    
    name: r'deudaId',
    required: false,
    includeIfNull: false,
  )


  final String? deudaId;



  @JsonKey(
    
    name: r'montoCubierto',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoCubierto;



  @JsonKey(
    
    name: r'limiteQueMando',
    required: true,
    includeIfNull: false,
  )


  final SalidaCoberturaLimiteQueMandoEnum limiteQueMando;



  @JsonKey(
    
    name: r'requiereAprobacion',
    required: true,
    includeIfNull: false,
  )


  final bool requiereAprobacion;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCobertura &&
      other.coberturaId == coberturaId &&
      other.deudaId == deudaId &&
      other.montoCubierto == montoCubierto &&
      other.limiteQueMando == limiteQueMando &&
      other.requiereAprobacion == requiereAprobacion &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        (coberturaId == null ? 0 : coberturaId.hashCode) +
        (deudaId == null ? 0 : deudaId.hashCode) +
        montoCubierto.hashCode +
        limiteQueMando.hashCode +
        requiereAprobacion.hashCode +
        esNueva.hashCode;

  factory SalidaCobertura.fromJson(Map<String, dynamic> json) => _$SalidaCoberturaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCoberturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCoberturaLimiteQueMandoEnum {
@JsonValue(r'PORCENTAJE_DEL_APORTE')
PORCENTAJE_DEL_APORTE(r'PORCENTAJE_DEL_APORTE'),
@JsonValue(r'TOPE_POR_PARTICIPANTE')
TOPE_POR_PARTICIPANTE(r'TOPE_POR_PARTICIPANTE'),
@JsonValue(r'TOPE_POR_PERIODO')
TOPE_POR_PERIODO(r'TOPE_POR_PERIODO'),
@JsonValue(r'SALDO_DEL_FONDO')
SALDO_DEL_FONDO(r'SALDO_DEL_FONDO'),
@JsonValue(r'DIAS_DE_MORA_INSUFICIENTES')
DIAS_DE_MORA_INSUFICIENTES(r'DIAS_DE_MORA_INSUFICIENTES'),
@JsonValue(r'MAXIMO_DE_COBERTURAS')
MAXIMO_DE_COBERTURAS(r'MAXIMO_DE_COBERTURAS');

const SalidaCoberturaLimiteQueMandoEnum(this.value);

final String value;

@override
String toString() => value;
}


