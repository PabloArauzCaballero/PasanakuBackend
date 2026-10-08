//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_presupuesto_partidas_inner.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPresupuestoPartidasInner {
  /// Returns a new [EntradaPresupuestoPartidasInner] instance.
  EntradaPresupuestoPartidasInner({

    required  this.cuentaContableId,

    required  this.periodoContableId,

    required  this.montoPresupuestado,
  });

  @JsonKey(
    
    name: r'cuentaContableId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaContableId;



  @JsonKey(
    
    name: r'periodoContableId',
    required: true,
    includeIfNull: false,
  )


  final String periodoContableId;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'montoPresupuestado',
    required: true,
    includeIfNull: false,
  )


  final String montoPresupuestado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPresupuestoPartidasInner &&
      other.cuentaContableId == cuentaContableId &&
      other.periodoContableId == periodoContableId &&
      other.montoPresupuestado == montoPresupuestado;

    @override
    int get hashCode =>
        cuentaContableId.hashCode +
        periodoContableId.hashCode +
        montoPresupuestado.hashCode;

  factory EntradaPresupuestoPartidasInner.fromJson(Map<String, dynamic> json) => _$EntradaPresupuestoPartidasInnerFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPresupuestoPartidasInnerToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

