//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_entregas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_ejecucion_entrega.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEjecucionEntrega {
  /// Returns a new [SalidaEjecucionEntrega] instance.
  SalidaEjecucionEntrega({

    required  this.entregaId,

    required  this.estado,

    required  this.montoEntregado,
  });

  @JsonKey(
    
    name: r'entregaId',
    required: true,
    includeIfNull: false,
  )


  final String entregaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaEjecucionEntregaEstadoEnum estado;



  @JsonKey(
    
    name: r'montoEntregado',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoEntregado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEjecucionEntrega &&
      other.entregaId == entregaId &&
      other.estado == estado &&
      other.montoEntregado == montoEntregado;

    @override
    int get hashCode =>
        entregaId.hashCode +
        estado.hashCode +
        montoEntregado.hashCode;

  factory SalidaEjecucionEntrega.fromJson(Map<String, dynamic> json) => _$SalidaEjecucionEntregaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEjecucionEntregaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaEjecucionEntregaEstadoEnum {
@JsonValue(r'ENTREGADA')
ENTREGADA(r'ENTREGADA');

const SalidaEjecucionEntregaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


