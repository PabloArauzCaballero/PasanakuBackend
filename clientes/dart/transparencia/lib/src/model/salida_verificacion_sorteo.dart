//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_transparencia/src/model/salida_verificacion_sorteo_paquete.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_verificacion_sorteo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaVerificacionSorteo {
  /// Returns a new [SalidaVerificacionSorteo] instance.
  SalidaVerificacionSorteo({

    required  this.verifica,

    required  this.hashEsperado,

    required  this.hashRecomputado,

    required  this.ordenCoincide,

     this.primerCupoDiscrepante,

    required  this.paquete,
  });

  @JsonKey(
    
    name: r'verifica',
    required: true,
    includeIfNull: false,
  )


  final bool verifica;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'hashEsperado',
    required: true,
    includeIfNull: false,
  )


  final String hashEsperado;



      /// SHA-256 en hexadecimal minuscula.
  @JsonKey(
    
    name: r'hashRecomputado',
    required: true,
    includeIfNull: false,
  )


  final String hashRecomputado;



  @JsonKey(
    
    name: r'ordenCoincide',
    required: true,
    includeIfNull: false,
  )


  final bool ordenCoincide;



  @JsonKey(
    
    name: r'primerCupoDiscrepante',
    required: false,
    includeIfNull: false,
  )


  final int? primerCupoDiscrepante;



  @JsonKey(
    
    name: r'paquete',
    required: true,
    includeIfNull: false,
  )


  final SalidaVerificacionSorteoPaquete paquete;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaVerificacionSorteo &&
      other.verifica == verifica &&
      other.hashEsperado == hashEsperado &&
      other.hashRecomputado == hashRecomputado &&
      other.ordenCoincide == ordenCoincide &&
      other.primerCupoDiscrepante == primerCupoDiscrepante &&
      other.paquete == paquete;

    @override
    int get hashCode =>
        verifica.hashCode +
        hashEsperado.hashCode +
        hashRecomputado.hashCode +
        ordenCoincide.hashCode +
        primerCupoDiscrepante.hashCode +
        paquete.hashCode;

  factory SalidaVerificacionSorteo.fromJson(Map<String, dynamic> json) => _$SalidaVerificacionSorteoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaVerificacionSorteoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

