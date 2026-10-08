//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_cumplimiento/src/model/salida_diligencia_limites_nuevos_inner.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_diligencia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDiligencia {
  /// Returns a new [SalidaDiligencia] instance.
  SalidaDiligencia({

    required  this.diligenciaId,

    required  this.estado,

    required  this.faltantes,

    required  this.limitesNuevos,
  });

  @JsonKey(

    name: r'diligenciaId',
    required: true,
    includeIfNull: false,
  )


  final String diligenciaId;



  @JsonKey(

    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaDiligenciaEstadoEnum estado;



  @JsonKey(

    name: r'faltantes',
    required: true,
    includeIfNull: false,
  )


  final List<String> faltantes;



  @JsonKey(

    name: r'limitesNuevos',
    required: true,
    includeIfNull: false,
  )


  final List<SalidaDiligenciaLimitesNuevosInner> limitesNuevos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDiligencia &&
      other.diligenciaId == diligenciaId &&
      other.estado == estado &&
      other.faltantes == faltantes &&
      other.limitesNuevos == limitesNuevos;

    @override
    int get hashCode =>
        diligenciaId.hashCode +
        estado.hashCode +
        faltantes.hashCode +
        limitesNuevos.hashCode;

  factory SalidaDiligencia.fromJson(Map<String, dynamic> json) => _$SalidaDiligenciaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDiligenciaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaDiligenciaEstadoEnum {
@JsonValue(r'EN_PROCESO')
EN_PROCESO(r'EN_PROCESO'),
@JsonValue(r'COMPLETA')
COMPLETA(r'COMPLETA'),
@JsonValue(r'OBSERVADA')
OBSERVADA(r'OBSERVADA');

const SalidaDiligenciaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


