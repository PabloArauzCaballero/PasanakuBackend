//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_resena.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaResena {
  /// Returns a new [SalidaResena] instance.
  SalidaResena({

    required  this.resenaId,

    required  this.estadoModeracion,

    required  this.pesoEnReputacion,

     this.motivoModeracion,

    required  this.marcas,
  });

  @JsonKey(
    
    name: r'resenaId',
    required: true,
    includeIfNull: false,
  )


  final String resenaId;



      /// Catalogo cerrado de ck_resena_participante_estado_moderacion.
  @JsonKey(
    
    name: r'estadoModeracion',
    required: true,
    includeIfNull: false,
  )


  final SalidaResenaEstadoModeracionEnum estadoModeracion;



      /// Siempre menor que el peso de los factores de pago.
  @JsonKey(
    
    name: r'pesoEnReputacion',
    required: true,
    includeIfNull: false,
  )


  final String pesoEnReputacion;



  @JsonKey(
    
    name: r'motivoModeracion',
    required: false,
    includeIfNull: false,
  )


  final String? motivoModeracion;



      /// Lo que la primera pasada encontro. Retiene, no recorta.
  @JsonKey(
    
    name: r'marcas',
    required: true,
    includeIfNull: false,
  )


  final List<SalidaResenaMarcasEnum> marcas;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaResena &&
      other.resenaId == resenaId &&
      other.estadoModeracion == estadoModeracion &&
      other.pesoEnReputacion == pesoEnReputacion &&
      other.motivoModeracion == motivoModeracion &&
      other.marcas == marcas;

    @override
    int get hashCode =>
        resenaId.hashCode +
        estadoModeracion.hashCode +
        pesoEnReputacion.hashCode +
        (motivoModeracion == null ? 0 : motivoModeracion.hashCode) +
        marcas.hashCode;

  factory SalidaResena.fromJson(Map<String, dynamic> json) => _$SalidaResenaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaResenaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Catalogo cerrado de ck_resena_participante_estado_moderacion.
enum SalidaResenaEstadoModeracionEnum {
    /// Catalogo cerrado de ck_resena_participante_estado_moderacion.
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
    /// Catalogo cerrado de ck_resena_participante_estado_moderacion.
@JsonValue(r'PUBLICADA')
PUBLICADA(r'PUBLICADA'),
    /// Catalogo cerrado de ck_resena_participante_estado_moderacion.
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA'),
    /// Catalogo cerrado de ck_resena_participante_estado_moderacion.
@JsonValue(r'OCULTA')
OCULTA(r'OCULTA');

const SalidaResenaEstadoModeracionEnum(this.value);

final String value;

@override
String toString() => value;
}



enum SalidaResenaMarcasEnum {
@JsonValue(r'TELEFONO')
TELEFONO(r'TELEFONO'),
@JsonValue(r'CORREO')
CORREO(r'CORREO'),
@JsonValue(r'DOCUMENTO')
DOCUMENTO(r'DOCUMENTO');

const SalidaResenaMarcasEnum(this.value);

final String value;

@override
String toString() => value;
}


