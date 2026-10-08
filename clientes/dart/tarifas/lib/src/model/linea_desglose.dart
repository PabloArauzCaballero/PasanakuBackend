//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_tarifas/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'linea_desglose.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class LineaDesglose {
  /// Returns a new [LineaDesglose] instance.
  LineaDesglose({

    required  this.concepto,

    required  this.detalle,

    required  this.monto,
  });

  @JsonKey(
    
    name: r'concepto',
    required: true,
    includeIfNull: false,
  )


  final String concepto;



      /// en lenguaje llano; es lo que el usuario lee antes de aceptar
  @JsonKey(
    
    name: r'detalle',
    required: true,
    includeIfNull: false,
  )


  final String detalle;



  @JsonKey(
    
    name: r'monto',
    required: true,
    includeIfNull: false,
  )


  final Dinero monto;





    @override
    bool operator ==(Object other) => identical(this, other) || other is LineaDesglose &&
      other.concepto == concepto &&
      other.detalle == detalle &&
      other.monto == monto;

    @override
    int get hashCode =>
        concepto.hashCode +
        detalle.hashCode +
        monto.hashCode;

  factory LineaDesglose.fromJson(Map<String, dynamic> json) => _$LineaDesgloseFromJson(json);

  Map<String, dynamic> toJson() => _$LineaDesgloseToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

