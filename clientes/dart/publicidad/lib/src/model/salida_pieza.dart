//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_pieza.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPieza {
  /// Returns a new [SalidaPieza] instance.
  SalidaPieza({

    required  this.piezaCreativaId,

    required  this.estadoModeracion,
  });

  @JsonKey(
    
    name: r'piezaCreativaId',
    required: true,
    includeIfNull: false,
  )


  final String piezaCreativaId;



  @JsonKey(
    
    name: r'estadoModeracion',
    required: true,
    includeIfNull: false,
  )


  final SalidaPiezaEstadoModeracionEnum estadoModeracion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPieza &&
      other.piezaCreativaId == piezaCreativaId &&
      other.estadoModeracion == estadoModeracion;

    @override
    int get hashCode =>
        piezaCreativaId.hashCode +
        estadoModeracion.hashCode;

  factory SalidaPieza.fromJson(Map<String, dynamic> json) => _$SalidaPiezaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPiezaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPiezaEstadoModeracionEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaPiezaEstadoModeracionEnum(this.value);

final String value;

@override
String toString() => value;
}


