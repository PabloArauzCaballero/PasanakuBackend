//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_derechos.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaDerechos {
  /// Returns a new [EntradaDerechos] instance.
  EntradaDerechos({

    required  this.usuarioId,

    required  this.tipo,

    required  this.descripcion,

    required  this.ultimaActividad,
  });

  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



      /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaDerechosTipoEnum tipo;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



      /// Desde cuando corre la retencion. Lo aporta quien atiende porque la ultima operacion del titular vive en `nucleo-financiero` y este servicio no la puede consultar. Hueco declarado: deja de ser parametro cuando el contrato de la billetera exponga la fecha.
  @JsonKey(
    
    name: r'ultimaActividad',
    required: true,
    includeIfNull: false,
  )


  final DateTime ultimaActividad;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaDerechos &&
      other.usuarioId == usuarioId &&
      other.tipo == tipo &&
      other.descripcion == descripcion &&
      other.ultimaActividad == ultimaActividad;

    @override
    int get hashCode =>
        usuarioId.hashCode +
        tipo.hashCode +
        descripcion.hashCode +
        ultimaActividad.hashCode;

  factory EntradaDerechos.fromJson(Map<String, dynamic> json) => _$EntradaDerechosFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaDerechosToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

/// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
enum EntradaDerechosTipoEnum {
    /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
@JsonValue(r'ACCESO')
ACCESO(r'ACCESO'),
    /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
@JsonValue(r'RECTIFICACION')
RECTIFICACION(r'RECTIFICACION'),
    /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
@JsonValue(r'OPOSICION')
OPOSICION(r'OPOSICION'),
    /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
@JsonValue(r'PORTABILIDAD')
PORTABILIDAD(r'PORTABILIDAD'),
    /// Los cinco que admite la tabla. CU-07 llama «supresion» a CANCELACION y el servicio acepta las dos palabras; el modelo guarda CANCELACION.
@JsonValue(r'CANCELACION')
CANCELACION(r'CANCELACION');

const EntradaDerechosTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


