//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_devengo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDevengo {
  /// Returns a new [SalidaDevengo] instance.
  SalidaDevengo({

    required  this.devengoId,

     this.cargoId,

    required  this.estado,

    required  this.montoTotal,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'devengoId',
    required: true,
    includeIfNull: false,
  )


  final String devengoId;



  @JsonKey(
    
    name: r'cargoId',
    required: false,
    includeIfNull: false,
  )


  final String? cargoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaDevengoEstadoEnum estado;



  @JsonKey(
    
    name: r'montoTotal',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoTotal;



  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDevengo &&
      other.devengoId == devengoId &&
      other.cargoId == cargoId &&
      other.estado == estado &&
      other.montoTotal == montoTotal &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        devengoId.hashCode +
        (cargoId == null ? 0 : cargoId.hashCode) +
        estado.hashCode +
        montoTotal.hashCode +
        esNuevo.hashCode;

  factory SalidaDevengo.fromJson(Map<String, dynamic> json) => _$SalidaDevengoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDevengoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaDevengoEstadoEnum {
@JsonValue(r'DEVENGADO')
DEVENGADO(r'DEVENGADO'),
@JsonValue(r'COBRADO')
COBRADO(r'COBRADO'),
@JsonValue(r'COBRADO_PARCIAL')
COBRADO_PARCIAL(r'COBRADO_PARCIAL'),
@JsonValue(r'EXONERADO')
EXONERADO(r'EXONERADO'),
@JsonValue(r'DEVUELTO')
DEVUELTO(r'DEVUELTO'),
@JsonValue(r'INCOBRABLE')
INCOBRABLE(r'INCOBRABLE');

const SalidaDevengoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


