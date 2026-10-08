//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_erp/src/model/entrada_presupuesto_partidas_inner.dart';
import 'package:json_annotation/json_annotation.dart';

part 'entrada_presupuesto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaPresupuesto {
  /// Returns a new [EntradaPresupuesto] instance.
  EntradaPresupuesto({

    required  this.centroCostoId,

    required  this.ejercicioFiscalId,

    required  this.nombre,

    required  this.moneda,

    required  this.partidas,
  });

  @JsonKey(
    
    name: r'centroCostoId',
    required: true,
    includeIfNull: false,
  )


  final String centroCostoId;



  @JsonKey(
    
    name: r'ejercicioFiscalId',
    required: true,
    includeIfNull: false,
  )


  final String ejercicioFiscalId;



  @JsonKey(
    
    name: r'nombre',
    required: true,
    includeIfNull: false,
  )


  final String nombre;



  @JsonKey(
    
    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final EntradaPresupuestoMonedaEnum moneda;



  @JsonKey(
    
    name: r'partidas',
    required: true,
    includeIfNull: false,
  )


  final List<EntradaPresupuestoPartidasInner> partidas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaPresupuesto &&
      other.centroCostoId == centroCostoId &&
      other.ejercicioFiscalId == ejercicioFiscalId &&
      other.nombre == nombre &&
      other.moneda == moneda &&
      other.partidas == partidas;

    @override
    int get hashCode =>
        centroCostoId.hashCode +
        ejercicioFiscalId.hashCode +
        nombre.hashCode +
        moneda.hashCode +
        partidas.hashCode;

  factory EntradaPresupuesto.fromJson(Map<String, dynamic> json) => _$EntradaPresupuestoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaPresupuestoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaPresupuestoMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const EntradaPresupuestoMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


