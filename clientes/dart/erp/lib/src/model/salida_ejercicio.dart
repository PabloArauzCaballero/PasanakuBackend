//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_ejercicio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaEjercicio {
  /// Returns a new [SalidaEjercicio] instance.
  SalidaEjercicio({

    required  this.ejercicioId,

    required  this.anio,

    required  this.periodos,
  });

  @JsonKey(
    
    name: r'ejercicioId',
    required: true,
    includeIfNull: false,
  )


  final String ejercicioId;



  @JsonKey(
    
    name: r'anio',
    required: true,
    includeIfNull: false,
  )


  final int anio;



      /// Siempre doce
  @JsonKey(
    
    name: r'periodos',
    required: true,
    includeIfNull: false,
  )


  final int periodos;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaEjercicio &&
      other.ejercicioId == ejercicioId &&
      other.anio == anio &&
      other.periodos == periodos;

    @override
    int get hashCode =>
        ejercicioId.hashCode +
        anio.hashCode +
        periodos.hashCode;

  factory SalidaEjercicio.fromJson(Map<String, dynamic> json) => _$SalidaEjercicioFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaEjercicioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

