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

    required  this.cargoId,

    required  this.estadoDelCargo,

    required  this.estadoDelDevengo,

     this.cuentaPorCobrarId,
  });

  @JsonKey(
    
    name: r'cargoId',
    required: true,
    includeIfNull: false,
  )


  final String cargoId;



  @JsonKey(
    
    name: r'estadoDelCargo',
    required: true,
    includeIfNull: false,
  )


  final SalidaCobroEstadoDelCargoEnum estadoDelCargo;



      /// DERIVADO de los cargos y devoluciones; el devengo es append-only
  @JsonKey(
    
    name: r'estadoDelDevengo',
    required: true,
    includeIfNull: false,
  )


  final String estadoDelDevengo;



  @JsonKey(
    
    name: r'cuentaPorCobrarId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaPorCobrarId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCobro &&
      other.cargoId == cargoId &&
      other.estadoDelCargo == estadoDelCargo &&
      other.estadoDelDevengo == estadoDelDevengo &&
      other.cuentaPorCobrarId == cuentaPorCobrarId;

    @override
    int get hashCode =>
        cargoId.hashCode +
        estadoDelCargo.hashCode +
        estadoDelDevengo.hashCode +
        (cuentaPorCobrarId == null ? 0 : cuentaPorCobrarId.hashCode);

  factory SalidaCobro.fromJson(Map<String, dynamic> json) => _$SalidaCobroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCobroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCobroEstadoDelCargoEnum {
@JsonValue(r'COBRADO')
COBRADO(r'COBRADO'),
@JsonValue(r'FALLIDO')
FALLIDO(r'FALLIDO');

const SalidaCobroEstadoDelCargoEnum(this.value);

final String value;

@override
String toString() => value;
}


