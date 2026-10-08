//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_revision.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRevision {
  /// Returns a new [SalidaRevision] instance.
  SalidaRevision({

    required  this.revisionId,

    required  this.estadoModeracion,
  });

  @JsonKey(
    
    name: r'revisionId',
    required: true,
    includeIfNull: false,
  )


  final String revisionId;



  @JsonKey(
    
    name: r'estadoModeracion',
    required: true,
    includeIfNull: false,
  )


  final SalidaRevisionEstadoModeracionEnum estadoModeracion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRevision &&
      other.revisionId == revisionId &&
      other.estadoModeracion == estadoModeracion;

    @override
    int get hashCode =>
        revisionId.hashCode +
        estadoModeracion.hashCode;

  factory SalidaRevision.fromJson(Map<String, dynamic> json) => _$SalidaRevisionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRevisionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRevisionEstadoModeracionEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'APROBADA')
APROBADA(r'APROBADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaRevisionEstadoModeracionEnum(this.value);

final String value;

@override
String toString() => value;
}


