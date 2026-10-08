//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_incidente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaIncidente {
  /// Returns a new [EntradaIncidente] instance.
  EntradaIncidente({

    required  this.tipo,

    required  this.severidad,

     this.activoInformacionId,

    required  this.datosPersonalesAfectados,

    required  this.usuariosAfectados,

    required  this.detectadoEn,

     this.eventoRiesgoId,
  });

  @JsonKey(

    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaIncidenteTipoEnum tipo;



  @JsonKey(

    name: r'severidad',
    required: true,
    includeIfNull: false,
  )


  final EntradaIncidenteSeveridadEnum severidad;



      /// El activo del inventario que se vio afectado. De el salen la clasificacion de datos y el contrato del proveedor, si lo hay.
  @JsonKey(

    name: r'activoInformacionId',
    required: false,
    includeIfNull: false,
  )


  final String? activoInformacionId;



      /// Lo que declara quien reporta. El activo lo puede sobrescribir: la clasificacion del inventario sabe mas que quien esta apagando el incendio.
  @JsonKey(

    name: r'datosPersonalesAfectados',
    required: true,
    includeIfNull: false,
  )


  final bool datosPersonalesAfectados;



      /// Aproximado esta bien. Esperar al numero final para notificar es como se pierde el plazo de notificacion.
          // minimum: 0
  @JsonKey(

    name: r'usuariosAfectados',
    required: true,
    includeIfNull: false,
  )


  final int usuariosAfectados;



      /// Desde aca corren los plazos, y no desde la ocurrencia: no se le puede exigir a nadie reportar algo que todavia no sabia.
  @JsonKey(

    name: r'detectadoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime detectadoEn;



      /// La perdida asociada, si ya se registro (CU-54).
  @JsonKey(

    name: r'eventoRiesgoId',
    required: false,
    includeIfNull: false,
  )


  final String? eventoRiesgoId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaIncidente &&
      other.tipo == tipo &&
      other.severidad == severidad &&
      other.activoInformacionId == activoInformacionId &&
      other.datosPersonalesAfectados == datosPersonalesAfectados &&
      other.usuariosAfectados == usuariosAfectados &&
      other.detectadoEn == detectadoEn &&
      other.eventoRiesgoId == eventoRiesgoId;

    @override
    int get hashCode =>
        tipo.hashCode +
        severidad.hashCode +
        activoInformacionId.hashCode +
        datosPersonalesAfectados.hashCode +
        usuariosAfectados.hashCode +
        detectadoEn.hashCode +
        eventoRiesgoId.hashCode;

  factory EntradaIncidente.fromJson(Map<String, dynamic> json) => _$EntradaIncidenteFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaIncidenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaIncidenteTipoEnum {
@JsonValue(r'ACCESO_NO_AUTORIZADO')
ACCESO_NO_AUTORIZADO(r'ACCESO_NO_AUTORIZADO'),
@JsonValue(r'DENEGACION_SERVICIO')
DENEGACION_SERVICIO(r'DENEGACION_SERVICIO'),
@JsonValue(r'FRAUDE_TECNOLOGICO')
FRAUDE_TECNOLOGICO(r'FRAUDE_TECNOLOGICO'),
@JsonValue(r'FUGA_DE_DATOS')
FUGA_DE_DATOS(r'FUGA_DE_DATOS'),
@JsonValue(r'MALWARE')
MALWARE(r'MALWARE'),
@JsonValue(r'PHISHING')
PHISHING(r'PHISHING');

const EntradaIncidenteTipoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum EntradaIncidenteSeveridadEnum {
@JsonValue(r'BAJA')
BAJA(r'BAJA'),
@JsonValue(r'MEDIA')
MEDIA(r'MEDIA'),
@JsonValue(r'ALTA')
ALTA(r'ALTA'),
@JsonValue(r'CRITICA')
CRITICA(r'CRITICA');

const EntradaIncidenteSeveridadEnum(this.value);

final String value;

@override
String toString() => value;
}


