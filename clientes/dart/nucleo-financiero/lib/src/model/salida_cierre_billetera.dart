//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_cierre_billetera.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCierreBilletera {
  /// Returns a new [SalidaCierreBilletera] instance.
  SalidaCierreBilletera({

    required  this.solicitudId,

    required  this.estado,

    required  this.saldoAlSolicitar,
  });

  @JsonKey(
    
    name: r'solicitudId',
    required: true,
    includeIfNull: false,
  )


  final String solicitudId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaCierreBilleteraEstadoEnum estado;



  @JsonKey(
    
    name: r'saldoAlSolicitar',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoAlSolicitar;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCierreBilletera &&
      other.solicitudId == solicitudId &&
      other.estado == estado &&
      other.saldoAlSolicitar == saldoAlSolicitar;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        estado.hashCode +
        saldoAlSolicitar.hashCode;

  factory SalidaCierreBilletera.fromJson(Map<String, dynamic> json) => _$SalidaCierreBilleteraFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCierreBilleteraToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaCierreBilleteraEstadoEnum {
@JsonValue(r'SOLICITADA')
SOLICITADA(r'SOLICITADA'),
@JsonValue(r'EN_VALIDACION')
EN_VALIDACION(r'EN_VALIDACION'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'EJECUTADA')
EJECUTADA(r'EJECUTADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaCierreBilleteraEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


