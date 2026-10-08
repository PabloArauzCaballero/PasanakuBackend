//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_verificacion_sorteo_paquete.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaVerificacionSorteoPaquete {
  /// Returns a new [SalidaVerificacionSorteoPaquete] instance.
  SalidaVerificacionSorteoPaquete({

    required  this.semilla,

    required  this.entropias,

    required  this.metodo,

    required  this.cupos,
  });

      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'semilla',
    required: true,
    includeIfNull: false,
  )


  final String semilla;



  @JsonKey(
    
    name: r'entropias',
    required: true,
    includeIfNull: false,
  )


  final List<String> entropias;



  @JsonKey(
    
    name: r'metodo',
    required: true,
    includeIfNull: false,
  )


  final String metodo;



  @JsonKey(
    
    name: r'cupos',
    required: true,
    includeIfNull: false,
  )


  final List<int> cupos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaVerificacionSorteoPaquete &&
      other.semilla == semilla &&
      other.entropias == entropias &&
      other.metodo == metodo &&
      other.cupos == cupos;

    @override
    int get hashCode =>
        semilla.hashCode +
        entropias.hashCode +
        metodo.hashCode +
        cupos.hashCode;

  factory SalidaVerificacionSorteoPaquete.fromJson(Map<String, dynamic> json) => _$SalidaVerificacionSorteoPaqueteFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaVerificacionSorteoPaqueteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

