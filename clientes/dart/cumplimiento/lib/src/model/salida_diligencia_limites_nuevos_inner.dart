//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_diligencia_limites_nuevos_inner.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDiligenciaLimitesNuevosInner {
  /// Returns a new [SalidaDiligenciaLimitesNuevosInner] instance.
  SalidaDiligenciaLimitesNuevosInner({

    required  this.concepto,

     this.monto,

    required  this.ventana,
  });

  @JsonKey(

    name: r'concepto',
    required: true,
    includeIfNull: false,
  )


  final String concepto;



  @JsonKey(

    name: r'monto',
    required: false,
    includeIfNull: false,
  )


  final String? monto;



  @JsonKey(

    name: r'ventana',
    required: true,
    includeIfNull: false,
  )


  final String ventana;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDiligenciaLimitesNuevosInner &&
      other.concepto == concepto &&
      other.monto == monto &&
      other.ventana == ventana;

    @override
    int get hashCode =>
        concepto.hashCode +
        monto.hashCode +
        ventana.hashCode;

  factory SalidaDiligenciaLimitesNuevosInner.fromJson(Map<String, dynamic> json) => _$SalidaDiligenciaLimitesNuevosInnerFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDiligenciaLimitesNuevosInnerToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

