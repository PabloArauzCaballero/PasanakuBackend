//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_resolucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaResolucion {
  /// Returns a new [EntradaResolucion] instance.
  EntradaResolucion({

    required  this.aceptada,

    required  this.argumento,

    required  this.evidenciasJson,

    required  this.resolucion,
  });

  @JsonKey(
    
    name: r'aceptada',
    required: true,
    includeIfNull: false,
  )


  final bool aceptada;



  @JsonKey(
    
    name: r'argumento',
    required: true,
    includeIfNull: false,
  )


  final String argumento;



  @JsonKey(
    
    name: r'evidenciasJson',
    required: true,
    includeIfNull: false,
  )


  final String evidenciasJson;



  @JsonKey(
    
    name: r'resolucion',
    required: true,
    includeIfNull: false,
  )


  final String resolucion;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaResolucion &&
      other.aceptada == aceptada &&
      other.argumento == argumento &&
      other.evidenciasJson == evidenciasJson &&
      other.resolucion == resolucion;

    @override
    int get hashCode =>
        aceptada.hashCode +
        argumento.hashCode +
        evidenciasJson.hashCode +
        resolucion.hashCode;

  factory EntradaResolucion.fromJson(Map<String, dynamic> json) => _$EntradaResolucionFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaResolucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

