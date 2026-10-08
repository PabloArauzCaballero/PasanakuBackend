//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_insignia.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaInsignia {
  /// Returns a new [SalidaInsignia] instance.
  SalidaInsignia({

    required  this.otorgadaId,

     this.insigniaCodigo,

    required  this.otorgadaEn,

    required  this.motivoLegible,

    required  this.revocada,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'otorgadaId',
    required: true,
    includeIfNull: false,
  )


  final String otorgadaId;



  @JsonKey(
    
    name: r'insigniaCodigo',
    required: false,
    includeIfNull: false,
  )


  final String? insigniaCodigo;



  @JsonKey(
    
    name: r'otorgadaEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime otorgadaEn;



  @JsonKey(
    
    name: r'motivoLegible',
    required: true,
    includeIfNull: false,
  )


  final String motivoLegible;



  @JsonKey(
    
    name: r'revocada',
    required: true,
    includeIfNull: false,
  )


  final bool revocada;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaInsignia &&
      other.otorgadaId == otorgadaId &&
      other.insigniaCodigo == insigniaCodigo &&
      other.otorgadaEn == otorgadaEn &&
      other.motivoLegible == motivoLegible &&
      other.revocada == revocada &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        otorgadaId.hashCode +
        (insigniaCodigo == null ? 0 : insigniaCodigo.hashCode) +
        otorgadaEn.hashCode +
        motivoLegible.hashCode +
        revocada.hashCode +
        esNueva.hashCode;

  factory SalidaInsignia.fromJson(Map<String, dynamic> json) => _$SalidaInsigniaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaInsigniaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

