//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_derechos.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDerechos {
  /// Returns a new [SalidaDerechos] instance.
  SalidaDerechos({

    required  this.solicitudId,

    required  this.fechaLimiteLegal,

    required  this.estado,

    required  this.datosRetenidosPorLey,

     this.procesoAnonimizacionId,

     this.estrategia,
  });

  @JsonKey(
    
    name: r'solicitudId',
    required: true,
    includeIfNull: false,
  )


  final String solicitudId;



  @JsonKey(
    
    name: r'fechaLimiteLegal',
    required: true,
    includeIfNull: false,
  )


  final DateTime fechaLimiteLegal;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaDerechosEstadoEnum estado;



      /// Que quedo conservado y con que base legal. Se le dice al titular.
  @JsonKey(
    
    name: r'datosRetenidosPorLey',
    required: true,
    includeIfNull: false,
  )


  final List<String> datosRetenidosPorLey;



  @JsonKey(
    
    name: r'procesoAnonimizacionId',
    required: false,
    includeIfNull: false,
  )


  final String? procesoAnonimizacionId;



  @JsonKey(
    
    name: r'estrategia',
    required: false,
    includeIfNull: false,
  )


  final SalidaDerechosEstrategiaEnum? estrategia;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDerechos &&
      other.solicitudId == solicitudId &&
      other.fechaLimiteLegal == fechaLimiteLegal &&
      other.estado == estado &&
      other.datosRetenidosPorLey == datosRetenidosPorLey &&
      other.procesoAnonimizacionId == procesoAnonimizacionId &&
      other.estrategia == estrategia;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        fechaLimiteLegal.hashCode +
        estado.hashCode +
        datosRetenidosPorLey.hashCode +
        procesoAnonimizacionId.hashCode +
        estrategia.hashCode;

  factory SalidaDerechos.fromJson(Map<String, dynamic> json) => _$SalidaDerechosFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDerechosToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaDerechosEstadoEnum {
@JsonValue(r'RECIBIDA')
RECIBIDA(r'RECIBIDA'),
@JsonValue(r'EN_PROCESO')
EN_PROCESO(r'EN_PROCESO'),
@JsonValue(r'ATENDIDA')
ATENDIDA(r'ATENDIDA'),
@JsonValue(r'PARCIAL')
PARCIAL(r'PARCIAL'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaDerechosEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaDerechosEstrategiaEnum {
@JsonValue(r'BORRADO_TOTAL')
BORRADO_TOTAL(r'BORRADO_TOTAL'),
@JsonValue(r'BORRADO_PARCIAL')
BORRADO_PARCIAL(r'BORRADO_PARCIAL'),
@JsonValue(r'SEUDONIMIZACION')
SEUDONIMIZACION(r'SEUDONIMIZACION');

const SalidaDerechosEstrategiaEnum(this.value);

final String value;

@override
String toString() => value;
}


