//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_orden.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaOrden {
  /// Returns a new [SalidaOrden] instance.
  SalidaOrden({

    required  this.ordenCompraId,

    required  this.estado,

    required  this.monto,
  });

  @JsonKey(
    
    name: r'ordenCompraId',
    required: true,
    includeIfNull: false,
  )


  final String ordenCompraId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaOrdenEstadoEnum estado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaOrden &&
      other.ordenCompraId == ordenCompraId &&
      other.estado == estado &&
      other.monto == monto;

    @override
    int get hashCode =>
        ordenCompraId.hashCode +
        estado.hashCode +
        monto.hashCode;

  factory SalidaOrden.fromJson(Map<String, dynamic> json) => _$SalidaOrdenFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaOrdenToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaOrdenEstadoEnum {
@JsonValue(r'BORRADOR')
BORRADOR(r'BORRADOR'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECIBIDA')
RECIBIDA(r'RECIBIDA'),
@JsonValue(r'CANCELADA')
CANCELADA(r'CANCELADA');

const SalidaOrdenEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


