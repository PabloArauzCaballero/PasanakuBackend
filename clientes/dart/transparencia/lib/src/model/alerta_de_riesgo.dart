//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'alerta_de_riesgo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class AlertaDeRiesgo {
  /// Returns a new [AlertaDeRiesgo] instance.
  AlertaDeRiesgo({

    required  this.alertaId,

    required  this.codigo,

    required  this.severidad,

    required  this.descripcion,

    required  this.accionSugerida,

    required  this.mensajeAlUsuario,
  });

  @JsonKey(
    
    name: r'alertaId',
    required: true,
    includeIfNull: false,
  )


  final String alertaId;



  @JsonKey(
    
    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final AlertaDeRiesgoCodigoEnum codigo;



  @JsonKey(
    
    name: r'severidad',
    required: true,
    includeIfNull: false,
  )


  final AlertaDeRiesgoSeveridadEnum severidad;



  @JsonKey(
    
    name: r'descripcion',
    required: true,
    includeIfNull: false,
  )


  final String descripcion;



  @JsonKey(
    
    name: r'accionSugerida',
    required: true,
    includeIfNull: false,
  )


  final String accionSugerida;



      /// En hechos, nunca en probabilidades. Si se lee el puntaje aca, esta mal.
  @JsonKey(
    
    name: r'mensajeAlUsuario',
    required: true,
    includeIfNull: false,
  )


  final String mensajeAlUsuario;





    @override
    bool operator ==(Object other) => identical(this, other) || other is AlertaDeRiesgo &&
      other.alertaId == alertaId &&
      other.codigo == codigo &&
      other.severidad == severidad &&
      other.descripcion == descripcion &&
      other.accionSugerida == accionSugerida &&
      other.mensajeAlUsuario == mensajeAlUsuario;

    @override
    int get hashCode =>
        alertaId.hashCode +
        codigo.hashCode +
        severidad.hashCode +
        descripcion.hashCode +
        accionSugerida.hashCode +
        mensajeAlUsuario.hashCode;

  factory AlertaDeRiesgo.fromJson(Map<String, dynamic> json) => _$AlertaDeRiesgoFromJson(json);

  Map<String, dynamic> toJson() => _$AlertaDeRiesgoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum AlertaDeRiesgoCodigoEnum {
@JsonValue(r'CAIDA_ABRUPTA_SCORE')
CAIDA_ABRUPTA_SCORE(r'CAIDA_ABRUPTA_SCORE'),
@JsonValue(r'GRUPO_INVIABLE')
GRUPO_INVIABLE(r'GRUPO_INVIABLE'),
@JsonValue(r'MORA_CONCENTRADA')
MORA_CONCENTRADA(r'MORA_CONCENTRADA'),
@JsonValue(r'RETIRO_MASIVO')
RETIRO_MASIVO(r'RETIRO_MASIVO');

const AlertaDeRiesgoCodigoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum AlertaDeRiesgoSeveridadEnum {
@JsonValue(r'BAJA')
BAJA(r'BAJA'),
@JsonValue(r'MEDIA')
MEDIA(r'MEDIA'),
@JsonValue(r'ALTA')
ALTA(r'ALTA'),
@JsonValue(r'CRITICA')
CRITICA(r'CRITICA');

const AlertaDeRiesgoSeveridadEnum(this.value);

final String value;

@override
String toString() => value;
}


