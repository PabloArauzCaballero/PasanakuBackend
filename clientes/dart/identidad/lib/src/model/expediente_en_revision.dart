//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'expediente_en_revision.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class ExpedienteEnRevision {
  /// Returns a new [ExpedienteEnRevision] instance.
  ExpedienteEnRevision({

    required  this.verificacionId,

    required  this.usuarioId,

    required  this.nombreCompleto,

     this.documento,

    required  this.estado,

    required  this.iniciadaEn,

     this.resueltaEn,

     this.motivoRechazo,

    required  this.fotos,
  });

  @JsonKey(
    
    name: r'verificacionId',
    required: true,
    includeIfNull: false,
  )


  final String verificacionId;



  @JsonKey(
    
    name: r'usuarioId',
    required: true,
    includeIfNull: false,
  )


  final String usuarioId;



  @JsonKey(
    
    name: r'nombreCompleto',
    required: true,
    includeIfNull: false,
  )


  final String nombreCompleto;



      /// Tipo y lugar de expedicion; el numero NUNCA viaja.
  @JsonKey(
    
    name: r'documento',
    required: false,
    includeIfNull: false,
  )


  final String? documento;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final ExpedienteEnRevisionEstadoEnum estado;



  @JsonKey(
    
    name: r'iniciadaEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime iniciadaEn;



  @JsonKey(
    
    name: r'resueltaEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? resueltaEn;



  @JsonKey(
    
    name: r'motivoRechazo',
    required: false,
    includeIfNull: false,
  )


  final String? motivoRechazo;



      /// Que caras tiene cargadas. Un expediente sin las tres no se puede aprobar.
  @JsonKey(
    
    name: r'fotos',
    required: true,
    includeIfNull: false,
  )


  final List<ExpedienteEnRevisionFotosEnum> fotos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is ExpedienteEnRevision &&
      other.verificacionId == verificacionId &&
      other.usuarioId == usuarioId &&
      other.nombreCompleto == nombreCompleto &&
      other.documento == documento &&
      other.estado == estado &&
      other.iniciadaEn == iniciadaEn &&
      other.resueltaEn == resueltaEn &&
      other.motivoRechazo == motivoRechazo &&
      other.fotos == fotos;

    @override
    int get hashCode =>
        verificacionId.hashCode +
        usuarioId.hashCode +
        nombreCompleto.hashCode +
        documento.hashCode +
        estado.hashCode +
        iniciadaEn.hashCode +
        resueltaEn.hashCode +
        motivoRechazo.hashCode +
        fotos.hashCode;

  factory ExpedienteEnRevision.fromJson(Map<String, dynamic> json) => _$ExpedienteEnRevisionFromJson(json);

  Map<String, dynamic> toJson() => _$ExpedienteEnRevisionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum ExpedienteEnRevisionEstadoEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'EN_REVISION')
EN_REVISION(r'EN_REVISION'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const ExpedienteEnRevisionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}



enum ExpedienteEnRevisionFotosEnum {
@JsonValue(r'ANVERSO')
ANVERSO(r'ANVERSO'),
@JsonValue(r'REVERSO')
REVERSO(r'REVERSO'),
@JsonValue(r'SELFIE')
SELFIE(r'SELFIE');

const ExpedienteEnRevisionFotosEnum(this.value);

final String value;

@override
String toString() => value;
}


