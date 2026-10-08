//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_postulacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaPostulacion {
  /// Returns a new [SalidaPostulacion] instance.
  SalidaPostulacion({

    required  this.solicitudId,

    required  this.puntaje,

    required  this.motivos,
  });

  @JsonKey(
    
    name: r'solicitudId',
    required: true,
    includeIfNull: false,
  )


  final String solicitudId;



  @JsonKey(
    
    name: r'puntaje',
    required: true,
    includeIfNull: false,
  )


  final String puntaje;



  @JsonKey(
    
    name: r'motivos',
    required: true,
    includeIfNull: false,
  )


  final List<String> motivos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaPostulacion &&
      other.solicitudId == solicitudId &&
      other.puntaje == puntaje &&
      other.motivos == motivos;

    @override
    int get hashCode =>
        solicitudId.hashCode +
        puntaje.hashCode +
        motivos.hashCode;

  factory SalidaPostulacion.fromJson(Map<String, dynamic> json) => _$SalidaPostulacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaPostulacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

