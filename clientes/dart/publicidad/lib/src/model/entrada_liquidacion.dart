//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_liquidacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaLiquidacion {
  /// Returns a new [EntradaLiquidacion] instance.
  EntradaLiquidacion({

    required  this.periodo,

     this.facturaElectronicaId,

     this.cuentaPorCobrarId,
  });

  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



      /// El comprobante fiscal, ya emitido por modulo 11. Sin el, la factura nace GENERADA y no puede avanzar: la tabla es append-only.
  @JsonKey(
    
    name: r'facturaElectronicaId',
    required: false,
    includeIfNull: false,
  )


  final String? facturaElectronicaId;



      /// La cuenta por cobrar, ya abierta por modulo 13 (CU-104).
  @JsonKey(
    
    name: r'cuentaPorCobrarId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaPorCobrarId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaLiquidacion &&
      other.periodo == periodo &&
      other.facturaElectronicaId == facturaElectronicaId &&
      other.cuentaPorCobrarId == cuentaPorCobrarId;

    @override
    int get hashCode =>
        periodo.hashCode +
        (facturaElectronicaId == null ? 0 : facturaElectronicaId.hashCode) +
        (cuentaPorCobrarId == null ? 0 : cuentaPorCobrarId.hashCode);

  factory EntradaLiquidacion.fromJson(Map<String, dynamic> json) => _$EntradaLiquidacionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaLiquidacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

