//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_cierre.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCierre {
  /// Returns a new [SalidaCierre] instance.
  SalidaCierre({

    required  this.cierreId,

    required  this.periodoId,

    required  this.totalDebe,

    required  this.totalHaber,

    required  this.cerradoEn,

     this.ejercicioCerrado,
  });

  @JsonKey(
    
    name: r'cierreId',
    required: true,
    includeIfNull: false,
  )


  final String cierreId;



  @JsonKey(
    
    name: r'periodoId',
    required: true,
    includeIfNull: false,
  )


  final String periodoId;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'totalDebe',
    required: true,
    includeIfNull: false,
  )


  final String totalDebe;



      /// Importe como CADENA decimal. Un number JSON es un doble.
  @JsonKey(
    
    name: r'totalHaber',
    required: true,
    includeIfNull: false,
  )


  final String totalHaber;



  @JsonKey(
    
    name: r'cerradoEn',
    required: true,
    includeIfNull: false,
  )


  final DateTime cerradoEn;



      /// Cierto si era el ultimo mes del ejercicio
  @JsonKey(
    
    name: r'ejercicioCerrado',
    required: false,
    includeIfNull: false,
  )


  final bool? ejercicioCerrado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCierre &&
      other.cierreId == cierreId &&
      other.periodoId == periodoId &&
      other.totalDebe == totalDebe &&
      other.totalHaber == totalHaber &&
      other.cerradoEn == cerradoEn &&
      other.ejercicioCerrado == ejercicioCerrado;

    @override
    int get hashCode =>
        cierreId.hashCode +
        periodoId.hashCode +
        totalDebe.hashCode +
        totalHaber.hashCode +
        cerradoEn.hashCode +
        ejercicioCerrado.hashCode;

  factory SalidaCierre.fromJson(Map<String, dynamic> json) => _$SalidaCierreFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCierreToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

