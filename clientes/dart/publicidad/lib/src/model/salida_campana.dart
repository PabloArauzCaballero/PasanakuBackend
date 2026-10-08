//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_campana.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCampana {
  /// Returns a new [SalidaCampana] instance.
  SalidaCampana({

    required  this.campanaPublicitariaId,

    required  this.estado,

    required  this.presupuestoTotal,
  });

  @JsonKey(
    
    name: r'campanaPublicitariaId',
    required: true,
    includeIfNull: false,
  )


  final String campanaPublicitariaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaCampanaEstadoEnum estado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'presupuestoTotal',
    required: true,
    includeIfNull: false,
  )


  final String presupuestoTotal;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCampana &&
      other.campanaPublicitariaId == campanaPublicitariaId &&
      other.estado == estado &&
      other.presupuestoTotal == presupuestoTotal;

    @override
    int get hashCode =>
        campanaPublicitariaId.hashCode +
        estado.hashCode +
        presupuestoTotal.hashCode;

  factory SalidaCampana.fromJson(Map<String, dynamic> json) => _$SalidaCampanaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCampanaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCampanaEstadoEnum {
@JsonValue(r'BORRADOR')
BORRADOR(r'BORRADOR'),
@JsonValue(r'EN_REVISION')
EN_REVISION(r'EN_REVISION'),
@JsonValue(r'ACTIVA')
ACTIVA(r'ACTIVA'),
@JsonValue(r'PAUSADA')
PAUSADA(r'PAUSADA'),
@JsonValue(r'FINALIZADA')
FINALIZADA(r'FINALIZADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaCampanaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


