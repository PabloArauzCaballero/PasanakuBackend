//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_respuesta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRespuesta {
  /// Returns a new [EntradaRespuesta] instance.
  EntradaRespuesta({

     this.canal,

    required  this.remitente,

    required  this.firma,

    required  this.cargaUtil,

     this.contenido,

    required  this.claveIdempotencia,

     this.notificacionRelacionadaId,
  });

  @JsonKey(
    
    name: r'canal',
    required: false,
    includeIfNull: false,
  )


  final EntradaRespuestaCanalEnum? canal;



  @JsonKey(
    
    name: r'remitente',
    required: true,
    includeIfNull: false,
  )


  final String remitente;



  @JsonKey(
    
    name: r'firma',
    required: true,
    includeIfNull: false,
  )


  final String firma;



  @JsonKey(
    
    name: r'cargaUtil',
    required: true,
    includeIfNull: false,
  )


  final String cargaUtil;



  @JsonKey(
    
    name: r'contenido',
    required: false,
    includeIfNull: false,
  )


  final String? contenido;



  @JsonKey(
    
    name: r'claveIdempotencia',
    required: true,
    includeIfNull: false,
  )


  final String claveIdempotencia;



  @JsonKey(
    
    name: r'notificacionRelacionadaId',
    required: false,
    includeIfNull: false,
  )


  final String? notificacionRelacionadaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRespuesta &&
      other.canal == canal &&
      other.remitente == remitente &&
      other.firma == firma &&
      other.cargaUtil == cargaUtil &&
      other.contenido == contenido &&
      other.claveIdempotencia == claveIdempotencia &&
      other.notificacionRelacionadaId == notificacionRelacionadaId;

    @override
    int get hashCode =>
        canal.hashCode +
        remitente.hashCode +
        firma.hashCode +
        cargaUtil.hashCode +
        contenido.hashCode +
        claveIdempotencia.hashCode +
        notificacionRelacionadaId.hashCode;

  factory EntradaRespuesta.fromJson(Map<String, dynamic> json) => _$EntradaRespuestaFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRespuestaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaRespuestaCanalEnum {
@JsonValue(r'IN_APP')
IN_APP(r'IN_APP'),
@JsonValue(r'CORREO')
CORREO(r'CORREO'),
@JsonValue(r'PUSH')
PUSH(r'PUSH'),
@JsonValue(r'WHATSAPP')
WHATSAPP(r'WHATSAPP'),
@JsonValue(r'SMS')
SMS(r'SMS'),
@JsonValue(r'LLAMADA_VOZ')
LLAMADA_VOZ(r'LLAMADA_VOZ');

const EntradaRespuestaCanalEnum(this.value);

final String value;

@override
String toString() => value;
}


