//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_ejecucion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEjecucion {
  /// Returns a new [SalidaEjecucion] instance.
  SalidaEjecucion({

     this.ejecucionId,

    required  this.estadoDeLaTarea,

    required  this.intentos,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'ejecucionId',
    required: false,
    includeIfNull: false,
  )


  final String? ejecucionId;



  @JsonKey(
    
    name: r'estadoDeLaTarea',
    required: true,
    includeIfNull: false,
  )


  final String estadoDeLaTarea;



          // minimum: 0
  @JsonKey(
    
    name: r'intentos',
    required: true,
    includeIfNull: false,
  )


  final int intentos;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEjecucion &&
      other.ejecucionId == ejecucionId &&
      other.estadoDeLaTarea == estadoDeLaTarea &&
      other.intentos == intentos &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        (ejecucionId == null ? 0 : ejecucionId.hashCode) +
        estadoDeLaTarea.hashCode +
        intentos.hashCode +
        esNueva.hashCode;

  factory SalidaEjecucion.fromJson(Map<String, dynamic> json) => _$SalidaEjecucionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEjecucionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

