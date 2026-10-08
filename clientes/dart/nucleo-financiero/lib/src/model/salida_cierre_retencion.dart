//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_nucleofinanciero/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_cierre_retencion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaCierreRetencion {
  /// Returns a new [SalidaCierreRetencion] instance.
  SalidaCierreRetencion({

    required  this.retencionId,

    required  this.estado,

    required  this.saldoDisponible,

    required  this.saldoRetenido,
  });

  @JsonKey(
    
    name: r'retencionId',
    required: true,
    includeIfNull: false,
  )


  final String retencionId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



  @JsonKey(
    
    name: r'saldoDisponible',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoDisponible;



  @JsonKey(
    
    name: r'saldoRetenido',
    required: true,
    includeIfNull: false,
  )


  final Dinero saldoRetenido;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaCierreRetencion &&
      other.retencionId == retencionId &&
      other.estado == estado &&
      other.saldoDisponible == saldoDisponible &&
      other.saldoRetenido == saldoRetenido;

    @override
    int get hashCode =>
        retencionId.hashCode +
        estado.hashCode +
        saldoDisponible.hashCode +
        saldoRetenido.hashCode;

  factory SalidaCierreRetencion.fromJson(Map<String, dynamic> json) => _$SalidaCierreRetencionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaCierreRetencionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

