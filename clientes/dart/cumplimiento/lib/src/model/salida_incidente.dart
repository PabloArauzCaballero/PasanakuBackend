//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_incidente.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaIncidente {
  /// Returns a new [SalidaIncidente] instance.
  SalidaIncidente({

    required  this.incidenteId,

    required  this.codigo,

    required  this.plazoReporte,

    required  this.requiereNotificarTitulares,

     this.plazoNotificacion,

     this.contratoTerceroId,

    required  this.registradoEn,
  });

  @JsonKey(

    name: r'incidenteId',
    required: true,
    includeIfNull: false,
  )


  final String incidenteId;



      /// Derivado del hecho. Dos avisos del mismo incidente, el del monitoreo y el de la persona que lo vio, producen el mismo codigo y el segundo choca contra el UNIQUE, en vez de abrir dos expedientes con dos relojes para el mismo hecho.
  @JsonKey(

    name: r'codigo',
    required: true,
    includeIfNull: false,
  )


  final String codigo;



      /// Guardado, no recalculado. Es el plazo que regia el dia del incidente.
  @JsonKey(

    name: r'plazoReporte',
    required: true,
    includeIfNull: false,
  )


  final DateTime plazoReporte;



  @JsonKey(

    name: r'requiereNotificarTitulares',
    required: true,
    includeIfNull: false,
  )


  final bool requiereNotificarTitulares;



      /// Ausente cuando no hay datos personales afectados.
  @JsonKey(

    name: r'plazoNotificacion',
    required: false,
    includeIfNull: false,
  )


  final DateTime? plazoNotificacion;



      /// El contrato del proveedor, resuelto por el activo afectado. Presente cuando el incidente ocurrio en un tercero: la responsabilidad frente al cliente no se terceriza, pero el contrato hay que poder mirarlo.
  @JsonKey(

    name: r'contratoTerceroId',
    required: false,
    includeIfNull: false,
  )


  final String? contratoTerceroId;



  @JsonKey(

    name: r'registradoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime registradoEn;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaIncidente &&
      other.incidenteId == incidenteId &&
      other.codigo == codigo &&
      other.plazoReporte == plazoReporte &&
      other.requiereNotificarTitulares == requiereNotificarTitulares &&
      other.plazoNotificacion == plazoNotificacion &&
      other.contratoTerceroId == contratoTerceroId &&
      other.registradoEn == registradoEn;

    @override
    int get hashCode =>
        incidenteId.hashCode +
        codigo.hashCode +
        plazoReporte.hashCode +
        requiereNotificarTitulares.hashCode +
        plazoNotificacion.hashCode +
        contratoTerceroId.hashCode +
        registradoEn.hashCode;

  factory SalidaIncidente.fromJson(Map<String, dynamic> json) => _$SalidaIncidenteFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaIncidenteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

