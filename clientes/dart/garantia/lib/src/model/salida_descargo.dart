//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_descargo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaDescargo {
  /// Returns a new [SalidaDescargo] instance.
  SalidaDescargo({

    required  this.descargoId,

    required  this.estado,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'descargoId',
    required: true,
    includeIfNull: false,
  )


  final String descargoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaDescargo &&
      other.descargoId == descargoId &&
      other.estado == estado &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        descargoId.hashCode +
        estado.hashCode +
        esNuevo.hashCode;

  factory SalidaDescargo.fromJson(Map<String, dynamic> json) => _$SalidaDescargoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaDescargoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

