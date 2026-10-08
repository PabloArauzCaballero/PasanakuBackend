//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_cobro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCobro {
  /// Returns a new [SalidaCobro] instance.
  SalidaCobro({

    required  this.pagoId,

    required  this.obligacionId,

    required  this.estadoObligacion,

    required  this.pendiente,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'pagoId',
    required: true,
    includeIfNull: false,
  )


  final String pagoId;



  @JsonKey(
    
    name: r'obligacionId',
    required: true,
    includeIfNull: false,
  )


  final String obligacionId;



  @JsonKey(
    
    name: r'estadoObligacion',
    required: true,
    includeIfNull: false,
  )


  final SalidaCobroEstadoObligacionEnum estadoObligacion;



  @JsonKey(
    
    name: r'pendiente',
    required: true,
    includeIfNull: false,
  )


  final Dinero pendiente;



      /// false cuando la respuesta viene de un reintento con la misma clave
  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCobro &&
      other.pagoId == pagoId &&
      other.obligacionId == obligacionId &&
      other.estadoObligacion == estadoObligacion &&
      other.pendiente == pendiente &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        pagoId.hashCode +
        obligacionId.hashCode +
        estadoObligacion.hashCode +
        pendiente.hashCode +
        esNuevo.hashCode;

  factory SalidaCobro.fromJson(Map<String, dynamic> json) => _$SalidaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCobroEstadoObligacionEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'PAGADO_PARCIAL')
PAGADO_PARCIAL(r'PAGADO_PARCIAL'),
@JsonValue(r'PAGADO')
PAGADO(r'PAGADO'),
@JsonValue(r'EN_MORA')
EN_MORA(r'EN_MORA'),
@JsonValue(r'VENCIDO')
VENCIDO(r'VENCIDO');

const SalidaCobroEstadoObligacionEnum(this.value);

final String value;

@override
String toString() => value;
}


