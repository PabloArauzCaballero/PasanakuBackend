//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_consumo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaConsumo {
  /// Returns a new [SalidaConsumo] instance.
  SalidaConsumo({

    required  this.cuentaPublicitariaId,

    required  this.periodo,

    required  this.total,
  });

  @JsonKey(
    
    name: r'cuentaPublicitariaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaPublicitariaId;



  @JsonKey(
    
    name: r'periodo',
    required: true,
    includeIfNull: false,
  )


  final String periodo;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'total',
    required: true,
    includeIfNull: false,
  )


  final String total;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaConsumo &&
      other.cuentaPublicitariaId == cuentaPublicitariaId &&
      other.periodo == periodo &&
      other.total == total;

    @override
    int get hashCode =>
        cuentaPublicitariaId.hashCode +
        periodo.hashCode +
        total.hashCode;

  factory SalidaConsumo.fromJson(Map<String, dynamic> json) => _$SalidaConsumoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaConsumoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

