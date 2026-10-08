//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_riesgo_operativo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRiesgoOperativo {
  /// Returns a new [SalidaRiesgoOperativo] instance.
  SalidaRiesgoOperativo({

    required  this.eventoId,

    required  this.codigo,

    required  this.perdidaNeta,

    required  this.moneda,

     this.planAccionId,
  });

  @JsonKey(

    name: r'eventoId',
    required: true,
    includeIfNull: false,
  )


  final String eventoId;



      /// Derivado del hecho —fecha de deteccion y categoria— para que sea legible en un listado y para que dos cargas del mismo hecho choquen contra el UNIQUE en vez de duplicar la base de perdidas.
  @JsonKey(

    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



      /// Bruta menos recuperacion, calculada por el motor.
  @JsonKey(

    name: r'perdidaNeta',
    required: true,
    includeIfNull: false,
  )


  final String perdidaNeta;



  @JsonKey(

    name: r'moneda',
    required: true,
    includeIfNull: false,
  )


  final SalidaRiesgoOperativoMonedaEnum moneda;



  @JsonKey(

    name: r'planAccionId',
    required: false,
    includeIfNull: false,
  )


  final String? planAccionId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRiesgoOperativo &&
      other.eventoId == eventoId &&
      other.codigo == codigo &&
      other.perdidaNeta == perdidaNeta &&
      other.moneda == moneda &&
      other.planAccionId == planAccionId;

    @override
    int get hashCode =>
        eventoId.hashCode +
        codigo.hashCode +
        perdidaNeta.hashCode +
        moneda.hashCode +
        planAccionId.hashCode;

  factory SalidaRiesgoOperativo.fromJson(Map<String, dynamic> json) => _$SalidaRiesgoOperativoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRiesgoOperativoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRiesgoOperativoMonedaEnum {
@JsonValue(r'BOB')
BOB(r'BOB'),
@JsonValue(r'USD')
USD(r'USD');

const SalidaRiesgoOperativoMonedaEnum(this.value);

final String value;

@override
String toString() => value;
}


