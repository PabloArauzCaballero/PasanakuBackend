//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_enrutamiento.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEnrutamiento {
  /// Returns a new [SalidaEnrutamiento] instance.
  SalidaEnrutamiento({

    required  this.proveedorId,

    required  this.proveedorCodigo,

    required  this.claveIdempotencia,

    required  this.estadoAnteTimeout,

    required  this.puedeConsultarEstado,
  });

  @JsonKey(
    
    name: r'proveedorId',
    required: true,
    includeIfNull: false,
  )


  final String proveedorId;



  @JsonKey(
    
    name: r'proveedorCodigo',
    required: true,
    includeIfNull: false,
  )


  final String proveedorCodigo;



      /// la MISMA que llego; conmutar de proveedor no la cambia
  @JsonKey(
    
    name: r'claveIdempotencia',
    required: true,
    includeIfNull: false,
  )


  final String claveIdempotencia;



  @JsonKey(
    
    name: r'estadoAnteTimeout',
    required: true,
    includeIfNull: false,
  )


  final SalidaEnrutamientoEstadoAnteTimeoutEnum estadoAnteTimeout;



  @JsonKey(
    
    name: r'puedeConsultarEstado',
    required: true,
    includeIfNull: false,
  )


  final bool puedeConsultarEstado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEnrutamiento &&
      other.proveedorId == proveedorId &&
      other.proveedorCodigo == proveedorCodigo &&
      other.claveIdempotencia == claveIdempotencia &&
      other.estadoAnteTimeout == estadoAnteTimeout &&
      other.puedeConsultarEstado == puedeConsultarEstado;

    @override
    int get hashCode =>
        proveedorId.hashCode +
        proveedorCodigo.hashCode +
        claveIdempotencia.hashCode +
        estadoAnteTimeout.hashCode +
        puedeConsultarEstado.hashCode;

  factory SalidaEnrutamiento.fromJson(Map<String, dynamic> json) => _$SalidaEnrutamientoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEnrutamientoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaEnrutamientoEstadoAnteTimeoutEnum {
@JsonValue(r'REINTENTABLE')
REINTENTABLE(r'REINTENTABLE'),
@JsonValue(r'EN_VERIFICACION')
EN_VERIFICACION(r'EN_VERIFICACION');

const SalidaEnrutamientoEstadoAnteTimeoutEnum(this.value);

final String value;

@override
String toString() => value;
}


