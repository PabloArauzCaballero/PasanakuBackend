//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_respuesta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRespuesta {
  /// Returns a new [SalidaRespuesta] instance.
  SalidaRespuesta({

     this.respuestaId,

    required  this.intencion,

    required  this.accion,

     this.referenciaId,
  });

  @JsonKey(
    
    name: r'respuestaId',
    required: false,
    includeIfNull: false,
  )


  final String? respuestaId;



  @JsonKey(
    
    name: r'intencion',
    required: true,
    includeIfNull: false,
  )


  final SalidaRespuestaIntencionEnum intencion;



  @JsonKey(
    
    name: r'accion',
    required: true,
    includeIfNull: false,
  )


  final SalidaRespuestaAccionEnum accion;



  @JsonKey(
    
    name: r'referenciaId',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRespuesta &&
      other.respuestaId == respuestaId &&
      other.intencion == intencion &&
      other.accion == accion &&
      other.referenciaId == referenciaId;

    @override
    int get hashCode =>
        respuestaId.hashCode +
        intencion.hashCode +
        accion.hashCode +
        referenciaId.hashCode;

  factory SalidaRespuesta.fromJson(Map<String, dynamic> json) => _$SalidaRespuestaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRespuestaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRespuestaIntencionEnum {
@JsonValue(r'YA_PAGUE')
YA_PAGUE(r'YA_PAGUE'),
@JsonValue(r'NO_PUEDO')
NO_PUEDO(r'NO_PUEDO'),
@JsonValue(r'BAJA')
BAJA(r'BAJA'),
@JsonValue(r'CONSULTA')
CONSULTA(r'CONSULTA'),
@JsonValue(r'NO_RECONOZCO')
NO_RECONOZCO(r'NO_RECONOZCO'),
@JsonValue(r'DESCONOCIDA')
DESCONOCIDA(r'DESCONOCIDA');

const SalidaRespuestaIntencionEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaRespuestaAccionEnum {
@JsonValue(r'PAGO_CONFIRMADO')
PAGO_CONFIRMADO(r'PAGO_CONFIRMADO'),
@JsonValue(r'COMPROBANTE_SOLICITADO')
COMPROBANTE_SOLICITADO(r'COMPROBANTE_SOLICITADO'),
@JsonValue(r'PROMESA_REGISTRADA')
PROMESA_REGISTRADA(r'PROMESA_REGISTRADA'),
@JsonValue(r'SUPRESION_APLICADA')
SUPRESION_APLICADA(r'SUPRESION_APLICADA'),
@JsonValue(r'RECLAMO_ABIERTO')
RECLAMO_ABIERTO(r'RECLAMO_ABIERTO'),
@JsonValue(r'TICKET_ABIERTO')
TICKET_ABIERTO(r'TICKET_ABIERTO');

const SalidaRespuestaAccionEnum(this.value);

final String value;

@override
String toString() => value;
}


