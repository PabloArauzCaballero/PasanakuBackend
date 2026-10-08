//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_retiro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRetiro {
  /// Returns a new [SalidaRetiro] instance.
  SalidaRetiro({

    required  this.ordenRetiroId,

    required  this.estado,

    required  this.costoRetiro,

    required  this.montoNeto,

    required  this.retencionId,
  });

  @JsonKey(
    
    name: r'ordenRetiroId',
    required: true,
    includeIfNull: false,
  )


  final String ordenRetiroId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaRetiroEstadoEnum estado;



  @JsonKey(
    
    name: r'costoRetiro',
    required: true,
    includeIfNull: false,
  )


  final Dinero costoRetiro;



  @JsonKey(
    
    name: r'montoNeto',
    required: true,
    includeIfNull: false,
  )


  final Dinero montoNeto;



  @JsonKey(
    
    name: r'retencionId',
    required: true,
    includeIfNull: false,
  )


  final String retencionId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRetiro &&
      other.ordenRetiroId == ordenRetiroId &&
      other.estado == estado &&
      other.costoRetiro == costoRetiro &&
      other.montoNeto == montoNeto &&
      other.retencionId == retencionId;

    @override
    int get hashCode =>
        ordenRetiroId.hashCode +
        estado.hashCode +
        costoRetiro.hashCode +
        montoNeto.hashCode +
        retencionId.hashCode;

  factory SalidaRetiro.fromJson(Map<String, dynamic> json) => _$SalidaRetiroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRetiroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRetiroEstadoEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'EN_REVISION')
EN_REVISION(r'EN_REVISION'),
@JsonValue(r'AUTORIZADA')
AUTORIZADA(r'AUTORIZADA'),
@JsonValue(r'PAGADA')
PAGADA(r'PAGADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaRetiroEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


