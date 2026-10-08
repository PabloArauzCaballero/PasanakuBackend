//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_verificacion_certificado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaVerificacionCertificado {
  /// Returns a new [SalidaVerificacionCertificado] instance.
  SalidaVerificacionCertificado({

    required  this.valido,

    required  this.estado,

     this.emitidoEn,

     this.expiraEn,
  });

  @JsonKey(
    
    name: r'valido',
    required: true,
    includeIfNull: false,
  )


  final bool valido;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaVerificacionCertificadoEstadoEnum estado;



  @JsonKey(
    
    name: r'emitidoEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? emitidoEn;



  @JsonKey(
    
    name: r'expiraEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? expiraEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaVerificacionCertificado &&
      other.valido == valido &&
      other.estado == estado &&
      other.emitidoEn == emitidoEn &&
      other.expiraEn == expiraEn;

    @override
    int get hashCode =>
        valido.hashCode +
        estado.hashCode +
        (emitidoEn == null ? 0 : emitidoEn.hashCode) +
        (expiraEn == null ? 0 : expiraEn.hashCode);

  factory SalidaVerificacionCertificado.fromJson(Map<String, dynamic> json) => _$SalidaVerificacionCertificadoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaVerificacionCertificadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaVerificacionCertificadoEstadoEnum {
@JsonValue(r'VIGENTE')
VIGENTE(r'VIGENTE'),
@JsonValue(r'VENCIDO')
VENCIDO(r'VENCIDO'),
@JsonValue(r'REVOCADO')
REVOCADO(r'REVOCADO'),
@JsonValue(r'NO_VALIDO')
NO_VALIDO(r'NO_VALIDO');

const SalidaVerificacionCertificadoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


