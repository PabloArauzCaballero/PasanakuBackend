//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_verificacion_cadena.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaVerificacionCadena {
  /// Returns a new [SalidaVerificacionCadena] instance.
  SalidaVerificacionCadena({

    required  this.integra,

    required  this.bloquesVerificados,

     this.primerBloqueFallido,

     this.componenteFallido,

     this.ultimoSellado,
  });

  @JsonKey(
    
    name: r'integra',
    required: true,
    includeIfNull: false,
  )


  final bool integra;



          // minimum: 0
  @JsonKey(
    
    name: r'bloquesVerificados',
    required: true,
    includeIfNull: false,
  )


  final int bloquesVerificados;



  @JsonKey(
    
    name: r'primerBloqueFallido',
    required: false,
    includeIfNull: false,
  )


  final int? primerBloqueFallido;



  @JsonKey(
    
    name: r'componenteFallido',
    required: false,
    includeIfNull: false,
  )


  final SalidaVerificacionCadenaComponenteFallidoEnum? componenteFallido;



  @JsonKey(
    
    name: r'ultimoSellado',
    required: false,
    includeIfNull: false,
  )


  final DateTime? ultimoSellado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaVerificacionCadena &&
      other.integra == integra &&
      other.bloquesVerificados == bloquesVerificados &&
      other.primerBloqueFallido == primerBloqueFallido &&
      other.componenteFallido == componenteFallido &&
      other.ultimoSellado == ultimoSellado;

    @override
    int get hashCode =>
        integra.hashCode +
        bloquesVerificados.hashCode +
        (primerBloqueFallido == null ? 0 : primerBloqueFallido.hashCode) +
        (componenteFallido == null ? 0 : componenteFallido.hashCode) +
        (ultimoSellado == null ? 0 : ultimoSellado.hashCode);

  factory SalidaVerificacionCadena.fromJson(Map<String, dynamic> json) => _$SalidaVerificacionCadenaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaVerificacionCadenaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaVerificacionCadenaComponenteFallidoEnum {
@JsonValue(r'HASH_CONTENIDO')
HASH_CONTENIDO(r'HASH_CONTENIDO'),
@JsonValue(r'HASH_ANTERIOR')
HASH_ANTERIOR(r'HASH_ANTERIOR'),
@JsonValue(r'HASH_BLOQUE')
HASH_BLOQUE(r'HASH_BLOQUE'),
@JsonValue(r'SECUENCIA')
SECUENCIA(r'SECUENCIA');

const SalidaVerificacionCadenaComponenteFallidoEnum(this.value);

final String value;

@override
String toString() => value;
}


