//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_identidad/src/model/limite_aplicable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_registro.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRegistro {
  /// Returns a new [SalidaRegistro] instance.
  SalidaRegistro({

    required  this.usuarioId,

    required  this.estado,

     this.cuentaBilleteraId,

     this.nivelDiligencia,

     this.limites,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaRegistroEstadoEnum estado;



  @JsonKey(
    
    name: r'cuentaBilleteraId',
    required: false,
    includeIfNull: false,
  )


  final String? cuentaBilleteraId;



  @JsonKey(
    
    name: r'nivelDiligencia',
    required: false,
    includeIfNull: false,
  )


  final SalidaRegistroNivelDiligenciaEnum? nivelDiligencia;



  @JsonKey(
    
    name: r'limites',
    required: false,
    includeIfNull: false,
  )


  final List<LimiteAplicable>? limites;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRegistro &&
      other.usuarioId == usuarioId &&
      other.estado == estado &&
      other.cuentaBilleteraId == cuentaBilleteraId &&
      other.nivelDiligencia == nivelDiligencia &&
      other.limites == limites;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        estado.hashCode +
        cuentaBilleteraId.hashCode +
        nivelDiligencia.hashCode +
        limites.hashCode;

  factory SalidaRegistro.fromJson(Map<String, dynamic> json) => _$SalidaRegistroFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRegistroToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRegistroEstadoEnum {
@JsonValue(r'PENDIENTE_VERIFICACION')
PENDIENTE_VERIFICACION(r'PENDIENTE_VERIFICACION'),
@JsonValue(r'ACTIVO')
ACTIVO(r'ACTIVO');

const SalidaRegistroEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaRegistroNivelDiligenciaEnum {
@JsonValue(r'SIMPLIFICADA')
SIMPLIFICADA(r'SIMPLIFICADA'),
@JsonValue(r'ESTANDAR')
ESTANDAR(r'ESTANDAR'),
@JsonValue(r'AMPLIADA')
AMPLIADA(r'AMPLIADA'),
@JsonValue(r'REFORZADA')
REFORZADA(r'REFORZADA');

const SalidaRegistroNivelDiligenciaEnum(this.value);

final String value;

@override
String toString() => value;
}


