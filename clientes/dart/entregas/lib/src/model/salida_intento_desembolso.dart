//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_intento_desembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaIntentoDesembolso {
  /// Returns a new [SalidaIntentoDesembolso] instance.
  SalidaIntentoDesembolso({

     this.ordenId,

    required  this.estado,

    required  this.numeroIntento,

     this.reintentableEn,

    required  this.esNuevo,
  });

  @JsonKey(
    
    name: r'ordenId',
    required: false,
    includeIfNull: false,
  )


  final String? ordenId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



          // minimum: 0
  @JsonKey(
    
    name: r'numeroIntento',
    required: true,
    includeIfNull: false,
  )


  final int numeroIntento;



      /// null cuando el error fue definitivo o se agotaron los intentos
  @JsonKey(
    
    name: r'reintentableEn',
    required: false,
    includeIfNull: false,
  )


  final DateTime? reintentableEn;



  @JsonKey(
    
    name: r'esNuevo',
    required: true,
    includeIfNull: false,
  )


  final bool esNuevo;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaIntentoDesembolso &&
      other.ordenId == ordenId &&
      other.estado == estado &&
      other.numeroIntento == numeroIntento &&
      other.reintentableEn == reintentableEn &&
      other.esNuevo == esNuevo;

    @override
    int get hashCode =>
        (ordenId == null ? 0 : ordenId.hashCode) +
        estado.hashCode +
        numeroIntento.hashCode +
        (reintentableEn == null ? 0 : reintentableEn.hashCode) +
        esNuevo.hashCode;

  factory SalidaIntentoDesembolso.fromJson(Map<String, dynamic> json) => _$SalidaIntentoDesembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaIntentoDesembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

