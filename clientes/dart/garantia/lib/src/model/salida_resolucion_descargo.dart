//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_resolucion_descargo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaResolucionDescargo {
  /// Returns a new [SalidaResolucionDescargo] instance.
  SalidaResolucionDescargo({

    required  this.descargoId,

    required  this.estadoDelDescargo,

    required  this.estadoDelExpediente,
  });

  @JsonKey(
    
    name: r'descargoId',
    required: true,
    includeIfNull: false,
  )


  final String descargoId;



  @JsonKey(
    
    name: r'estadoDelDescargo',
    required: true,
    includeIfNull: false,
  )


  final SalidaResolucionDescargoEstadoDelDescargoEnum estadoDelDescargo;



  @JsonKey(
    
    name: r'estadoDelExpediente',
    required: true,
    includeIfNull: false,
  )


  final String estadoDelExpediente;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaResolucionDescargo &&
      other.descargoId == descargoId &&
      other.estadoDelDescargo == estadoDelDescargo &&
      other.estadoDelExpediente == estadoDelExpediente;

    @override
    int get hashCode =>
        descargoId.hashCode +
        estadoDelDescargo.hashCode +
        estadoDelExpediente.hashCode;

  factory SalidaResolucionDescargo.fromJson(Map<String, dynamic> json) => _$SalidaResolucionDescargoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaResolucionDescargoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaResolucionDescargoEstadoDelDescargoEnum {
@JsonValue(r'ACEPTADO')
ACEPTADO(r'ACEPTADO'),
@JsonValue(r'RECHAZADO')
RECHAZADO(r'RECHAZADO');

const SalidaResolucionDescargoEstadoDelDescargoEnum(this.value);

final String value;

@override
String toString() => value;
}


