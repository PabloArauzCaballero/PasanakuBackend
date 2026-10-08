//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_presupuesto.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPresupuesto {
  /// Returns a new [SalidaPresupuesto] instance.
  SalidaPresupuesto({

    required  this.presupuestoId,

    required  this.estado,

    required  this.total,
  });

  @JsonKey(
    
    name: r'presupuestoId',
    required: true,
    includeIfNull: false,
  )


  final String presupuestoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaPresupuestoEstadoEnum estado;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'total',
    required: true,
    includeIfNull: false,
  )


  final String total;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPresupuesto &&
      other.presupuestoId == presupuestoId &&
      other.estado == estado &&
      other.total == total;

    @override
    int get hashCode =>
        presupuestoId.hashCode +
        estado.hashCode +
        total.hashCode;

  factory SalidaPresupuesto.fromJson(Map<String, dynamic> json) => _$SalidaPresupuestoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPresupuestoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaPresupuestoEstadoEnum {
@JsonValue(r'BORRADOR')
BORRADOR(r'BORRADOR'),
@JsonValue(r'APROBADO')
APROBADO(r'APROBADO'),
@JsonValue(r'CERRADO')
CERRADO(r'CERRADO');

const SalidaPresupuestoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


