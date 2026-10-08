//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_firma.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaFirma {
  /// Returns a new [SalidaFirma] instance.
  SalidaFirma({

    required  this.contratoId,

     this.firmadoEn,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'contratoId',
    required: true,
    includeIfNull: false,
  )


  final String contratoId;



  @JsonKey(
    
    name: r'firmadoEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? firmadoEn;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaFirma &&
      other.contratoId == contratoId &&
      other.firmadoEn == firmadoEn &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        contratoId.hashCode +
        (firmadoEn == null ? 0 : firmadoEn.hashCode) +
        esNueva.hashCode;

  factory SalidaFirma.fromJson(Map<String, dynamic> json) => _$SalidaFirmaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaFirmaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

