//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_certificado.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCertificado {
  /// Returns a new [SalidaCertificado] instance.
  SalidaCertificado({

    required  this.certificadoId,

    required  this.codigoVerificacion,

    required  this.urlPublica,

    required  this.hashContenido,

    required  this.emitidoEn,

    required  this.expiraEn,

    required  this.contenido,
  });

  @JsonKey(
    
    name: r'certificadoId',
    required: true,
    includeIfNull: false,
  )


  final String certificadoId;



  @JsonKey(
    
    name: r'codigoVerificacion',
    required: true,
    includeIfNull: false,
  )


  final String codigoVerificacion;



  @JsonKey(
    
    name: r'urlPublica',
    required: true,
    includeIfNull: false,
  )


  final String urlPublica;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'hashContenido',
    required: true,
    includeIfNull: false,
  )


  final String hashContenido;



  @JsonKey(
    
    name: r'emitidoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime emitidoEn;



  @JsonKey(
    
    name: r'expiraEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime expiraEn;



  @JsonKey(
    
    name: r'contenido',
    required: true,
    includeIfNull: false,
  )


  final Map<String, String> contenido;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCertificado &&
      other.certificadoId == certificadoId &&
      other.codigoVerificacion == codigoVerificacion &&
      other.urlPublica == urlPublica &&
      other.hashContenido == hashContenido &&
      other.emitidoEn == emitidoEn &&
      other.expiraEn == expiraEn &&
      other.contenido == contenido;

    @override
    int get hashCode =>
        certificadoId.hashCode +
        codigoVerificacion.hashCode +
        urlPublica.hashCode +
        hashContenido.hashCode +
        emitidoEn.hashCode +
        expiraEn.hashCode +
        contenido.hashCode;

  factory SalidaCertificado.fromJson(Map<String, dynamic> json) => _$SalidaCertificadoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCertificadoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

