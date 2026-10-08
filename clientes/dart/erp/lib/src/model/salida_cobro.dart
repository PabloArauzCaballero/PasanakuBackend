//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
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

    required  this.cobroId,

    required  this.cuentaPorCobrarId,

    required  this.estado,

    required  this.cobrado,

    required  this.saldoPendiente,
  });

  @JsonKey(
    
    name: r'cobroId',
    required: true,
    includeIfNull: false,
  )


  final String cobroId;



  @JsonKey(
    
    name: r'cuentaPorCobrarId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaPorCobrarId;



      /// Derivado de la suma de cobros; la columna es append-only
  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaCobroEstadoEnum estado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'cobrado',
    required: true,
    includeIfNull: false,
  )


  final String cobrado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'saldoPendiente',
    required: true,
    includeIfNull: false,
  )


  final String saldoPendiente;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCobro &&
      other.cobroId == cobroId &&
      other.cuentaPorCobrarId == cuentaPorCobrarId &&
      other.estado == estado &&
      other.cobrado == cobrado &&
      other.saldoPendiente == saldoPendiente;

    @override
    int get hashCode =>
        cobroId.hashCode +
        cuentaPorCobrarId.hashCode +
        estado.hashCode +
        cobrado.hashCode +
        saldoPendiente.hashCode;

  factory SalidaCobro.fromJson(Map<String, dynamic> json) => _$SalidaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Derivado de la suma de cobros; la columna es append-only
enum SalidaCobroEstadoEnum {
    /// Derivado de la suma de cobros; la columna es append-only
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
    /// Derivado de la suma de cobros; la columna es append-only
@JsonValue(r'COBRADA_PARCIAL')
COBRADA_PARCIAL(r'COBRADA_PARCIAL'),
    /// Derivado de la suma de cobros; la columna es append-only
@JsonValue(r'COBRADA')
COBRADA(r'COBRADA'),
    /// Derivado de la suma de cobros; la columna es append-only
@JsonValue(r'INCOBRABLE')
INCOBRABLE(r'INCOBRABLE');

const SalidaCobroEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


