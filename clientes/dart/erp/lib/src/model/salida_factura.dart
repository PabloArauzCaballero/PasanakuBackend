//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_factura.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaFactura {
  /// Returns a new [SalidaFactura] instance.
  SalidaFactura({

    required  this.facturaProveedorId,

    required  this.estado,

    required  this.monto,

    required  this.montoPagado,

    required  this.saldoPendiente,

     this.asientoContableId,
  });

  @JsonKey(
    
    name: r'facturaProveedorId',
    required: true,
    includeIfNull: false,
  )


  final String facturaProveedorId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaFacturaEstadoEnum estado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final String monto;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'montoPagado',
    required: true,
    includeIfNull: false,
  )


  final String montoPagado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'saldoPendiente',
    required: true,
    includeIfNull: false,
  )


  final String saldoPendiente;



      /// Lo escribe nucleo-financiero (invariante 12). Viaja nulo hasta que responde el evento.
  @JsonKey(
    
    name: r'asientoContableId',
    required: false,
    includeIfNull: false,
  )


  final String? asientoContableId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaFactura &&
      other.facturaProveedorId == facturaProveedorId &&
      other.estado == estado &&
      other.monto == monto &&
      other.montoPagado == montoPagado &&
      other.saldoPendiente == saldoPendiente &&
      other.asientoContableId == asientoContableId;

    @override
    int get hashCode =>
        facturaProveedorId.hashCode +
        estado.hashCode +
        monto.hashCode +
        montoPagado.hashCode +
        saldoPendiente.hashCode +
        (asientoContableId == null ? 0 : asientoContableId.hashCode);

  factory SalidaFactura.fromJson(Map<String, dynamic> json) => _$SalidaFacturaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaFacturaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaFacturaEstadoEnum {
@JsonValue(r'REGISTRADA')
REGISTRADA(r'REGISTRADA'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'PAGADA_PARCIAL')
PAGADA_PARCIAL(r'PAGADA_PARCIAL'),
@JsonValue(r'PAGADA')
PAGADA(r'PAGADA'),
@JsonValue(r'ANULADA')
ANULADA(r'ANULADA');

const SalidaFacturaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


