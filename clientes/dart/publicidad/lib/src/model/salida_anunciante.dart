//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_anunciante.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAnunciante {
  /// Returns a new [SalidaAnunciante] instance.
  SalidaAnunciante({

    required  this.anuncianteId,

    required  this.cuentaPublicitariaId,
  });

  @JsonKey(
    
    name: r'anuncianteId',
    required: true,
    includeIfNull: false,
  )


  final String anuncianteId;



  @JsonKey(
    
    name: r'cuentaPublicitariaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaPublicitariaId;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAnunciante &&
      other.anuncianteId == anuncianteId &&
      other.cuentaPublicitariaId == cuentaPublicitariaId;

    @override
    int get hashCode =>
        anuncianteId.hashCode +
        cuentaPublicitariaId.hashCode;

  factory SalidaAnunciante.fromJson(Map<String, dynamic> json) => _$SalidaAnuncianteFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAnuncianteToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

