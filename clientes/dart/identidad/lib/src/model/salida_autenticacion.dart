//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_autenticacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAutenticacion {
  /// Returns a new [SalidaAutenticacion] instance.
  SalidaAutenticacion({

     this.sesionId,

     this.expiraEn,

    required  this.requiereFactorAdicional,

    required  this.dispositivoConfiable,

     this.tokenAcceso,
  });

  @JsonKey(
    
    name: r'sesionId',
    required: false,
    includeIfNull: false,
  )


  final String? sesionId;



  @JsonKey(
    
    name: r'expiraEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? expiraEn;



  @JsonKey(
    
    name: r'requiereFactorAdicional',
    required: true,
    includeIfNull: false,
  )


  final bool requiereFactorAdicional;



  @JsonKey(
    
    name: r'dispositivoConfiable',
    required: true,
    includeIfNull: false,
  )


  final bool dispositivoConfiable;



      /// El JWT firmado con RS256 que los otros trece servicios validan contra el JWKS de identidad (ADR-024). Ausente cuando la respuesta pide un segundo factor: recien se emite con la sesion abierta.
  @JsonKey(
    
    name: r'tokenAcceso',
    required: false,
    includeIfNull: false,
  )


  final String? tokenAcceso;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAutenticacion &&
      other.sesionId == sesionId &&
      other.expiraEn == expiraEn &&
      other.requiereFactorAdicional == requiereFactorAdicional &&
      other.dispositivoConfiable == dispositivoConfiable &&
      other.tokenAcceso == tokenAcceso;

    @override
    int get hashCode =>
        sesionId.hashCode +
        expiraEn.hashCode +
        requiereFactorAdicional.hashCode +
        dispositivoConfiable.hashCode +
        tokenAcceso.hashCode;

  factory SalidaAutenticacion.fromJson(Map<String, dynamic> json) => _$SalidaAutenticacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAutenticacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

