//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_restriccion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRestriccion {
  /// Returns a new [SalidaRestriccion] instance.
  SalidaRestriccion({

    required  this.restriccionId,

    required  this.nivel,

     this.vigenteHasta,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'restriccionId',
    required: true,
    includeIfNull: false,
  )


  final String restriccionId;



  @JsonKey(
    
    name: r'nivel',
    required: true,
    includeIfNull: false,
  )


  final String nivel;



  @JsonKey(
    
    name: r'vigenteHasta',
    required: false,
    includeIfNull: false,
  )


  final DateTime? vigenteHasta;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRestriccion &&
      other.restriccionId == restriccionId &&
      other.nivel == nivel &&
      other.vigenteHasta == vigenteHasta &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        restriccionId.hashCode +
        nivel.hashCode +
        (vigenteHasta == null ? 0 : vigenteHasta.hashCode) +
        esNueva.hashCode;

  factory SalidaRestriccion.fromJson(Map<String, dynamic> json) => _$SalidaRestriccionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRestriccionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

