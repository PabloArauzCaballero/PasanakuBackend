//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_resolucion_descargo.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaResolucionDescargo {
  /// Returns a new [EntradaResolucionDescargo] instance.
  EntradaResolucionDescargo({

    required  this.aceptado,

    required  this.resolucion,
  });

  @JsonKey(
    
    name: r'aceptado',
    required: true,
    includeIfNull: false,
  )


  final bool aceptado;



  @JsonKey(
    
    name: r'resolucion',
    required: true,
    includeIfNull: false,
  )


  final String resolucion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaResolucionDescargo &&
      other.aceptado == aceptado &&
      other.resolucion == resolucion;

    @override
    int get hashCode =>
        aceptado.hashCode +
        resolucion.hashCode;

  factory EntradaResolucionDescargo.fromJson(Map<String, dynamic> json) => _$EntradaResolucionDescargoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaResolucionDescargoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

