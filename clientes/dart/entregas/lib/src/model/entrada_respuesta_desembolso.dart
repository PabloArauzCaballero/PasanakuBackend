//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_respuesta_desembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaRespuestaDesembolso {
  /// Returns a new [EntradaRespuestaDesembolso] instance.
  EntradaRespuestaDesembolso({

    required  this.iniciado,

    required  this.exitoso,

     this.referenciaProveedor,

     this.codigoError,

     this.mensajeProveedor,
  });

  @JsonKey(
    
    name: r'iniciado',
    required: true,
    includeIfNull: false,
  )


  final DateTime iniciado;



  @JsonKey(
    
    name: r'exitoso',
    required: true,
    includeIfNull: false,
  )


  final bool exitoso;



      /// obligatoria si fue exitoso — es contra lo que cruza el extracto
  @JsonKey(
    
    name: r'referenciaProveedor',
    required: false,
    includeIfNull: false,
  )


  final String? referenciaProveedor;



      /// CUENTA_INEXISTENTE, CUENTA_CERRADA, TITULAR_NO_COINCIDE y MONEDA_INCOMPATIBLE son definitivos: no se reintentan
  @JsonKey(
    
    name: r'codigoError',
    required: false,
    includeIfNull: false,
  )


  final String? codigoError;



  @JsonKey(
    
    name: r'mensajeProveedor',
    required: false,
    includeIfNull: false,
  )


  final String? mensajeProveedor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaRespuestaDesembolso &&
      other.iniciado == iniciado &&
      other.exitoso == exitoso &&
      other.referenciaProveedor == referenciaProveedor &&
      other.codigoError == codigoError &&
      other.mensajeProveedor == mensajeProveedor;

    @override
    int get hashCode =>
        iniciado.hashCode +
        exitoso.hashCode +
        (referenciaProveedor == null ? 0 : referenciaProveedor.hashCode) +
        (codigoError == null ? 0 : codigoError.hashCode) +
        (mensajeProveedor == null ? 0 : mensajeProveedor.hashCode);

  factory EntradaRespuestaDesembolso.fromJson(Map<String, dynamic> json) => _$EntradaRespuestaDesembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaRespuestaDesembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

